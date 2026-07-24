#!/usr/bin/env bash
# Supervises kubectl port-forward commands listed in port-forwards.conf.
# Each forward runs in its own background loop so a single failure (broken pipe,
# API server hiccup, etc.) only restarts that one forward, not all of them.
#
# Designed to be invoked by systemd (see train-ticket-port-forwards.service).

set -u

ADDRESS="${PF_ADDRESS:-129.62.148.112}"
NAMESPACE_DEFAULT="${PF_NAMESPACE:-default}"
RESTART_DELAY="${PF_RESTART_DELAY:-3}"
CONFIG_FILE="${PF_CONFIG:-/etc/train-ticket/port-forwards.conf}"
KUBECTL_BIN="${KUBECTL_BIN:-/usr/bin/kubectl}"

log() { printf '[%s] [pf] %s\n' "$(date '+%F %T')" "$*"; }

if [[ ! -r "$CONFIG_FILE" ]]; then
    log "ERROR: config file $CONFIG_FILE not readable"
    exit 1
fi

declare -a CHILD_PIDS=()

shutdown() {
    log "shutdown requested, killing ${#CHILD_PIDS[@]} supervisors"
    for pid in "${CHILD_PIDS[@]}"; do
        kill -TERM "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    exit 0
}
trap shutdown SIGINT SIGTERM

supervise() {
    local svc="$1" local_port="$2" target_port="$3" ns="$4"
    local tag="${svc}:${local_port}->${target_port}@${ns}"
    while true; do
        log "starting $tag"
        "$KUBECTL_BIN" port-forward \
            --namespace "$ns" \
            --address "$ADDRESS" \
            "service/$svc" "${local_port}:${target_port}"
        local rc=$?
        log "$tag exited (rc=$rc), restarting in ${RESTART_DELAY}s"
        sleep "$RESTART_DELAY"
    done
}

while IFS= read -r raw || [[ -n "$raw" ]]; do
    line="${raw%%#*}"
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" ]] && continue

    read -r svc local_port target_port ns <<<"$line"
    ns="${ns:-$NAMESPACE_DEFAULT}"

    if [[ -z "${svc:-}" || -z "${local_port:-}" || -z "${target_port:-}" ]]; then
        log "WARN: skipping malformed line: $raw"
        continue
    fi

    supervise "$svc" "$local_port" "$target_port" "$ns" &
    CHILD_PIDS+=("$!")
done < "$CONFIG_FILE"

log "launched ${#CHILD_PIDS[@]} port-forward supervisors on $ADDRESS"
wait
