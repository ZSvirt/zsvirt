CREATE KEYSPACE zstack_logging WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};

use zstack_logging;

CREATE TABLE VersionCO (
    name text,
    version text,
    schemaName text,
    date timestamp,
    PRIMARY KEY ((name), version)
) WITH CLUSTERING ORDER BY (version DESC);


CREATE TABLE LogCO (
	type text,
	resourceUuid text,
	level text,
	uuid text,
	dateInLong bigint,
	date timestamp,
	text text,
	parameters text,
	opaque text,
	PRIMARY KEY((type, resourceUuid), dateInLong, level, uuid)
) WITH CLUSTERING ORDER BY (dateInLong DESC);

CREATE TABLE LogRefCO (
	key text,
	uuid text,
	type text,
	resourceUuid text,
	PRIMARY KEY((key), uuid)
) WITH CLUSTERING ORDER BY (uuid ASC);

INSERT INTO VersionCO (name, version, schemaName, date) VALUES('schema_version', '1.4', 'V1.4__schema.cql', dateof(now()));
