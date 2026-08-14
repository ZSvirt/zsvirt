#!/bin/bash

if test $# -ne 1; then
    echo usage: $(basename "$0") DBPASS
    exit 1
fi

mn_status=$(zstack-ctl status | grep '^MN status.*Stopped')
if [ $? -eq 1 ]; then
  echo "Stopped ZStack before clear unused ip"
  exit 1
fi

ZSHA2_PATH=/usr/local/bin/zsha2
if [ -f "$ZSHA2_PATH" ]; then
    echo "zsha2 detected"
    zsha2_status=$(zsha2 status 2>/dev/null | grep -q 'MN.*status.*Running')
    if [ $? -eq 0 ]; then
        echo "both of the nodes need to be stopped before clear unused ip. Get details by 'zsha2 status'"
        exit 1
    fi
fi

echo "Risky operation. Back up your database before running this script. Continue? [y/Y/N]:"
read input
if [ "$input" != "y" ] && [ "$input" != "Y" ]; then
    echo "Cancelled"
    exit 1
fi

dbpass="$1"

mysql -uzstack -p"$dbpass" zstack -e "CALL cleanupUsedIpVO();"
if [ $? -eq 1 ]; then
  echo "Failed to clean unused ip"
  exit 1
fi