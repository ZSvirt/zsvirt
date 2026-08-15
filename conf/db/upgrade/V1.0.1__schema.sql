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
    UNIQUE KEY `ukExternalServiceConfigurationVOServiceType` (`serviceType`)
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

-- Feature: replace VpcXxxVO.accountName by accountUuid | ZSV-12380
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

-- Feature: support duplicate name of accounts | ZSV-12379
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
