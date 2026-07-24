#!/usr/bin/env bash
# Installs the train-ticket port-forward systemd service.
# Run as root:  sudo ./install.sh
set -euo pipefail

SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

install -d -m 0755 /etc/train-ticket
install -m 0644 "$SRC_DIR/port-forwards.conf"              /etc/train-ticket/port-forwards.conf
install -m 0755 "$SRC_DIR/port-forwards.sh"                /usr/local/bin/train-ticket-port-forwards.sh
install -m 0644 "$SRC_DIR/train-ticket-port-forwards.service" /etc/systemd/system/train-ticket-port-forwards.service

systemctl daemon-reload
systemctl enable --now train-ticket-port-forwards.service

echo
echo "Installed. Useful commands:"
echo "  systemctl status  train-ticket-port-forwards"
echo "  systemctl restart train-ticket-port-forwards"
echo "  journalctl -u     train-ticket-port-forwards -f"
echo "  \$EDITOR /etc/train-ticket/port-forwards.conf   # then: systemctl restart train-ticket-port-forwards"
