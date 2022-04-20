#!/bin/bash

user=denis

source stop-server.sh

su -c 'cd ~/projects/tgn-history-bot' $user
source start-ring-server.sh

caddy run --config /etc/caddy/Caddyfile

exit 0
