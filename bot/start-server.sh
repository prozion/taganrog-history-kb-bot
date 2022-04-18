#!/bin/bash

user=denis

source stop-server.sh

su -c 'cd ~/projects/tgn-history-bot' $user
su -c 'lein ring server-headless' $user

caddy run --config /etc/caddy/Caddyfile

exit 0
