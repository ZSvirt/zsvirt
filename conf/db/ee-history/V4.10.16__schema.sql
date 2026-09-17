CREATE TABLE IF NOT EXISTS `zstack`.`PluginDriverVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `vendor` varchar(64) NOT NULL,
    `features` varchar(1024) NOT NULL,
    `optionTypes` text DEFAULT NULL,
    `license` varchar(1024) DEFAULT NULL,
    `version` varchar(1024) DEFAULT NULL,
    `description` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VolumeCbtBackupRecordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `mode` varchar(255) NOT NULL,
    `target` varchar(2048) NOT NULL,
    `scratchNodeName` varchar(255) NOT NULL,
    `bitmapName` varchar(255) NOT NULL,
    `lastBitmapName` varchar(255),
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ExponBlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `exponStatus` varchar(32) NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkExponBlockVolumeVOBlockVolumeVO FOREIGN KEY (uuid) REFERENCES BlockVolumeVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`ExternalPrimaryStorageVO` MODIFY COLUMN `config` TEXT DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostRefVO` (
    `id`       BIGINT UNSIGNED UNIQUE,
    `hostId`   INT          DEFAULT NULL,
    `protocol` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET @row_number = 0;
INSERT INTO ExternalPrimaryStorageHostRefVO (id, hostId, protocol)
SELECT
    p.id,
    (@row_number := @row_number + 1) as hostId,
    e.defaultProtocol as protocol
FROM PrimaryStorageHostRefVO p LEFT JOIN ExternalPrimaryStorageVO e ON p.primaryStorageUuid = e.uuid
ORDER BY p.id;

-- Feature: Customer Resource Attributes | ZSV-???

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeKeyVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeValueVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `keyUuid` char(32) NOT NULL,
    `value` varchar(2048) NOT NULL,
    `resourceUuid` char(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT fkResourceAttributeValueVOResourceAttributeKeyVO FOREIGN KEY (keyUuid) REFERENCES ResourceAttributeKeyVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkResourceAttributeValueVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeKeyResourceTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `keyUuid` char(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT fkResourceAttributeKeyResourceTypeVOResourceAttributeKeyVO FOREIGN KEY (keyUuid) REFERENCES ResourceAttributeKeyVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `uqResourceAttributeKeyResourceTypeVO` UNIQUE(`keyUuid`, `resourceType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeConstraintVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `keyUuid` char(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `parameter` varchar(2048) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT fkResourceAttributeConstraintVOResourceAttributeKeyVO FOREIGN KEY (keyUuid) REFERENCES ResourceAttributeKeyVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: improve ipv6 | ZSV-8660

CREATE TABLE IF NOT EXISTS `zstack`.`ReservedIpRangeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `name` varchar(255) DEFAULT NULL COMMENT 'name',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `ipVersion` int(10) unsigned DEFAULT 4 COMMENT 'ip range version',
    `startIp` varchar(64) NOT NULL COMMENT 'start ip',
    `endIp` varchar(64) NOT NULL COMMENT 'end ip',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkReservedIpRangeVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

update EventSubscriptionVO set name = 'VM NIC IP Changed (GuestTools Is Required)' where uuid='98536fa94e3f4481a38331a989132b7c';
update EventSubscriptionVO set name = 'NIC IP Configured in VM has been Occupied or in the Reserved Range (GuestTools Is Required)' where uuid='4a3494bcdbac4eaab9e9e56e27d74a2a';

DELIMITER $$
DROP FUNCTION IF EXISTS `INET6_ATON` $$
CREATE FUNCTION `INET6_ATON`(
    ip VARCHAR(128)
) RETURNS BINARY(16)
BEGIN
    DECLARE binary_ip BINARY(16) DEFAULT 0x00000000000000000000000000000000;
    DECLARE hextet VARCHAR(5);
    DECLARE i INT DEFAULT 1;
    DECLARE segment_position INT DEFAULT 1;
    DECLARE segment_count INT;
    DECLARE expanded_ip VARCHAR(45);
    IF INSTR(ip, '.') > 0 THEN
        SET binary_ip = CONCAT(REPEAT(UNHEX('00'), 10), UNHEX('FFFF'), UNHEX(LPAD(HEX(INET_ATON(ip)), 8, '0')));
    ELSE
        IF INSTR(ip, '::') > 0 THEN
            SET segment_count = LENGTH(ip) - LENGTH(REPLACE(ip, ':', '')) + 1;
            SET expanded_ip = REPLACE(ip, '::', CONCAT(':', REPEAT(':0000', 8 - segment_count), ':'));
            IF LEFT(expanded_ip, 1) = ':' THEN
                SET expanded_ip = SUBSTRING(expanded_ip, 2);
            END IF;
            IF RIGHT(expanded_ip, 1) = ':' THEN
                SET expanded_ip = SUBSTRING(expanded_ip, 1, LENGTH(expanded_ip) - 1);
            END IF;
        ELSE
            SET expanded_ip = ip;
        END IF;
        WHILE i <= 8 DO
            SET hextet = SUBSTRING_INDEX(SUBSTRING_INDEX(expanded_ip, ':', i), ':', -1);
            IF LENGTH(hextet) > 0 THEN
                SET binary_ip = INSERT(binary_ip, segment_position, 4, UNHEX(LPAD(hextet, 4, '0')));
            END IF;
            SET segment_position = segment_position + 2;
            SET i = i + 1;
        END WHILE;
    END IF;
    RETURN binary_ip;
END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS upgradeIpInBinaryColumn;
DELIMITER $$
CREATE PROCEDURE upgradeIpInBinaryColumn()
BEGIN
    CALL INSERT_COLUMN('UsedIpVO', 'ipInBinary', 'VARBINARY(16)', 0, 0x00000000000000000000000000000000, 'ipInLong');

    UPDATE `zstack`.`UsedIpVO`
    SET `ipInBinary` = INET6_ATON(`ip`)
    WHERE INET6_ATON(`ip`) IS NOT NULL
    AND `ipInBinary` = 0x00000000000000000000000000000000;

    CALL CREATE_INDEX('UsedIpVO', 'idxUsedIpVOipInBinary', 'ipInBinary');

    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeIpInBinaryColumn();
DROP PROCEDURE IF EXISTS upgradeIpInBinaryColumn;

CREATE TABLE IF NOT EXISTS `zstack`.`VmGuestNetworkInfoVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` char(32) NOT NULL,
    `vmNicUuid` char(32) DEFAULT NULL,
    `ipAddress` varchar(128) DEFAULT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `dnsList` varchar(255) DEFAULT NULL,
    `routeList` varchar(1024) DEFAULT NULL,
    `ipv6Address` varchar(128) DEFAULT NULL,
    `ipv6Gateway` varchar(128) DEFAULT NULL,
    `dns6List` varchar(255) DEFAULT NULL,
    `route6List` varchar(1024) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    INDEX `idxVmGuestNetworkInfoVOipAddress` (`ipAddress`),
    INDEX `idxVmGuestNetworkInfoVOipv6Address` (`ipv6Address`),
    CONSTRAINT `fkVmGuestNetworkInfoVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmGuestNetworkInfoVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;
DELIMITER $$
CREATE PROCEDURE createThickProvisionVolumeTag()
BEGIN
    DECLARE volUuid VARCHAR(32);
    DECLARE newTagUuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;

    DECLARE volCursor CURSOR FOR
        SELECT uuid
        FROM zstack.VolumeVO
        WHERE type = 'Memory'
          AND primaryStorageUuid IN (
            SELECT uuid
            FROM zstack.PrimaryStorageVO
            WHERE type = 'SharedBlock'
        )
          AND uuid NOT IN (
            SELECT resourceUuid
            FROM zstack.SystemTagVO
            WHERE resourceType = 'VolumeVO'
              AND tag = 'volumeProvisioningStrategy::ThickProvisioning'
        );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN volCursor;

    read_loop:
    LOOP
        FETCH volCursor INTO volUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET newTagUuid = REPLACE(UUID(), '-', '');

        INSERT INTO zstack.SystemTagVO (uuid, resourceUuid, resourceType, inherent, type, tag, createDate, lastOpDate)
        VALUES (newTagUuid, volUuid, 'VolumeVO', 0, 'System', 'volumeProvisioningStrategy::ThickProvisioning', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;

    CLOSE volCursor;
    SELECT CURTIME() AS finishTime;
END $$
DELIMITER ;

CALL createThickProvisionVolumeTag();
DROP PROCEDURE IF EXISTS createThickProvisionVolumeTag;

DROP PROCEDURE IF EXISTS `UPGRADE_VM_METADATA_TABLES_IDEMPOTENT`;
DELIMITER $$
CREATE PROCEDURE `UPGRADE_VM_METADATA_TABLES_IDEMPOTENT`()
BEGIN
    DECLARE old_fk_checks INT DEFAULT 1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = old_fk_checks;
        RESIGNAL;
    END;

    SET FOREIGN_KEY_CHECKS = 0;

    CALL RENAME_TABLE('VmInstanceDeviceAddressVO', 'VmInstanceResourceMetadataVO');
    CALL RENAME_TABLE('VmInstanceDeviceAddressGroupVO', 'VmInstanceResourceMetadataGroupVO');
    CALL RENAME_TABLE('VmInstanceDeviceAddressArchiveVO', 'VmInstanceResourceMetadataArchiveVO');

    CALL GENERIC_ADD_FOREIGN_KEY('VmInstanceResourceMetadataVO','fkVmInstanceResourceMetadataVOVmInstanceEO', 'vmInstanceUuid', 'VmInstanceEO', 'uuid', 'CASCADE');
    CALL GENERIC_ADD_FOREIGN_KEY('VmInstanceResourceMetadataGroupVO','fkVmInstanceResourceMetadataGroupVOVmInstanceEO', 'vmInstanceUuid', 'VmInstanceEO', 'uuid', 'CASCADE');
    CALL GENERIC_ADD_FOREIGN_KEY('VmInstanceResourceMetadataArchiveVO','fkVmInstanceResourceMetadataArchiveVOVmInstanceEO', 'vmInstanceUuid', 'VmInstanceEO', 'uuid', 'CASCADE');
    CALL GENERIC_ADD_FOREIGN_KEY('VmInstanceResourceMetadataArchiveVO','fkVmMetadataArchiveVOVmMetadataGroupVO', 'addressGroupUuid', 'VmInstanceResourceMetadataGroupVO', 'uuid', 'CASCADE');

    SET FOREIGN_KEY_CHECKS = 1;
END $$
DELIMITER ;

CALL UPGRADE_VM_METADATA_TABLES_IDEMPOTENT();
