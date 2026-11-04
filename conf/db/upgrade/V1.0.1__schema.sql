CREATE TABLE IF NOT EXISTS `zstack`.`TpmKeyBackupVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM `EncryptedResourceKeyRefVO`
       WHERE `resourceUuid` NOT IN (SELECT `uuid` FROM `ResourceVO`);
ALTER TABLE `EncryptedResourceKeyRefVO`
        ADD CONSTRAINT `fkEncryptedResourceKeyRefResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO`(`uuid`)
        ON DELETE CASCADE;

-- Feature: ZCenter Account | ZSV-12257, ZSV-12379, ZSV-12380

CALL INSERT_COLUMN('AccountVO', 'source', 'varchar(32)', 0, 'Local', 'type');

UPDATE `zstack`.`AccountVO` a
INNER JOIN `zstack`.`AccountThirdPartyAccountSourceRefVO` ref ON ref.accountUuid = a.uuid
INNER JOIN `zstack`.`LdapServerVO` ldap ON ldap.uuid = ref.accountSourceUuid
SET a.`source` = IF(ldap.serverType IN ('OpenLdap', 'WindowsAD'), ldap.serverType, 'WindowsAD');

UPDATE `zstack`.`AccountVO` a
INNER JOIN `zstack`.`AccountThirdPartyAccountSourceRefVO` ref ON ref.accountUuid = a.uuid
INNER JOIN `zstack`.`ThirdPartyAccountSourceVO` src ON src.uuid = ref.accountSourceUuid
SET a.`source` = src.type
WHERE src.type IN ('CAS', 'OAuth2');

UPDATE `zstack`.`AccountVO`
SET `type` = 'Normal'
WHERE `type` = 'ThirdParty';

UPDATE `zstack`.`ThirdPartyAccountSourceVO` src
INNER JOIN `zstack`.`LdapServerVO` ldap ON ldap.uuid = src.uuid
SET src.`type` = IF(ldap.serverType IN ('OpenLdap', 'WindowsAD'), ldap.serverType, 'WindowsAD')
WHERE src.`type` = 'ldap';

CALL INSERT_COLUMN('ThirdPartyAccountSourceVO', 'updateAccountStrategies', 'varchar(255)', 0, '', 'createAccountStrategy');

-- VpcUserVpnGatewayVO
-- the old unique key starts with dataCenterUuid, which also backs fkVpcUserVpnGatewayVODataCenterVO,
-- so add a temporary index on dataCenterUuid before dropping the old unique key (avoids ERROR 1553).
CALL INSERT_COLUMN('VpcUserVpnGatewayVO', 'accountUuid', 'varchar(32)', 1, NULL, 'uuid');
UPDATE `zstack`.`VpcUserVpnGatewayVO` t
    JOIN `zstack`.`AccountVO` a ON t.`accountName` = a.`name`
    SET t.`accountUuid` = a.`uuid`;
ALTER TABLE `zstack`.`VpcUserVpnGatewayVO` MODIFY `accountUuid` varchar(32) NOT NULL;
CALL DROP_FOREIGN_KEY('VpcUserVpnGatewayVO', 'fkVpcUserVpnGatewayVOAccountVO');
-- dropping the FK leaves a same-named index on accountName; drop it so the new FK name does not collide (avoids ERROR 1061).
CALL DELETE_INDEX('VpcUserVpnGatewayVO', 'fkVpcUserVpnGatewayVOAccountVO');
CALL CREATE_INDEX('VpcUserVpnGatewayVO', 'idxTmpVpcUserVpnGatewayVODataCenterUuid', 'dataCenterUuid');
CALL DELETE_INDEX('VpcUserVpnGatewayVO', 'ukVpcUserVpnGatewayVO');
ALTER TABLE `zstack`.`VpcUserVpnGatewayVO`
    ADD UNIQUE KEY `ukVpcUserVpnGatewayVO` (`dataCenterUuid`, `accountUuid`, `gatewayId`) USING BTREE;
CALL DELETE_INDEX('VpcUserVpnGatewayVO', 'idxTmpVpcUserVpnGatewayVODataCenterUuid');
CALL ADD_CONSTRAINT('VpcUserVpnGatewayVO', 'fkVpcUserVpnGatewayVOAccountVO', 'accountUuid', 'AccountVO', 'uuid', 'RESTRICT');
CALL DROP_COLUMN('VpcUserVpnGatewayVO', 'accountName');

-- VpcVpnGatewayVO
-- the old unique key starts with vSwitchUuid, which also backs fkVpcVpnGatewayVOEcsVSwitchVO,
-- so add a temporary index on vSwitchUuid before dropping the old unique key (avoids ERROR 1553).
CALL INSERT_COLUMN('VpcVpnGatewayVO', 'accountUuid', 'varchar(32)', 1, NULL, 'uuid');
UPDATE `zstack`.`VpcVpnGatewayVO` t
    JOIN `zstack`.`AccountVO` a ON t.`accountName` = a.`name`
    SET t.`accountUuid` = a.`uuid`;
ALTER TABLE `zstack`.`VpcVpnGatewayVO` MODIFY `accountUuid` varchar(32) NOT NULL;
CALL DROP_FOREIGN_KEY('VpcVpnGatewayVO', 'fkVpcVpnGatewayVOAccountVO');
CALL DELETE_INDEX('VpcVpnGatewayVO', 'fkVpcVpnGatewayVOAccountVO');
CALL CREATE_INDEX('VpcVpnGatewayVO', 'idxTmpVpcVpnGatewayVOVSwitchUuid', 'vSwitchUuid');
CALL DELETE_INDEX('VpcVpnGatewayVO', 'ukVpcVpnGatewayVO');
ALTER TABLE `zstack`.`VpcVpnGatewayVO`
    ADD UNIQUE KEY `ukVpcVpnGatewayVO` (`vSwitchUuid`, `accountUuid`, `gatewayId`) USING BTREE;
CALL DELETE_INDEX('VpcVpnGatewayVO', 'idxTmpVpcVpnGatewayVOVSwitchUuid');
CALL ADD_CONSTRAINT('VpcVpnGatewayVO', 'fkVpcVpnGatewayVOAccountVO', 'accountUuid', 'AccountVO', 'uuid', 'RESTRICT');
CALL DROP_COLUMN('VpcVpnGatewayVO', 'accountName');

-- VpcVpnConnectionVO
-- the old unique key starts with connectionId (no foreign key on it), so no temporary index is needed.
CALL INSERT_COLUMN('VpcVpnConnectionVO', 'accountUuid', 'varchar(32)', 1, NULL, 'uuid');
UPDATE `zstack`.`VpcVpnConnectionVO` t
    JOIN `zstack`.`AccountVO` a ON t.`accountName` = a.`name`
    SET t.`accountUuid` = a.`uuid`;
ALTER TABLE `zstack`.`VpcVpnConnectionVO` MODIFY `accountUuid` varchar(32) NOT NULL;
CALL DROP_FOREIGN_KEY('VpcVpnConnectionVO', 'fkVpcVpnConnectionVOAccountVO');
CALL DELETE_INDEX('VpcVpnConnectionVO', 'fkVpcVpnConnectionVOAccountVO');
CALL DELETE_INDEX('VpcVpnConnectionVO', 'ukVpcVpnConnectionVO');
ALTER TABLE `zstack`.`VpcVpnConnectionVO`
    ADD UNIQUE KEY `ukVpcVpnConnectionVO` (`connectionId`, `accountUuid`, `userGatewayUuid`) USING BTREE;
CALL ADD_CONSTRAINT('VpcVpnConnectionVO', 'fkVpcVpnConnectionVOAccountVO', 'accountUuid', 'AccountVO', 'uuid', 'RESTRICT');
CALL DROP_COLUMN('VpcVpnConnectionVO', 'accountName');

-- VpcVpnIkeConfigVO (without accountName unique key)
CALL INSERT_COLUMN('VpcVpnIkeConfigVO', 'accountUuid', 'varchar(32)', 1, NULL, 'uuid');
UPDATE `zstack`.`VpcVpnIkeConfigVO` t
    JOIN `zstack`.`AccountVO` a ON t.`accountName` = a.`name`
    SET t.`accountUuid` = a.`uuid`;
ALTER TABLE `zstack`.`VpcVpnIkeConfigVO` MODIFY `accountUuid` varchar(32) NOT NULL;
CALL DROP_FOREIGN_KEY('VpcVpnIkeConfigVO', 'fkVpcVpnIkeConfigVOAccountVO');
CALL DELETE_INDEX('VpcVpnIkeConfigVO', 'fkVpcVpnIkeConfigVOAccountVO');
CALL ADD_CONSTRAINT('VpcVpnIkeConfigVO', 'fkVpcVpnIkeConfigVOAccountVO', 'accountUuid', 'AccountVO', 'uuid', 'RESTRICT');
CALL DROP_COLUMN('VpcVpnIkeConfigVO', 'accountName');

-- VpcVpnIpSecConfigVO (without accountName unique key)
CALL INSERT_COLUMN('VpcVpnIpSecConfigVO', 'accountUuid', 'varchar(32)', 1, NULL, 'uuid');
UPDATE `zstack`.`VpcVpnIpSecConfigVO` t
    JOIN `zstack`.`AccountVO` a ON t.`accountName` = a.`name`
    SET t.`accountUuid` = a.`uuid`;
ALTER TABLE `zstack`.`VpcVpnIpSecConfigVO` MODIFY `accountUuid` varchar(32) NOT NULL;
CALL DROP_FOREIGN_KEY('VpcVpnIpSecConfigVO', 'fkVpcVpnIpSecConfigVOAccountVO');
CALL DELETE_INDEX('VpcVpnIpSecConfigVO', 'fkVpcVpnIpSecConfigVOAccountVO');
CALL ADD_CONSTRAINT('VpcVpnIpSecConfigVO', 'fkVpcVpnIpSecConfigVOAccountVO', 'accountUuid', 'AccountVO', 'uuid', 'RESTRICT');
CALL DROP_COLUMN('VpcVpnIpSecConfigVO', 'accountName');

CALL CREATE_INDEX('AccountVO', 'idxAccountVOname', 'name');
CALL DELETE_INDEX('AccountVO', 'name');
ALTER TABLE `zstack`.`AccountVO` ADD CONSTRAINT `uqAccountVOSourceName` UNIQUE (`source`, `name`);

-- Feature: ZCenter License & License Client | ZSV-12274, ZSV-12506

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizedNodeVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `appId` char(32) NOT NULL,
    `ip` varchar(255) NOT NULL,
    `port` int DEFAULT NULL,
    `lastSyncDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    `status` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `protocol` varchar(64) NOT NULL DEFAULT 'Normal',
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizedCapacityVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nodeUuid` char(32) NOT NULL,
    `prodInfo` varchar(255) NOT NULL,
    `quotaType` varchar(64) NOT NULL,
    `quota` bigint unsigned NOT NULL DEFAULT 0,
    `licenseType` varchar(64) NOT NULL,
    `state` varchar(64) NOT NULL DEFAULT 'active',
    `issueTime` bigint unsigned NOT NULL DEFAULT 0,
    `expireTime` bigint unsigned NOT NULL DEFAULT 0,
    `localUsed` bigint unsigned NOT NULL DEFAULT 0,
    `otherUsed` bigint unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkLicenseAuthorizedCapacityLicenseAuthorizedNode` FOREIGN KEY (`nodeUuid`) REFERENCES `LicenseAuthorizedNodeVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LicenseAuthorizeHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nodeUuid` char(32) NOT NULL,
    `resourceUuid` char(32) DEFAULT NULL,
    `quotaType` varchar(64) NOT NULL,
    `usageFrom` bigint unsigned DEFAULT 0,
    `usageTo` bigint unsigned DEFAULT NULL,
    `quota` bigint unsigned DEFAULT 0,
    `licenseType` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `action` varchar(255) NOT NULL,
    `result` varchar(64) DEFAULT NULL,
    `error` text DEFAULT NULL,
    `requestUuid` char(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: VM boot time | ZSV-12297

CALL INSERT_COLUMN('VmInstanceEO', 'bootTime', 'timestamp NULL', 1, NULL, 'state');

CREATE OR REPLACE VIEW `zstack`.`VmInstanceVO` AS
SELECT uuid,
       name,
       description,
       zoneUuid,
       clusterUuid,
       imageUuid,
       hostUuid,
       internalId,
       lastHostUuid,
       instanceOfferingUuid,
       rootVolumeUuid,
       defaultL3NetworkUuid,
       type,
       hypervisorType,
       cpuNum,
       cpuSpeed,
       memorySize,
       reservedMemorySize,
       platform,
       guestOsType,
       allocatorStrategy,
       createDate,
       lastOpDate,
       state,
       architecture,
       bootTime
FROM `zstack`.`VmInstanceEO`
WHERE deleted IS NULL;
-- Feature: Log Server | ZSV-12254

CREATE TABLE IF NOT EXISTS `zstack`.`LogServerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) NULL,
    `category` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `level` varchar(255) NULL,
    `state` varchar(255) NOT NULL DEFAULT 'Enabled',
    `configuration` text NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: External Service Configuration

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalServiceConfigurationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serviceType` varchar(32) NOT NULL,
    `configuration` text DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    INDEX `idxExternalServiceConfigurationVOServiceType` (`serviceType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: System AccessKey | ZSV-12416

CALL ADD_COLUMN('AccessKeyVO', 'type', 'varchar(32)', 0, 'User');
-- Feature: Resource notification webhook | ZSV-12415
CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifySubscriptionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `resourceTypes` text DEFAULT NULL,
    `eventTypes` varchar(256) DEFAULT NULL,
    `type` varchar(32) NOT NULL DEFAULT 'WEBHOOK',
    `state` varchar(32) NOT NULL DEFAULT 'Enabled',
    `accountUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_ResNotifySubscriptionVO_name` (`name`),
    INDEX `idx_ResNotifySubscriptionVO_accountUuid` (`accountUuid`),
    INDEX `idx_ResNotifySubscriptionVO_type_state` (`type`, `state`),
    CONSTRAINT `fkResNotifySubscriptionVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResNotifyWebhookRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `webhookUrl` text NOT NULL,
    `secret` varchar(256) DEFAULT NULL,
    `customHeaders` text,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_ResNotifyWebhookRefVO_webhookUrl` (`webhookUrl`(255)),
    CONSTRAINT `fk_ResNotifyWebhookRefVO_ResNotifySubscriptionVO`
        FOREIGN KEY (`uuid`) REFERENCES `ResNotifySubscriptionVO`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE shadow FROM ImageCacheShadowVO shadow
    LEFT JOIN PrimaryStorageEO ps ON shadow.primaryStorageUuid = ps.uuid
    WHERE ps.uuid IS NULL;
CALL ADD_CONSTRAINT('ImageCacheShadowVO', 'fkImageCacheShadowVOPrimaryStorageEOCascade', 'primaryStorageUuid', 'PrimaryStorageEO', 'uuid', 'CASCADE');

-- Feature: Platform Service deployment | ZSV-12505

CREATE TABLE IF NOT EXISTS `zstack`.`PlatformServicePackageVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `serviceType` varchar(32) NOT NULL,
    `artifactType` varchar(32) NOT NULL,
    `imageUuid` char(32) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `version` varchar(255) DEFAULT NULL,
    `activeApiId` varchar(255) DEFAULT NULL,
    `lastError` varchar(4096) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    INDEX `idxPlatformServicePackageVOServiceType` (`serviceType`),
    INDEX `idxPlatformServicePackageVOImageUuid` (`imageUuid`),
    INDEX `idxPlatformServicePackageVOStatus` (`status`),
    INDEX `idxPlatformServicePackageVOActiveApiId` (`activeApiId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PlatformServiceInstanceVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `serviceType` varchar(32) NOT NULL,
    `packageUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `healthStatus` varchar(32) NOT NULL,
    `version` varchar(255) DEFAULT NULL,
    `activeApiId` varchar(255) DEFAULT NULL,
    `lastError` varchar(4096) DEFAULT NULL,
    `lastHealthCheckTime` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    INDEX `idxPlatformServiceInstanceVOServiceType` (`serviceType`),
    INDEX `idxPlatformServiceInstanceVOPackageUuid` (`packageUuid`),
    INDEX `idxPlatformServiceInstanceVOVmInstanceUuid` (`vmInstanceUuid`),
    INDEX `idxPlatformServiceInstanceVOState` (`state`),
    INDEX `idxPlatformServiceInstanceVOActiveApiId` (`activeApiId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: LicenseHistoryVO cause | ZSV-12697
CALL INSERT_COLUMN('LicenseHistoryVO', 'cause', 'varchar(32)', 0, 'UpdateLicense', 'source');

-- Feature: serialize VM metadata cleanup with cluster-wide metadata flushes | ZSV-11867
CREATE TABLE IF NOT EXISTS `zstack`.`VmMetadataCleanupBarrierVO` (
    `id` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL DEFAULT 'Idle',
    `operationUuid` varchar(32) DEFAULT NULL,
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `leaseExpireDate` timestamp NULL DEFAULT NULL,
    `generation` bigint NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVmMetadataCleanupBarrierVOManagementNodeVO`
        FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`VmMetadataCleanupBarrierVO` (`id`, `state`, `createDate`)
VALUES ('vm-metadata-cleanup', 'Idle', CURRENT_TIMESTAMP);

-- Volume LUKS encryption flag (API opt-in + EncryptedResourceKeyRefVO binding)
-- VmInstanceEO.vmEncryption: VM sensitive system-tag encryption (consolePassword / sshkey / userdata)

ALTER TABLE `zstack`.`VolumeEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeSnapshotEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeBackupVO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `vmEncryption` tinyint(1) NOT NULL DEFAULT 0;

UPDATE `zstack`.`VolumeBackupVO` vb
SET vb.`encrypted` = 1
WHERE EXISTS (
    SELECT 1
    FROM `zstack`.`EncryptedResourceKeyRefVO` keyRef
    WHERE keyRef.`resourceType` = 'VolumeBackupVO'
      AND keyRef.`resourceUuid` = vb.`uuid`
);

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS
SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid,
       rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate,
       isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate, lastAttachDate, protocol, encrypted
FROM `zstack`.`VolumeEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotVO`;
CREATE VIEW `zstack`.`VolumeSnapshotVO` AS
SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid,
       primaryStorageUuid, primaryStorageInstallPath, distance, size, latest,
       fullSnapshot, encrypted, volumeType, state, status, createDate, lastOpDate
FROM `zstack`.`VolumeSnapshotEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS
SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId,
       lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type,
       hypervisorType, cpuNum, cpuSpeed, memorySize, reservedMemorySize, platform,
       guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture, bootTime, vmEncryption
FROM `zstack`.`VmInstanceEO`
WHERE deleted IS NULL;

-- Host PKI

CREATE TABLE IF NOT EXISTS `zstack`.`PkiCaVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `scope` varchar(64) NOT NULL,
    `caType` varchar(32) NOT NULL,
    `subjectDn` varchar(512) DEFAULT NULL,
    `certChainPem` mediumtext NOT NULL,
    `encryptedPrivateKeyPem` mediumtext DEFAULT NULL,
    `serial` varchar(128) DEFAULT NULL,
    `fingerprint` varchar(128) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `crlPem` mediumtext DEFAULT NULL,
    `notBefore` timestamp NULL DEFAULT NULL,
    `notAfter` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukPkiCaScopeType` (`scope`, `caType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostCertificateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL,
    `caUuid` varchar(32) NOT NULL,
    `certUsage` varchar(64) NOT NULL,
    `serial` varchar(128) DEFAULT NULL,
    `fingerprint` varchar(128) DEFAULT NULL,
    `sanSnapshot` varchar(2048) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `notBefore` timestamp NULL DEFAULT NULL,
    `notAfter` timestamp NULL DEFAULT NULL,
    `lastInstallDate` timestamp NULL DEFAULT NULL,
    `lastError` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukHostCertificateHostUsage` (`hostUuid`, `certUsage`),
    KEY `idxHostCertificateCaUuid` (`caUuid`),
    CONSTRAINT `fkHostCertificateHost` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostCertificateCa` FOREIGN KEY (`caUuid`) REFERENCES `zstack`.`PkiCaVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Key provider / scanner alarm state (persists ACTIVE/INACTIVE across MN restart)

CREATE TABLE IF NOT EXISTS `zstack`.`ScannerAlarmStateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `scannerName` varchar(255) NOT NULL,
    `alarmType` varchar(255) NOT NULL,
    `alarmKey` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastUpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukScannerAlarmState` (`scannerName`, `alarmType`, `alarmKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Cleanup | ZSV-12537

DROP TRIGGER IF EXISTS `trigger_attach_eip_for_ecsinstance`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_DiskOfferingEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_EipVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_ImageEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_InstanceOfferingEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_IpRangeEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_L3NetworkEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_LoadBalancerListenerVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_LoadBalancerVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_PolicyVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_PortForwardingRuleVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_QuotaVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_SchedulerVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_SecurityGroupVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_UserGroupVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_UserVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_VipVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_VmInstanceEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_VmNicVO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_VolumeEO`;
DROP TRIGGER IF EXISTS `trigger_clean_AccountResourceRefVO_for_VolumeSnapshotEO`;
DROP TRIGGER IF EXISTS `trigger_cleanup_for_VmInstanceEO_hard_delete`;
DROP TRIGGER IF EXISTS `trigger_cleanup_for_VolumeEO_hard_delete`;
DROP TRIGGER IF EXISTS `trigger_cleanup_for_VolumeEO_soft_delete`;
DROP TRIGGER IF EXISTS `trigger_decrease_vswitch_for_create_ecs`;

DROP FUNCTION IF EXISTS `INET6_ATON`;
DROP FUNCTION IF EXISTS `Json_simpleGetKeyValue`;
DROP FUNCTION IF EXISTS `Separate_ceph_pool`;
DROP FUNCTION IF EXISTS `Upgrade_Scheduler`;
DROP FUNCTION IF EXISTS `update_policy`;

DROP PROCEDURE IF EXISTS `AddFkPciDeviceVOVmInstanceEO`;
DROP PROCEDURE IF EXISTS `AlterBlockScsiLunTable`;
DROP PROCEDURE IF EXISTS `Alter_Ceph_Table`;
DROP PROCEDURE IF EXISTS `Alter_SCSI_Table`;
DROP PROCEDURE IF EXISTS `Alter_Volume_Snapshot_Tree_Table`;
DROP PROCEDURE IF EXISTS `CheckAndCreateResourceConfig`;
DROP PROCEDURE IF EXISTS `CreateGpuDeviceVO`;
DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
DROP PROCEDURE IF EXISTS `UPGRADE_VM_METADATA_TABLES_IDEMPOTENT`;
DROP PROCEDURE IF EXISTS `UpdateVolumeBackupVmInstanceUuid`;
DROP PROCEDURE IF EXISTS `Update_Vip_Account`;
DROP PROCEDURE IF EXISTS `addColumnsToSNSTextTemplateVO`;
DROP PROCEDURE IF EXISTS `addHaStrategyConditionOnVmHaLevel`;
DROP PROCEDURE IF EXISTS `addHostHaStatus`;
DROP PROCEDURE IF EXISTS `addLBServiceToPrivateBasicNetwork`;
DROP PROCEDURE IF EXISTS `addLongJobVOIndex`;
DROP PROCEDURE IF EXISTS `addMissingResourceRef`;
DROP PROCEDURE IF EXISTS `addPciDeviceVendor`;
DROP PROCEDURE IF EXISTS `addResourceConfigForVirtualRouter`;
DROP PROCEDURE IF EXISTS `addServiceToPublicNetwork`;
DROP PROCEDURE IF EXISTS `addSnatConfigForVirtualRouter`;
DROP PROCEDURE IF EXISTS `addSnatConfigForVpcHa`;
DROP PROCEDURE IF EXISTS `addZoneUuidOnAffinityGroupVO`;
DROP PROCEDURE IF EXISTS `changeL3NetworkDefaultIpversion`;
DROP PROCEDURE IF EXISTS `checkAllBlockHostInPrimaryHostRef`;
DROP PROCEDURE IF EXISTS `checkHostInitiatorRefVO`;
DROP PROCEDURE IF EXISTS `checkProjectAdminOfVirtualId`;
DROP PROCEDURE IF EXISTS `checkVirtualhostsExist`;
DROP PROCEDURE IF EXISTS `cleanDeprecatedGlobalConfig`;
DROP PROCEDURE IF EXISTS `cleanExpireVmUsageVO`;
DROP PROCEDURE IF EXISTS `copyDataNetworkTag`;
DROP PROCEDURE IF EXISTS `createAutoScalingVmTemplate`;
DROP PROCEDURE IF EXISTS `createRoleRefsInProject`;
DROP PROCEDURE IF EXISTS `createThickProvisionVolumeTag`;
DROP PROCEDURE IF EXISTS `createVmSchedulingRule`;
DROP PROCEDURE IF EXISTS `createVmSchedulingRuleGroupRef`;
DROP PROCEDURE IF EXISTS `deleteRoleReadAPI`;
DROP PROCEDURE IF EXISTS `deleteVmLevelCpuModeIfAlreadySetClusterLevelCpuMode`;
DROP PROCEDURE IF EXISTS `doFixResourceVOResourceType`;
DROP PROCEDURE IF EXISTS `enableVnicMultipleQueuesForVirtualRouter`;
DROP PROCEDURE IF EXISTS `fixMissingProjectAdminPolicy`;
DROP PROCEDURE IF EXISTS `fixMissingShareResourceVO`;
DROP PROCEDURE IF EXISTS `fixMissingTag2RoleInProjects`;
DROP PROCEDURE IF EXISTS `fixResourceVOResourceType`;
DROP PROCEDURE IF EXISTS `generateNetworkCategory`;
DROP PROCEDURE IF EXISTS `generateNormalpRangeVO`;
DROP PROCEDURE IF EXISTS `generatePciDeviceSpecVOAccountRef`;
DROP PROCEDURE IF EXISTS `generatePciDeviceVOAccountRef`;
DROP PROCEDURE IF EXISTS `generateVipForVirtualRouterPubIP`;
DROP PROCEDURE IF EXISTS `generateVpcRouterVmVO`;
DROP PROCEDURE IF EXISTS `getAdminAccountUUid`;
DROP PROCEDURE IF EXISTS `getIpRangeUuidForNetwork`;
DROP PROCEDURE IF EXISTS `getLinkedAccountUUid`;
DROP PROCEDURE IF EXISTS `getMaxAccountResourceRefVO`;
DROP PROCEDURE IF EXISTS `getProjectAccount`;
DROP PROCEDURE IF EXISTS `getRoleUuid`;
DROP PROCEDURE IF EXISTS `getServiceProviderForNetwork`;
DROP PROCEDURE IF EXISTS `getUseIpUuid`;
DROP PROCEDURE IF EXISTS `getVRouterGuestL3Network`;
DROP PROCEDURE IF EXISTS `getVirtualRouterUuidFromNicUuid`;
DROP PROCEDURE IF EXISTS `handleLegacyPciSpecUuidTags`;
DROP PROCEDURE IF EXISTS `iam2_Organization_Quota`;
DROP PROCEDURE IF EXISTS `initializeRecoveryTemplate`;
DROP PROCEDURE IF EXISTS `insertApplianceVmAffinityGroup`;
DROP PROCEDURE IF EXISTS `insertDefaultSecurityGroup`;
DROP PROCEDURE IF EXISTS `insertIAM2ProjectRole`;
DROP PROCEDURE IF EXISTS `insertIAM2ProjectRoleVOForProjectSystemRoles`;
DROP PROCEDURE IF EXISTS `insertIAM2ProjectVirtualIDGroupRef`;
DROP PROCEDURE IF EXISTS `insertIntoRole`;
DROP PROCEDURE IF EXISTS `insertVirtualRouterEipRefVO`;
DROP PROCEDURE IF EXISTS `insertVirtualRouterPortForwardingRuleRefVO`;
DROP PROCEDURE IF EXISTS `migrateBlockPrimaryHostRef`;
DROP PROCEDURE IF EXISTS `migrateClockTrackSystemTagToGlobalConfig`;
DROP PROCEDURE IF EXISTS `migrateIPsecL3Network`;
DROP PROCEDURE IF EXISTS `migrateIscsiLunVOToScsiLunVO`;
DROP PROCEDURE IF EXISTS `migrateReserveMemTagVO`;
DROP PROCEDURE IF EXISTS `migrateSchedulerJob`;
DROP PROCEDURE IF EXISTS `migrateUserTagVO`;
DROP PROCEDURE IF EXISTS `migrateVipPeerL3Network`;
DROP PROCEDURE IF EXISTS `modifyHostXfsFragAlarmDefaultThreshold`;
DROP PROCEDURE IF EXISTS `modifyVipNetworkServicesRefVO`;
DROP PROCEDURE IF EXISTS `moveClusterCpuModeFromSystemTagToResourceConfig`;
DROP PROCEDURE IF EXISTS `moveOsInfoToKVMHostVO`;
DROP PROCEDURE IF EXISTS `securityGroupRule`;
DROP PROCEDURE IF EXISTS `setApplianceVmCpuModeToNone`;
DROP PROCEDURE IF EXISTS `setDefaultUsbAttachType`;
DROP PROCEDURE IF EXISTS `splitHybridLicenseRecords`;
DROP PROCEDURE IF EXISTS `syncSnatDefaultL3ConfigForVirtualRouter`;
DROP PROCEDURE IF EXISTS `syncSnatDefaultL3ConfigForVpcHa`;
DROP PROCEDURE IF EXISTS `updateArchiveTicketStatusHistoryVO`;
DROP PROCEDURE IF EXISTS `updateClusterHostCpuModelCheckTag`;
DROP PROCEDURE IF EXISTS `updateIAM2VirtualIDRoleRefCreateAccountUuid`;
DROP PROCEDURE IF EXISTS `updateLoadBalancerListenerVO`;
DROP PROCEDURE IF EXISTS `updatePlatformAdminsWithoutZoneRelation`;
DROP PROCEDURE IF EXISTS `updateTicketStatusHistoryVO`;
DROP PROCEDURE IF EXISTS `upgradeChild`;
DROP PROCEDURE IF EXISTS `upgradeEmailAddressFromEndpoint`;
DROP PROCEDURE IF EXISTS `upgradeIAM2ReadRole`;
DROP PROCEDURE IF EXISTS `upgradeIpInBinaryColumn`;
DROP PROCEDURE IF EXISTS `upgradeOrganization`;
DROP PROCEDURE IF EXISTS `upgradePrivilegeAdmin`;
DROP PROCEDURE IF EXISTS `upgradeProjectAdminSystemTags`;
DROP PROCEDURE IF EXISTS `upgradeProjectOperatorSystemTags`;
DROP PROCEDURE IF EXISTS `upgradeSystemEndpointNotifier`;
DROP PROCEDURE IF EXISTS `upgradeVolumeSnapshotRefSystemTags`;

