#!/bin/bash
# reset.sh — tear down the Train-Ticket deployment
#
# CHANGES 2026-08-05
#
# 1. TRACE DATA PROTECTION. `kubectl delete -f .../otel` deletes every
#    manifest in that directory. Once the Jaeger Badger PVC lives there,
#    a reset destroys all collected traces — and the `standard` storage
#    class has RECLAIMPOLICY=Delete, so the underlying data goes with it.
#    The PVC now lives in its own directory and is NEVER deleted unless
#    --delete-trace-data is passed explicitly.
#
# 2. tsdb RELEASE FIX. `grep ts-` does not match the release named `tsdb`
#    ("tsdb" contains no "ts-"), so tsdb survived every reset and left a
#    stale Helm record that made the next `helm install tsdb` fail. The
#    filter is now `grep -E '^(ts-|tsdb)'`, anchored so it matches the
#    release NAME column rather than any column containing the substring.
#
# 3. xargs -r. With no matching releases, `xargs helm uninstall` ran
#    `helm uninstall` with no arguments and errored. -r skips the call.
#
# 4. Namespace is now validated. An empty $1 made every `-n $namespace`
#    consume the following argument, silently targeting the wrong scope.
#
# 5. Paths are resolved from the repo root rather than assuming the
#    script was invoked from there.
#
# 6. Confirmation prompt, bypassable with --yes for scripted use.
#
# NOT set -e: deletes are expected to fail when a resource is already
# gone. Failures are reported, not fatal.
#
# Usage:
#   ./reset.sh <namespace>
#   ./reset.sh <namespace> --yes
#   ./reset.sh <namespace> --yes --delete-trace-data

set -uo pipefail

TT_ROOT=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "$TT_ROOT/utils.sh"

# --- Resolve repo root -----------------------------------------------------
REPO_ROOT=$(cd "$TT_ROOT" && git rev-parse --show-toplevel 2>/dev/null)
if [[ -z "${REPO_ROOT:-}" || ! -d "$REPO_ROOT/deployment" ]]; then
  CAND="$TT_ROOT"
  for _ in 1 2 3 4; do
    [[ -d "$CAND/deployment" ]] && break
    CAND=$(dirname "$CAND")
  done
  REPO_ROOT="$CAND"
fi
if [[ ! -d "$REPO_ROOT/deployment" ]]; then
  echo "ERROR: cannot locate the repo root (no deployment/ found)." >&2
  exit 1
fi

MANIFESTS="$REPO_ROOT/deployment/kubernetes-manifests"

# --- Arguments -------------------------------------------------------------
namespace="${1:-}"
if [[ -z "$namespace" || "$namespace" == -* ]]; then
  echo "Usage: $0 <namespace> [--yes] [--delete-trace-data]" >&2
  echo "Refusing to run without an explicit namespace." >&2
  exit 2
fi
shift

ASSUME_YES=0
DELETE_TRACE_DATA=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes|-y)             ASSUME_YES=1; shift ;;
    --delete-trace-data)  DELETE_TRACE_DATA=1; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

if ! kubectl get namespace "$namespace" >/dev/null 2>&1; then
  echo "ERROR: namespace '$namespace' does not exist." >&2
  exit 1
fi

# --- Warn about trace data -------------------------------------------------
PVC_NAME="${JAEGER_PVC_NAME:-jaeger-badger-pvc}"
PVC_EXISTS=0
kubectl get pvc "$PVC_NAME" -n "$namespace" >/dev/null 2>&1 && PVC_EXISTS=1

echo "About to tear down Train-Ticket in namespace: $namespace"
echo
if [[ "$PVC_EXISTS" -eq 1 ]]; then
  if [[ "$DELETE_TRACE_DATA" -eq 1 ]]; then
    echo "  !! PVC '$PVC_NAME' WILL BE DELETED."
    echo "  !! The storage class reclaim policy is Delete, so every"
    echo "  !! collected trace is destroyed and cannot be recovered."
    echo "  !! Export anything you need to data/raw/ FIRST."
  else
    echo "  PVC '$PVC_NAME' will be PRESERVED (trace data is safe)."
    echo "  Pass --delete-trace-data to remove it deliberately."
  fi
else
  echo "  No Jaeger PVC found — nothing to preserve."
fi
echo

if [[ "$ASSUME_YES" -ne 1 ]]; then
  read -r -p "Proceed? [y/N] " reply
  [[ "$reply" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }
  echo
fi

# --- Teardown --------------------------------------------------------------
echo "=== Deleting quickstart-k8s manifests ==="
kubectl delete -f "$MANIFESTS/quickstart-k8s/yamls" -n "$namespace" \
  --ignore-not-found || echo "  (some resources were already gone)"
echo

echo "=== Uninstalling ts-* and tsdb Helm releases ==="
# Anchored so the NAME column is matched, not any column containing 'ts-'.
# '^(ts-|tsdb)' catches tsdb, which the old 'grep ts-' never matched.
RELEASES=$(helm ls -n "$namespace" 2>/dev/null | awk 'NR>1 {print $1}' \
           | grep -E '^(ts-|tsdb)' || true)
if [[ -n "$RELEASES" ]]; then
  echo "$RELEASES" | sed 's/^/  /'
  echo "$RELEASES" | xargs -r helm uninstall -n "$namespace" \
    || echo "  (one or more uninstalls failed — check 'helm ls -a')"
else
  echo "  none found"
fi
echo

echo "=== Uninstalling infrastructure Helm releases ==="
for rel in "${rabbitmqRelease:-}" "${nacosRelease:-}" "${nacosDBRelease:-}"; do
  if [[ -z "$rel" ]]; then
    echo "  SKIP: a release variable is unset in utils.sh"
    continue
  fi
  if helm status "$rel" -n "$namespace" >/dev/null 2>&1; then
    helm uninstall "$rel" -n "$namespace" || echo "  FAILED: $rel"
  else
    echo "  not installed: $rel"
  fi
done
echo

echo "=== Deleting observability stack (otel/) ==="
# The Jaeger PVC is deliberately NOT in this directory. See
# deployment/kubernetes-manifests/storage/.
kubectl delete -f "$MANIFESTS/otel" -n "$namespace" \
  --ignore-not-found || echo "  (some resources were already gone)"
echo

if [[ "$DELETE_TRACE_DATA" -eq 1 && "$PVC_EXISTS" -eq 1 ]]; then
  echo "=== Deleting trace-data PVC (explicitly requested) ==="
  kubectl delete -f "$MANIFESTS/storage" -n "$namespace" --ignore-not-found
  echo
fi

# --- Post-teardown check ---------------------------------------------------
echo "=== Remaining in namespace $namespace ==="
LEFT=$(helm ls -a -n "$namespace" 2>/dev/null | awk 'NR>1 {print $1}' \
       | grep -E '^(ts-|tsdb)' || true)
if [[ -n "$LEFT" ]]; then
  echo "  STALE HELM RECORDS — the next install of these will fail:"
  echo "$LEFT" | sed 's/^/    /'
  echo "  Clear with: helm uninstall <name> -n $namespace"
else
  echo "  No ts-/tsdb Helm records remain."
fi

PODS=$(kubectl get pods -n "$namespace" --no-headers 2>/dev/null \
       | grep -c '^ts-' || true)
echo "  ts-* pods still present: ${PODS:-0} (some terminate asynchronously)"

if [[ "$PVC_EXISTS" -eq 1 && "$DELETE_TRACE_DATA" -ne 1 ]]; then
  echo "  PVC '$PVC_NAME': preserved"
fi
echo
echo "Reset complete."