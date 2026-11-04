CREATE TABLE `zstack`.`TemplatedVmInstanceVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`TemplatedVmInstanceCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `templatedVmInstanceUuid` char(32) NOT NULL,
    `cacheVmInstanceUuid` char(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`TemplatedVmInstanceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `templatedVmInstanceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;