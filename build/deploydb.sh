#!/bin/bash

set -e

user="$1"
password="$2"
host="$3"
port="$4"
MYSQL='mysql'

if [[ `id -u` -ne 0 ]] && [[ x"$user" = x"root" ]]; then
    MYSQL='sudo mysql'
fi

base=`dirname $0`

if [[ ! -n $host ]] || [[ ! -n $port ]];then
  loginCmd="--user=$user --password=$password"
else
  loginCmd="--user=$user --password=$password --host=$host --port=$port"
fi

${MYSQL} ${loginCmd} << EOF
set global log_bin_trust_function_creators=1;
DROP DATABASE IF EXISTS zstack;
CREATE DATABASE zstack;
DROP DATABASE IF EXISTS zstack_rest;
CREATE DATABASE zstack_rest;
grant all privileges on zstack.* to root@'%' identified by "${password}";
grant all privileges on zstack_rest.* to root@'%' identified by "${password}";
grant all privileges on zstack.* to root@'127.0.0.1' identified by "${password}";
grant all privileges on zstack_rest.* to root@'127.0.0.1' identified by "${password}";
EOF

# assign flyway version if not defined
: "${flywayver:=3.2.1}"
flyway="$base/../conf/tools/flyway-$flywayver/flyway"
flyway_sql="$base/../conf/tools/flyway-$flywayver/sql/"

mkdir -p ${flyway_sql}

eval "rm -f ${flyway_sql}/*"
cp ${base}/../conf/db/V0.5.1__schema.sql ${flyway_sql}
cp ${base}/../conf/db/beforeMigrate.sql ${flyway_sql}
cp ${base}/../conf/db/beforeValidate.sql ${flyway_sql}
cp ${base}/../conf/db/upgrade/* ${flyway_sql}

if [[ ! -n $host ]] || [[ ! -n $port ]];then
  url="jdbc:mysql://localhost:3306/zstack"
else
  url="jdbc:mysql://$host:$port/zstack"
fi
${flyway} -user=${user} -password=${password} -url=${url} clean

# create baseline and clean its contents for 'beforeValidate.sql'
${flyway} -user=${user} -password=${password} -url=${url} baseline
${MYSQL} ${loginCmd} zstack -e "DELETE FROM schema_version"

${flyway} -outOfOrder=true -user=${user} -password=${password} -url=${url} migrate

eval "rm -f ${flyway_sql}/*"

cp ${base}/../conf/db/V0.6__schema_buildin_httpserver.sql ${flyway_sql}

if [[ ! -n $host ]] || [[ ! -n $port ]];then
  url="jdbc:mysql://localhost:3306/zstack_rest"
else
  url="jdbc:mysql://$host:$port/zstack_rest"
fi
${flyway} -user=${user} -password=${password} -url=${url} clean
${flyway} -outOfOrder=true -user=${user} -password=${password} -url=${url} migrate

eval "rm -f ${flyway_sql}/*"
