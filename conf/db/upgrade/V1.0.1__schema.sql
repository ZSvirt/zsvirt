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
