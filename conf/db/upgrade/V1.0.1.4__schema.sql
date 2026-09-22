-- Feature: Allocator Domain (scheduling domain) | ZSV-13008

CREATE TABLE IF NOT EXISTS `zstack`.`AllocatorDomainVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL DEFAULT 'Enabled',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    INDEX `idxAllocatorDomainVOZoneUuid` (`zoneUuid`),
    CONSTRAINT `fkAllocatorDomainVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAllocatorDomainVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AllocatorDomainRuleVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `domainUuid` varchar(32) NOT NULL,
    `priority` int(11) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `resourceQualifier` varchar(32) DEFAULT NULL,
    `resourcePolicy` text DEFAULT NULL,
    `effect` varchar(32) NOT NULL,
    `policyFrom` varchar(255) DEFAULT NULL,
    `policyUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukAllocatorDomainRule` (`domainUuid`, `resourceType`, `resourceUuid`, `resourceQualifier`, `effect`),
    UNIQUE KEY `ukAllocatorDomainRulePriority` (`domainUuid`, `priority`),
    INDEX `idxAllocatorDomainRuleVOResource` (`resourceType`, `resourceUuid`),
    CONSTRAINT `fkAllocatorDomainRuleVOAllocatorDomainVO` FOREIGN KEY (`domainUuid`) REFERENCES `AllocatorDomainVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AllocatorDomainSceneVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `domainUuid` varchar(32) NOT NULL,
    `scene` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ukAllocatorDomainScene` (`domainUuid`, `scene`),
    CONSTRAINT `fkAllocatorDomainSceneVOAllocatorDomainVO` FOREIGN KEY (`domainUuid`) REFERENCES `AllocatorDomainVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Unused vm.resourceBinding.Scene (AllocationScene Auto/All removed)
DELETE FROM `zstack`.`GlobalConfigVO`
 WHERE `category` = 'vm' AND `name` = 'resourceBinding.Scene';

CALL ADD_COLUMN('VolumeCbtBackupRecordVO', 'cleanupAttemptUuid', 'varchar(32)', 1, NULL);
