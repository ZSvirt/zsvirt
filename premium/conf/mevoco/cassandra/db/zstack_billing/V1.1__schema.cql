CREATE KEYSPACE zstack_billing WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};

use zstack_billing;

CREATE TABLE VersionCO (
    name text,
    version text,
    schemaName text,
    date timestamp,
    PRIMARY KEY ((name), version)
) WITH CLUSTERING ORDER BY (version DESC);

CREATE TYPE PriceUDF ( 
        resourceUnit text,
        timeUnit text,
        price float
        );

CREATE TABLE VmUsageCO (
        accountUuid text,
        dateInLong bigint,
        vmUuid text,
        date timestamp,
        name text,
        state text,
        cpuNum int,
        memorySize bigint,
        rootVolumeSize bigint,
        inventory text,
        cpuPrice frozen <PriceUDF>,
        memoryPrice frozen <PriceUDF>,
        volumePrice frozen <PriceUDF>,
        PRIMARY KEY((accountUuid), dateInLong, vmUuid)
        ) WITH CLUSTERING ORDER BY (dateInLong ASC);

CREATE TABLE PriceCO (
        resourceName text,
        dateInLong bigint,
        date timestamp,
        resourceUnit text,
        timeUnit text,
        price float,
        PRIMARY KEY((resourceName), dateInLong)
        ) WITH CLUSTERING ORDER BY (dateInLong DESC);

CREATE TABLE DataVolumeUsageCO (
        accountUuid text,
        dateInLong bigint,
        uuid text,
        volumeStatus text,
        volumeName text,
        volumeSize bigint,
        inventory text,
        date timestamp,
        price frozen <PriceUDF>,
        PRIMARY KEY((accountUuid), dateInLong, uuid)
        ) WITH CLUSTERING ORDER BY (dateInLong ASC, uuid ASC);

CREATE TABLE RootVolumeUsageCO (
        accountUuid text,
        dateInLong bigint,
        vmUuid text,
        uuid text,
        volumeStatus text,
        volumeName text,
        volumeSize bigint,
        inventory text,
        date timestamp,
        price frozen <PriceUDF>,
        PRIMARY KEY((accountUuid), dateInLong, vmUuid)
        ) WITH CLUSTERING ORDER BY (dateInLong ASC, vmUuid ASC);

INSERT INTO VersionCO (name, version, schemaName, date) VALUES('schema_version', '1.1', 'V1.1__schema.cql', dateof(now()));
