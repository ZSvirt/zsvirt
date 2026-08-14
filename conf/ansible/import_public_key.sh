#!/bin/sh

if [ ! -d ~/.ssh ]; then
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh
fi

if [ ! -f ~/.ssh/authorized_keys ]; then
    touch ~/.ssh/authorized_keys
    chmod 600 ~/.ssh/authorized_keys
fi

if [ ! -f "$1" ]; then
    echo "cannot find public key[$1]"
    exit 1
fi

pub_key=`cat $1`
grep "$pub_key" ~/.ssh/authorized_keys > /dev/null
if [ $? -eq 1 ]; then
    echo "$pub_key" >> ~/.ssh/authorized_keys
fi

if [ -x /sbin/restorecon ]; then
    /sbin/restorecon ~/.ssh ~/.ssh/authorized_keys
fi

exit 0
