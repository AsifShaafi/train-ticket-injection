#!/usr/bin/env bash
# Stops and removes all kubectl port-forwards started by port-forwards.sh.
#
# Handles both cases:
#   1. Forwards running under the systemd unit (train-ticket-port-forwards.service)
#      -> stops the unit, which kills the supervisor and its kubectl children.
#   2. Forwards started by running port-forwards.sh directly (no systemd)
#      -> kills the supervisor process(es) and any leftover
#         `kubectl port-forward` processes for the services in the config file.

set -u

SERVICE_NAME="${PF_SERVICE_NAME:-train-ticket-port-forwards.service}"
CONFIG_FILE="${PF_CONFIG:-/etc/train-ticket/port-forwards.conf}"
SUPERVISOR_SCRIPT="${PF_SUPERVISOR_SCRIPT:-port-forwards.sh}"

log() { printf '[%s] [pf-stop] %s\n' "$(date '+%F %T')" "$*"; }

stopped_via_systemd=false

if command -v systemctl >/dev/null 2>&1; then
    if systemctl is-active --quiet "$SERVICE_NAME" 2>/dev/null; then
        log "stopping systemd unit $SERVICE_NAME"
        if systemctl stop "$SERVICE_NAME"; then
            stopped_via_systemd=true
        else
            log "WARN: failed to stop $SERVICE_NAME via systemctl"
        fi
    else
        log "systemd unit $SERVICE_NAME not active, skipping"
    fi
else
    log "systemctl not found, skipping systemd stop"
fi

log "checking for standalone supervisor/kubectl port-forward processes"

killed_any=false

# Kill any lingering supervisor script instances (e.g. run manually, outside systemd).
supervisor_pids="$(pgrep -f "$SUPERVISOR_SCRIPT" 2>/dev/null || true)"
if [[ -n "$supervisor_pids" ]]; then
    log "killing supervisor process(es): $supervisor_pids"
    kill -TERM $supervisor_pids 2>/dev/null || true
    killed_any=true
fi

# Kill any leftover `kubectl port-forward` processes for services in the config file.
if [[ -r "$CONFIG_FILE" ]]; then
    while IFS= read -r raw || [[ -n "$raw" ]]; do
        line="${raw%%#*}"
        line="${line#"${line%%[![:space:]]*}"}"
        line="${line%"${line##*[![:space:]]}"}"
        [[ -z "$line" ]] && continue

        read -r svc _local_port _target_port _ns <<<"$line"
        [[ -z "${svc:-}" ]] && continue

        pids="$(pgrep -f "kubectl port-forward.*service/${svc}\b" 2>/dev/null || true)"
        if [[ -n "$pids" ]]; then
            log "killing kubectl port-forward for service/$svc: $pids"
            kill -TERM $pids 2>/dev/null || true
            killed_any=true
        fi
    done < "$CONFIG_FILE"
else
    log "config file $CONFIG_FILE not readable, skipping per-service cleanup"
fi

if [[ "$stopped_via_systemd" == false && "$killed_any" == false ]]; then
    log "nothing to stop (no active systemd unit, no matching processes)"
else
    log "done"
fi
