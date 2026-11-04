#!/bin/bash
set -e
#If some arguments are "", the script will be called failed, since shell can't 
#recognize "", when sending it through arguments. 
echo "$0 $*"
user="$1"
password="$2"
host="$3"
port="$4"
zstack_user_password="$5"

base=`dirname $0`

# assign flyway version if not defined
: "${flywayver:=3.2.1}"
flyway="$base/tools/flyway-$flywayver/flyway"
flyway_sql="$base/tools/flyway-$flywayver/sql/"

MYSQL='mysql'

if [[ `id -u` -ne 0 ]] && [[ x"$user" = x"root" ]]; then
    MYSQL='sudo mysql'
fi

mysql_run() {
    $MYSQL --user=$user --password=$password --host=$host --port=$port "$@"
}

schema_map=""
flyway_target=()
if [ -d "$base/springConfigXml/ee" ]; then
    schema_map=$(mktemp -d)
    trap 'rm -rf "$schema_map"' EXIT
    tar -xzf "$base/db/zsvirt-ee-schema-map.tgz" -C "$schema_map"
    # Initialize through 1.0.0, whose schema is covered by commercial history through 5.0.2.
    flyway_target=("-target=1.0.0")
fi

mysql_run << EOF
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "$password";
grant all privileges on zstack_rest.* to root@'%' identified by "$password";
grant all privileges on zstack.* to root@'localhost' identified by "$password";
grant all privileges on zstack_rest.* to root@'localhost' identified by "$password";
EOF

rm -rf $flyway_sql
mkdir -p $flyway_sql

cp $base/db/V0.5.1__schema.sql $flyway_sql
cp $base/db/beforeMigrate.sql $flyway_sql
cp $base/db/beforeValidate.sql $flyway_sql
if [ -n "$schema_map" ]; then
    cp "$schema_map/upgrade/V1.0.0__schema.sql" "$flyway_sql"
else
    cp $base/db/upgrade/* $flyway_sql
fi

url="jdbc:mysql://$host:$port/zstack"

bash $flyway -user=$user -password=$password -url=$url clean

# create baseline and clean its contents for 'beforeValidate.sql'
bash $flyway -user=$user -password=$password -url=$url baseline
mysql_run zstack -e "DELETE FROM schema_version"

bash $flyway -user=$user -password=$password -url=$url "${flyway_target[@]}" migrate

if [ -n "$schema_map" ]; then
    rm -f "$flyway_sql"/V*.sql
    cp "$base/db/ee-history/"V*.sql "$flyway_sql"
    for sql in "$schema_map/ee/"V*.sql; do
        [ -f "$flyway_sql/$(basename "$sql")" ] || cp -L "$sql" "$flyway_sql"
    done

    # Register SQL already covered by the fresh-install baseline; do not execute it again.
    python - "$flyway_sql" > "$schema_map/history.sql" <<'PY'
import glob
import os
import sys
import zlib

def version_key(path):
    return tuple(int(part) for part in os.path.basename(path).split('__')[0][1:].split('.'))

if not os.path.isfile(os.path.join(sys.argv[1], 'V5.0.2__schema.sql')):
    raise RuntimeError('missing commercial schema checkpoint 5.0.2')

print('START TRANSACTION;\nDELETE FROM schema_version;')
paths = sorted(glob.glob(os.path.join(sys.argv[1], 'V*.sql')), key=version_key)
for rank, path in enumerate([p for p in paths if version_key(p) <= (5, 0, 2)], 1):
    script = os.path.basename(path)
    version, description = script[1:-4].split('__', 1)
    with open(path, 'rb') as source:
        checksum = zlib.crc32(source.read()) & 0xffffffff
    if checksum >= 0x80000000:
        checksum -= 0x100000000
    print("INSERT INTO schema_version (version_rank, installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) VALUES (%d, %d, '%s', '%s', 'SQL', '%s', %d, SUBSTRING_INDEX(CURRENT_USER(), '@', 1), NOW(), 0, 1);" %
          (rank, rank, version, description.replace('_', ' '), script, checksum))
print('COMMIT;')
PY
    mysql_run zstack < "$schema_map/history.sql"
    bash $flyway -user=$user -password=$password -url=$url migrate
fi

eval "rm -f $flyway_sql/*"

cp $base/db/V0.6__schema_buildin_httpserver.sql $flyway_sql
cp $base/db/zstack_rest/upgrade/* $flyway_sql

url="jdbc:mysql://$host:$port/zstack_rest"
bash $flyway -user=$user -password=$password -url=$url clean
bash $flyway -user=$user -password=$password -url=$url migrate

eval "rm -f $flyway_sql/*"

hostname=`hostname`

[ -z $zstack_user_password ] && zstack_user_password=''

db_version=`mysql --version | awk '/Distrib/{print $5}' |awk -F'.' '{print $1}'`
if [ $db_version -ge 10 ];then
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
drop user if exists zstack;
drop user if exists zstack_rest;
create user 'zstack' identified by "$zstack_user_password";
create user 'zstack_rest' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
flush privileges;
EOF
else
    mysql --user=$user --password=$password --host=$host --port=$port << EOF
grant usage on *.* to 'zstack'@'localhost';
grant usage on *.* to 'zstack'@'%';
drop user zstack;
create user 'zstack' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@'%' identified by "$zstack_user_password";
grant all privileges on zstack.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'localhost' identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@"$hostname" identified by "$zstack_user_password";
grant all privileges on zstack_rest.* to zstack@'%' identified by "$zstack_user_password";
flush privileges;
EOF
fi

