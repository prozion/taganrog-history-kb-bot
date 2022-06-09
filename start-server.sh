#!/bin/bash

user=denis

source stop-server.sh

su -c 'cd ~/projects/taganrog-history-bot' $user
source start-ring-server.sh

caddy run --config /etc/caddy/Caddyfile

exit 0
