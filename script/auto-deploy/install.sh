#!/usr/bin/env bash
# Installs the train-ticket auto-deploy systemd timer + service.
# Run as root:  sudo ./install.sh
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

install -d -m 0755 /etc/train-ticket

# Don't clobber an existing (possibly edited) config.
if [[ ! -f /etc/train-ticket/auto-deploy.conf ]]; then
    install -m 0644 "$SRC_DIR/auto-deploy.conf" /etc/train-ticket/auto-deploy.conf
else
    echo "Keeping existing /etc/train-ticket/auto-deploy.conf"
    echo "  (reference: $SRC_DIR/auto-deploy.conf)"
fi

install -m 0755 "$SRC_DIR/auto-deploy.sh"                       /usr/local/bin/train-ticket-auto-deploy.sh
install -m 0644 "$SRC_DIR/train-ticket-auto-deploy.service"     /etc/systemd/system/train-ticket-auto-deploy.service
install -m 0644 "$SRC_DIR/train-ticket-auto-deploy.timer"       /etc/systemd/system/train-ticket-auto-deploy.timer

systemctl daemon-reload
systemctl enable --now train-ticket-auto-deploy.timer

echo
echo "Installed. Useful commands:"
echo "  systemctl status   train-ticket-auto-deploy.timer"
echo "  systemctl list-timers train-ticket-auto-deploy.timer"
echo "  journalctl -u      train-ticket-auto-deploy.service -f"
echo "  systemctl start    train-ticket-auto-deploy.service   # force a check/deploy now"
echo "  \$EDITOR /etc/train-ticket/auto-deploy.conf            # change branch/args"
