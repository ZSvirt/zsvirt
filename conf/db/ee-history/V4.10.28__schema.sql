-- Feature: Progress Refactor | ZSV-9610

DROP TABLE IF EXISTS `zstack`.`TaskProgressVO`;
CREATE TABLE IF NOT EXISTS `zstack`.`TaskProgressVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `apiId` char(32) NOT NULL,
    `content` varchar(255) DEFAULT NULL,
    `opaque` text,
    `createTime` bigint unsigned NOT NULL,
    `lastOpTime` bigint unsigned NOT NULL,
    `currentStep` bigint unsigned DEFAULT 0,
    `totalStep` bigint unsigned DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idxTaskProgressVOApiId` (`apiId`),
    KEY `idxTaskProgressVOLastOpTime` (`lastOpTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Others

ALTER TABLE `zstack`.`HostNetworkInterfaceVO` MODIFY COLUMN `deviceName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` MODIFY COLUMN `vendorName` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` MODIFY COLUMN `subvendorName` VARCHAR(255) DEFAULT NULL;

