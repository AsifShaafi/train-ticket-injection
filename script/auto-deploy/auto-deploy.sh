#!/usr/bin/env bash
# Polls the configured git branch and, when a new commit appears upstream,
# rebuilds the train-ticket images and redeploys them into minikube.
#
# Designed to be invoked by systemd on a timer (see
# train-ticket-auto-deploy.service / .timer). Safe to run by hand too.
#
# Why rebuild + reset + deploy?
#   - Images use a fixed tag (e.g. 1.0.2) with imagePullPolicy: IfNotPresent,
#     so a rebuilt image is NOT picked up by running pods.
#   - deploy.sh uses `helm install`, which fails if the releases already exist.
#   So the only reliable way to roll out new code is: rebuild the images inside
#   minikube's docker daemon, tear the release down, then deploy fresh so the
#   recreated pods load the new local image.
#   NOTE: reset-deploy wipes cluster + DB state on every update.

set -uo pipefail

CONFIG_FILE="${AD_CONFIG:-/etc/train-ticket/auto-deploy.conf}"
LOCK_FILE="${AD_LOCK:-/tmp/train-ticket-auto-deploy.lock}"
STATE_FILE="${AD_STATE:-/tmp/train-ticket-auto-deploy.last}"

log() { printf '[%s] [auto-deploy] %s\n' "$(date '+%F %T')" "$*"; }

if [[ ! -r "$CONFIG_FILE" ]]; then
    log "ERROR: config file $CONFIG_FILE not readable"
    exit 1
fi
# shellcheck disable=SC1090
source "$CONFIG_FILE"

REPO_DIR="${REPO_DIR:?REPO_DIR not set in $CONFIG_FILE}"
BRANCH="${BRANCH:-injection}"
REMOTE_NAME="${REMOTE_NAME:-origin}"
DEPLOY_ARGS="${DEPLOY_ARGS:-}"
NAMESPACE="${NAMESPACE:-default}"
AUTO_START_MINIKUBE="${AUTO_START_MINIKUBE:-1}"
RESET_BEFORE_DEPLOY="${RESET_BEFORE_DEPLOY:-1}"

# Non-blocking lock: if a previous (possibly long) build is still running,
# this timer tick just bows out instead of stacking another deploy.
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    log "another deploy is already in progress, skipping this tick"
    exit 0
fi

cd "$REPO_DIR" || { log "ERROR: cannot cd into $REPO_DIR"; exit 1; }

# `chmod -R 777 deployment` churns file modes; ignore that so the tree stays
# clean and `git reset --hard` is a no-op except for real content changes.
git config core.fileMode false

log "fetching ${REMOTE_NAME}/${BRANCH}"
if ! git fetch --quiet "$REMOTE_NAME" "$BRANCH"; then
    log "ERROR: git fetch failed"
    exit 1
fi

LOCAL=$(git rev-parse HEAD 2>/dev/null)
REMOTE=$(git rev-parse "${REMOTE_NAME}/${BRANCH}" 2>/dev/null)

if [[ -z "$REMOTE" ]]; then
    log "ERROR: cannot resolve ${REMOTE_NAME}/${BRANCH}"
    exit 1
fi

if [[ "$LOCAL" == "$REMOTE" ]]; then
    log "up to date at ${LOCAL:0:12}, nothing to deploy"
    exit 0
fi

log "new commit detected: ${LOCAL:0:12} -> ${REMOTE:0:12}; starting redeploy"

git checkout -q "$BRANCH" 2>/dev/null || true
if ! git reset --hard "${REMOTE_NAME}/${BRANCH}"; then
    log "ERROR: git reset --hard failed"
    exit 1
fi

if ! minikube status >/dev/null 2>&1; then
    if [[ "$AUTO_START_MINIKUBE" == "1" ]]; then
        log "minikube is not running, starting it"
        if ! minikube start; then
            log "ERROR: minikube start failed"
            exit 1
        fi
    else
        log "ERROR: minikube not running and auto-start disabled"
        exit 1
    fi
fi

# Build images straight into minikube's docker daemon.
eval "$(minikube docker-env)"

chmod -R 777 deployment 2>/dev/null || true

log "building images (make build)"
if ! make build; then
    log "ERROR: make build failed"
    exit 1
fi

if [[ "$RESET_BEFORE_DEPLOY" == "1" ]]; then
    log "tearing down existing release (make reset-deploy)"
    make reset-deploy Namespace="$NAMESPACE" \
        || log "WARN: reset-deploy returned non-zero (continuing)"
fi

log "deploying (make deploy-no-build DeployArgs=\"$DEPLOY_ARGS\")"
if ! make deploy-no-build Namespace="$NAMESPACE" DeployArgs="$DEPLOY_ARGS"; then
    log "ERROR: deploy-no-build failed"
    exit 1
fi

echo "$REMOTE" >"$STATE_FILE" 2>/dev/null || true
log "redeploy complete, now running ${REMOTE:0:12}"
