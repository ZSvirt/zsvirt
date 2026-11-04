ALTER TABLE `zstack`.`PlatformServicePackageVO`
    ADD COLUMN `backupStorageUuid` char(32) DEFAULT NULL AFTER `imageUuid`;
