#!/bin/bash
# deploy.sh — deploy Train-Ticket
#
# CHANGES 2026-08-05
#
# 1. MULTIPLE FLAGS NOW WORK. The old script read flags from "$2" only and
#    gated parsing on `[ $# == 2 ]`. So:
#        ./deploy.sh default --with-tracing --independent-db
#    had $# == 3, the gate failed, parse_args never ran, and quick_start
#    deployed with NO tracing and NO error. The only way to pass more than
#    one flag was to quote them into a single argument, which is not
#    discoverable. Flags are now read from "$@" normally; the old quoted
#    form still works.
#
# 2. TRACE STORAGE APPLIED BEFORE THE OTEL STACK. The Jaeger Deployment
#    mounts persistentVolumeClaim jaeger-badger-pvc, defined in
#    deployment/kubernetes-manifests/storage/. Applied after otel/, or not
#    at all, the pod sits Pending on an unbound claim.
#
# 3. FAILURES ARE REPORTED. utils.sh sends helm output to /dev/null, so a
#    failed install produced a clean-looking run and a broken cluster.
#    Each stage's exit status is now recorded and summarised at the end.
#
# 4. NO-TRACING WARNING. The default deploy.yaml.sample carries no OTel
#    references; only sw_deploy.yaml.sample injects the agent. A deploy
#    without --with-tracing produces a cluster that emits no traces, which
#    is useless for this project and has cost real time before.
#
# 5. Namespace validated and created if absent.
#
# NOT set -e: utils.sh functions have their own error handling, and an
# abort partway through leaves a half-deployed cluster that is harder to
# reason about than a completed run with a failure summary.
#
# Usage:
#   ./deploy.sh <namespace> --with-tracing
#   ./deploy.sh <namespace> --with-tracing --independent-db --with-monitoring
#   ./deploy.sh <namespace> --all
#   ./deploy.sh <namespace> "--with-tracing --with-monitoring"   # legacy form

set -uo pipefail

TT_ROOT=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "$TT_ROOT/utils.sh"

# --- Repo root and manifest paths ------------------------------------------
REPO_ROOT=$(cd "$TT_ROOT" && git rev-parse --show-toplevel 2>/dev/null)
if [[ -z "${REPO_ROOT:-}" || ! -d "$REPO_ROOT/deployment" ]]; then
  CAND="$TT_ROOT"
  for _ in 1 2 3 4; do
    [[ -d "$CAND/deployment" ]] && break
    CAND=$(dirname "$CAND")
  done
  REPO_ROOT="$CAND"
fi
[[ -d "$REPO_ROOT/deployment" ]] || { echo "ERROR: repo root not found" >&2; exit 1; }
MANIFESTS="$REPO_ROOT/deployment/kubernetes-manifests"
STORAGE_DIR="$MANIFESTS/storage"

# --- Arguments -------------------------------------------------------------
namespace="${1:-}"
if [[ -z "$namespace" || "$namespace" == -* ]]; then
  echo "Usage: $0 <namespace> [--all] [--independent-db] [--with-monitoring] [--with-tracing]" >&2
  exit 2
fi
shift

argNone=1
argDB=0
argMonitoring=0
argTracing=0
argAll=0

# Accept both the normal form and the legacy single-quoted-string form.
for arg in $@; do
  argNone=0
  case $arg in
    "--all")              argAll=1 ;;
    "--independent-db")   argDB=1 ;;
    "--with-monitoring")  argMonitoring=1 ;;
    "--with-tracing")     argTracing=1 ;;
    *) echo "Unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# --- Stage tracking --------------------------------------------------------
STAGES=()
run_stage() {
  local label="$1"; shift
  echo
  echo "=== $label ==="
  if "$@"; then
    STAGES+=("OK      $label")
  else
    STAGES+=("FAILED  $label")
    echo "  !! $label reported a failure. Continuing; see the summary below."
  fi
}

# Applies the Jaeger PVC. Must run before deploy_tracing.
deploy_trace_storage() {
  if [[ ! -d "$STORAGE_DIR" ]]; then
    echo "  ERROR: $STORAGE_DIR missing."
    echo "  The Jaeger Deployment mounts PVC jaeger-badger-pvc. Without it"
    echo "  the pod stays Pending and no traces are stored."
    return 1
  fi
  kubectl apply -f "$STORAGE_DIR" -n "$namespace"
}

# --- Namespace -------------------------------------------------------------
if ! kubectl get namespace "$namespace" >/dev/null 2>&1; then
  echo "Namespace '$namespace' does not exist. Creating it."
  kubectl create namespace "$namespace" || exit 1
fi

# --- Warn about a tracing-less deploy --------------------------------------
if [[ "$argTracing" -ne 1 && "$argAll" -ne 1 ]]; then
  echo
  echo "WARNING: deploying WITHOUT --with-tracing."
  echo "  deploy.yaml.sample contains no OTel references; only"
  echo "  sw_deploy.yaml.sample injects the Java agent. This cluster will"
  echo "  emit no traces, and Jaeger will show only Nacos polling."
  echo "  For SecLogTrace, use --with-tracing."
  echo
  if [[ -t 0 ]]; then
    read -r -p "Continue anyway? [y/N] " reply
    [[ "$reply" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
  fi
fi

# --- Deploy paths ----------------------------------------------------------
function quick_start {
  echo "quick start (no tracing)"
  run_stage "infrastructures"      deploy_infrastructures "$namespace"
  run_stage "mysql (all-in-one)"   deploy_tt_mysql_all_in_one "$namespace"
  run_stage "secrets"              deploy_tt_secret "$namespace"
  run_stage "services"             deploy_tt_svc "$namespace"
  run_stage "deployments"          deploy_tt_dp "$namespace"
}

function deploy_all {
  run_stage "infrastructures"        deploy_infrastructures "$namespace"
  run_stage "mysql (per-service)"    deploy_tt_mysql_each_service "$namespace"
  run_stage "secrets"                deploy_tt_secret "$namespace"
  run_stage "services"               deploy_tt_svc "$namespace"
  run_stage "deployments (OTel)"     deploy_tt_dp_sw "$namespace"
  run_stage "trace storage (PVC)"    deploy_trace_storage
  run_stage "tracing stack"          deploy_tracing "$namespace"
  run_stage "monitoring"             deploy_monitoring
}

function deploy {
  if [[ "$argNone" == 1 ]]; then quick_start; return; fi
  if [[ "$argAll"  == 1 ]]; then deploy_all;  return; fi

  run_stage "infrastructures" deploy_infrastructures "$namespace"

  if [[ "$argDB" == 1 ]]; then
    run_stage "mysql (per-service)" deploy_tt_mysql_each_service "$namespace"
  else
    run_stage "mysql (all-in-one)"  deploy_tt_mysql_all_in_one "$namespace"
  fi

  run_stage "secrets"  deploy_tt_secret "$namespace"
  run_stage "services" deploy_tt_svc "$namespace"

  if [[ "$argTracing" == 1 ]]; then
    run_stage "deployments (OTel)"  deploy_tt_dp_sw "$namespace"
    run_stage "trace storage (PVC)" deploy_trace_storage
    run_stage "tracing stack"       deploy_tracing "$namespace"
  else
    run_stage "deployments"         deploy_tt_dp "$namespace"
  fi

  if [[ "$argMonitoring" == 1 ]]; then
    run_stage "monitoring" deploy_monitoring
  fi
}

echo "Namespace : $namespace"
echo "Flags     : all=$argAll db=$argDB monitoring=$argMonitoring tracing=$argTracing"
deploy

# --- Summary ---------------------------------------------------------------
echo
echo "=========================== SUMMARY ==========================="
printf '  %s\n' "${STAGES[@]}"
FAILED=$(printf '%s\n' "${STAGES[@]}" | grep -c '^FAILED' || true)
echo "==============================================================="
if [[ "${FAILED:-0}" -gt 0 ]]; then
  echo "  $FAILED stage(s) failed. The cluster is incomplete."
  echo "  Check for stale Helm records:  helm ls -a -n $namespace"
fi

if [[ "$argTracing" == 1 || "$argAll" == 1 ]]; then
  echo
  echo "  Verify tracing storage once pods settle:"
  echo "    kubectl get pvc jaeger-badger-pvc -n $namespace"
  echo "    kubectl get pod -n $namespace -l app.kubernetes.io/name=jaeger"
  echo "    kubectl exec -n $namespace deploy/jaeger -- ls /badger/data"
  echo "  A Pending Jaeger pod means the PVC did not bind."
fi

exit "${FAILED:-0}"