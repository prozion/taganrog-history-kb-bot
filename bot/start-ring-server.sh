#!/bin/bash

user=denis

nohup lein ring server-headless > /dev/null >/var/log/projects/taganrog-history-kb/ring.log 2>&1& echo $! > /var/run/denis/ring.pid
# su -c 'nohup lein ring server-headless' $user
