CREATE TABLE IF NOT EXISTS `zstack`.`SoftwarePackageVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `hostUuid` char(32),
    `managementNodeUuid` char(32),
    `installPath` varchar(2048),
    `unzipInstallPath` varchar(2048),
    `type` varchar(1024),
    `md5sum` char(32),
    `status` char(32),
    `size` bigint unsigned,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CALL INSERT_COLUMN('HostEO', 'hostname', 'varchar(256)', 1, NULL, 'nqn');
DROP VIEW IF EXISTS `zstack`.`HostVO`;
CREATE VIEW `zstack`.`HostVO` AS SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType,
state, status, architecture, nqn, hostname, createDate, lastOpDate FROM `zstack`.`HostEO` WHERE deleted IS NULL;

UPDATE `zstack`.`VolumeSnapshotTreeVO` t JOIN `zstack`.`VolumeVO` v ON t.volumeUuid = v.uuid
SET t.rootImageUuid = v.rootImageUuid
WHERE t.current = true
  AND v.rootImageUuid IS NOT NULL
  AND t.rootImageUuid IS NULL;


DELETE ref FROM `zstack`.`VolumeSnapshotReferenceVO` ref
                    INNER JOIN `zstack`.`VolumeEO` vol ON vol.uuid = ref.referenceVolumeUuid
WHERE ref.referenceType = 'VolumeVO'
  AND ref.referenceVolumeUuid = ref.referenceUuid
  AND ref.referenceInstallUrl NOT LIKE CONCAT('%', SUBSTRING_INDEX(vol.installPath, '/', -1), '%');

UPDATE `zstack`.`GlobalConfigVO` SET value="64", defaultValue="64" WHERE category="volumeSnapshot" AND name="incrementalSnapshot.maxNum" AND value > 120;

