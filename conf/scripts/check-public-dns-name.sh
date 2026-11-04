#!/bin/sh

for site in `echo {dnsCheckList}`
do
    which nslookup &>/dev/null
    if [ $? -eq 0 ]; then
        nslookup -retry=1 -timeout=2 $site &>/dev/null
    else
        ping -c 1 -W 2 $site &>/dev/null
    fi

    if [ $? -eq 0 ]; then
        exit 0
    fi
done

exit 1
