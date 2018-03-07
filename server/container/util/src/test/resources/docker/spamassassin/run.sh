#!/bin/bash
set -m

/rule-update.sh &
/spamd.sh &

pids=`jobs -p`

exitcode=0

wait
