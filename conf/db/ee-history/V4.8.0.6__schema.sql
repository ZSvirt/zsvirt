-- in version zsv_4.3.0
-- Feature: LDAP Enhance | ZSV-5531

CREATE TABLE IF NOT EXISTS `zstack`.`ThirdPartyAccountSourceVO` (
    `uuid` char(32) not null unique,
    `description` varchar(2048) default null,
    `type` varchar(32) not null,
    `createAccountStrategy` varchar(32) not null,
    `deleteAccountStrategy` varchar(32) not null,
    `lastOpDate` timestamp on update current_timestamp,
    `createDate` timestamp,
    primary key (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AccountThirdPartyAccountSourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `credentials` varchar(255) not null,
    `accountSourceUuid` char(32) not null,
    `accountUuid` char(32) not null,
    `lastOpDate` timestamp on update current_timestamp,
    `createDate` timestamp,
    primary key (`id`),
    UNIQUE KEY `credentialsAccountSourceUuid` (credentials,accountSourceUuid) USING BTREE,
    CONSTRAINT `fkAccountSourceRefVOThirdPartyAccountSourceVO` FOREIGN KEY (`accountSourceUuid`) REFERENCES ThirdPartyAccountSourceVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAccountSourceRefVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES AccountVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL DROP_COLUMN('LdapServerVO', 'scope');
CALL DROP_COLUMN('LdapServerVO', 'lastOpDate');
CALL DROP_COLUMN('LdapServerVO', 'createDate');
CALL DROP_COLUMN('LdapServerVO', 'description');
CALL DROP_COLUMN('LdapServerVO', 'name');

CALL ADD_COLUMN('LdapServerVO', 'serverType', 'varchar(32)', 0, 'WindowsAD');
CALL ADD_COLUMN('LdapServerVO', 'filter', 'varchar(2048)', 1, NULL);
CALL ADD_COLUMN('LdapServerVO', 'usernameProperty', 'varchar(255)', 0, 'cn');
DROP TABLE IF EXISTS `zstack`.`LdapAccountRefVO`;
DROP TABLE IF EXISTS `zstack`.`LdapResourceRefVO`;

CALL INSERT_COLUMN('AccountVO', 'state', 'varchar(128)', 0, 'Enabled', 'type');

-- Feature: Backup Management | ZSV-5764

CALL INSERT_COLUMN('SchedulerJobGroupVO', 'zoneUuid', 'varchar(32)', 1, NULL, 'state');
CALL INSERT_COLUMN('SchedulerJobGroupVO', 'managementNodeUuid', 'varchar(32)', 1, NULL, 'zoneUuid');
ALTER TABLE `zstack`.`SchedulerJobGroupVO` ADD CONSTRAINT `fkSchedulerJobGroupVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL;
CALL INSERT_COLUMN('SchedulerJobGroupJobRefVO', 'priority', 'int', 0, 0, 'schedulerJobGroupUuid');

-- Feature: support host without bond to attach | ZSV-5925

CALL INSERT_COLUMN('L2VirtualSwitchNetworkVO', 'vSwitchIndex', 'INT unsigned', 1, NULL, 'uuid');
DELETE FROM `zstack`.`L2NetworkHostRefVO` WHERE `attachStatus` = 'Detached';
call DROP_COLUMN('L2NetworkHostRefVO', 'attachStatus');
CALL INSERT_COLUMN('L2NetworkHostRefVO', 'bridgeName', 'varchar(16)', 1, NULL, 'l2ProviderType');

CREATE TABLE IF NOT EXISTS `zstack`.`UplinkGroupVO` (
    `id` bigint unsigned NOT NULL UNIQUE,
    `interfaceName` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `bondingUuid` varchar(32) DEFAULT NULL,
    `interfaceUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkUplinkGroupVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES HostNetworkBondingVO (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkUplinkGroupVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Other

DELETE FROM `zstack`.`EncryptEntityMetadataVO` WHERE `entityName` = 'IAM2VirtualIDVO';
DELETE FROM `zstack`.`EncryptEntityMetadataVO` WHERE `entityName` = 'IAM2ProjectAttributeVO';
