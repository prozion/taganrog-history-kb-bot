#!/bin/bash
project_name="tgn-history"

for pid in `pgrep -f ${project_name}`
do
  echo "kill ${pid}"
  kill $pid
done

caddy stop
