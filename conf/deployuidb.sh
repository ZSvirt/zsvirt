#!/bin/sh
set -e
echo "$0 $*"
user="$1"
password="$2"
host="$3"
port="$4"
zstack_ui_db_password="$5"

base=`dirname $0`
flyway="$base/tools/flyway-3.2.1/flyway"
flyway_sql="$base/tools/flyway-3.2.1/sql/"

# give grant option to the new management ip after `zstack-ctl change_ip`
mysql --user=$user --password=$password --port=$port << EOF
grant all privileges on *.* to root@"$host" identified by "$password" with grant option;
EOF

mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'root'@'localhost';
grant usage on *.* to 'root'@'%';
DROP DATABASE IF EXISTS zstack_ui;
CREATE DATABASE zstack_ui;
grant all privileges on zstack_ui.* to root@'%' identified by "$password";
grant all privileges on zstack_ui.* to root@'localhost' identified by "$password";
flush privileges;
EOF

rm -rf $flyway_sql
mkdir -p $flyway_sql

ui_schema_path=`echo ~zstack`"/zstack-ui/tmp/WEB-INF/classes/db/migration/"
if [ -d $ui_schema_path ]; then
    cp $ui_schema_path/* $flyway_sql
    url="jdbc:mysql://$host:$port/zstack_ui"
    bash $flyway -user=$user -password=$password -url=$url clean
    bash $flyway -user=$user -password=$password -url=$url migrate
    eval "rm -f $flyway_sql/*"
fi

hostname=`hostname`
db_version=`mysql --version | awk '/Distrib/{print $5}' |awk -F'.' '{print $1}'`
if [ $db_version -ge 10 ];then
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
drop user if exists zstack_ui;
create user 'zstack_ui' identified by "$zstack_ui_db_password";
grant all privileges on zstack_ui.* to zstack_ui@'localhost' identified by "$zstack_ui_db_password";
grant all privileges on zstack_ui.* to zstack_ui@'%' identified by "$zstack_ui_db_password";
flush privileges;
EOF
else
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'zstack_ui'@'localhost';
grant usage on *.* to 'zstack_ui'@'%';
drop user zstack_ui;
create user 'zstack_ui' identified by "$zstack_ui_db_password";
grant all privileges on zstack_ui.* to zstack_ui@'localhost' identified by "$zstack_ui_db_password";
grant all privileges on zstack_ui.* to zstack_ui@'%' identified by "$zstack_ui_db_password";
grant all privileges on zstack_ui.* to zstack_ui@"$hostname" identified by "$zstack_ui_db_password";
flush privileges;
EOF
fi
