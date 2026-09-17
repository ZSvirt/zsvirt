CREATE TABLE IF NOT EXISTS `zstack`.`VmCustomSpecificationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `platform` varchar(32) NOT NULL,
    `hostname` varchar(255) DEFAULT NULL,
    `rootPassword` varchar(255) DEFAULT NULL,
    `generateSID` boolean DEFAULT FALSE,
    `domainMode` varchar(32) DEFAULT 'WorkGroup',
    `domainName` varchar(255) DEFAULT NULL,
    `domainUsername` varchar(255) DEFAULT NULL,
    `domainPassword` varchar(255) DEFAULT NULL,
    `organization` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVmCustomSpecificationVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM `zstack`.`ResourceConfigVO` WHERE category='sharedblock'  AND name='qcow2.allocation' AND value='metadata';
