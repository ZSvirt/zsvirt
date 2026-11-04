#!/bin/sh

user="$1"
password="$2"
database=`dirname $0`/../conf/db/database-test.sql
schema=`dirname $0`/../conf/db/schema-test.sql
trigger=`dirname $0`/../conf/db/trigger.sql

mysql --user=$user --password=$password < $database
mysql --user=$user --password=$password < $schema
mysql --user=$user -t zstacktest --password=$password < $trigger
