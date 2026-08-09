#!/bin/bash

LOG="/tmp/production-worker.log"

echo "$(date) - production-worker started" >> "$LOG"

while true
do
    echo "$(date) - worker processing..." >> "$LOG"

    cat /dev/zero > /dev/null &

    sleep 5
done
