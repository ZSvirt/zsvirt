-- Feature: vTPM & Secure Boot | ZSPHER-1, ZSPHER-14

CREATE TABLE IF NOT EXISTS `zstack`.`TpmVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `vmInstanceUuid` char(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkTpmVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmHostFileVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `vmInstanceUuid` char(32) NOT NULL,
    `hostUuid` char(32) NOT NULL,
    `type` varchar(64) NOT NULL COMMENT 'NvRam, TpmState',
    `path` varchar(1024) NOT NULL COMMENT 'Absolute path of the file on the host',
    `lastSyncReason` varchar(255) DEFAULT NULL COMMENT 'The reason for the last sync operation',
    `changeDate` timestamp NULL DEFAULT NULL COMMENT 'Timestamp when file was reported changed, null after sync',
    `lastSyncDate` timestamp NULL DEFAULT NULL COMMENT 'Timestamp of the last successful sync',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    INDEX `idxVmHostFileVOVmInstanceUuid` (`vmInstanceUuid`),
    INDEX `idxVmHostFileVOHostUuid` (`hostUuid`),
    UNIQUE KEY `ukVmHostFileVO` (`vmInstanceUuid`, `hostUuid`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmHostBackupFileVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `resourceUuid` char(32) NOT NULL,
    `type` varchar(64) NOT NULL COMMENT 'NvRam, TpmState',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukVmHostBackupFileVO` (`resourceUuid`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmHostFileContentVO` (
    `uuid` char(32) NOT NULL UNIQUE COMMENT 'VmHostFileVO.uuid or VmHostBackupFileVO.uuid',
    `content` MEDIUMBLOB DEFAULT NULL,
    `format` varchar(64) NOT NULL COMMENT 'Raw, TarballGzip',
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkVmHostFileContentVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Feature: KMS | ZSPHER-46, ZSPHER-60, ZSPHER-61, ZSPHER-62

CREATE TABLE IF NOT EXISTS `zstack`.`KeyProviderVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(32) NOT NULL,
    `connected` boolean NOT NULL DEFAULT FALSE,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukKeyProviderVOName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`KmsVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `endpoint` varchar(255) NOT NULL,
    `port` int unsigned NOT NULL,
    `kmipVersion` varchar(32) DEFAULT NULL,
    `username` varchar(255) DEFAULT NULL,
    `password` varchar(255) DEFAULT NULL,
    `trustState` varchar(32) NOT NULL DEFAULT 'MUTUAL_UNTRUSTED',
    `activeIdentityUuid` varchar(32) DEFAULT NULL,
    `serverCertExpiredDate` timestamp NULL DEFAULT NULL,
    `serverCertPem` text DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    INDEX `idxKmsVOActiveIdentityUuid` (`activeIdentityUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`KmsIdentityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `kmsUuid` varchar(32) NOT NULL,
    `identityType` varchar(32) NOT NULL,
    `clientCertPem` text DEFAULT NULL,
    `clientKeyPem` text DEFAULT NULL,
    `csrPem` text DEFAULT NULL,
    `certExpiredDate` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukKmsIdentityVOKmsUuidType` (`kmsUuid`, `identityType`),
    INDEX `idxKmsIdentityVOKmsUuid` (`kmsUuid`),
    CONSTRAINT `fkKmsIdentityVOKmsVO` FOREIGN KEY (`kmsUuid`) REFERENCES `KmsVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NkpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `kdf` varchar(64) NOT NULL,
    `saltPolicy` varchar(64) NOT NULL,
    `backedUp` boolean NOT NULL DEFAULT FALSE,
    `currentVersion` int unsigned DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKeyIdentityVO` (
    `hostUuid` varchar(32) NOT NULL UNIQUE,
    `publicKey` text NOT NULL,
    `fingerprint` varchar(128) NOT NULL,
    `verified` boolean NOT NULL DEFAULT FALSE,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`hostUuid`),
    CONSTRAINT `fkHostKeyIdentityVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`EncryptedResourceKeyRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceType` varchar(255) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `providerUuid` varchar(32) DEFAULT NULL,
    `providerName` varchar(255) NOT NULL,
    `keyVersion` int unsigned DEFAULT NULL,
    `kekRef` varchar(255) DEFAULT NULL,
    `wrappedDek` text NOT NULL,
    `algorithm` varchar(64) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    INDEX `idxEncryptedResourceKeyRefVOResource` (`resourceType`, `resourceUuid`),
    INDEX `idxEncryptedResourceKeyRefVOProviderUuid` (`providerUuid`),
    INDEX `idxEncryptedResourceKeyRefVOProviderName` (`providerName`),
    CONSTRAINT `fkEncryptedResourceKeyRefVOProviderUuid` FOREIGN KEY (`providerUuid`) REFERENCES `KeyProviderVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmMetadataDirtyVO` (
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `managementNodeUuid` VARCHAR(32) DEFAULT NULL,
    `dirtyVersion` BIGINT NOT NULL DEFAULT 1,
    `lastClaimTime` TIMESTAMP NULL DEFAULT NULL,
    `storageStructureChange` TINYINT(1) NOT NULL DEFAULT 0,
    `retryCount` INT NOT NULL DEFAULT 0,
    `nextRetryTime` TIMESTAMP NULL DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`vmInstanceUuid`),
    INDEX `idx_VmMetadataDirtyVO_unclaimed` (`managementNodeUuid`, `nextRetryTime`, `lastOpDate`),
    CONSTRAINT `fkVmMetadataDirtyVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`)  REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmMetadataDirtyVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmMetadataFlushStateVO` (
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `metadataSnapshot` LONGTEXT,
    `lastFlushFinishTime` TIMESTAMP NULL DEFAULT NULL,
    `pendingStaleRecovery` TINYINT(1) NOT NULL DEFAULT 0,
    `staleRecoveryCount` INT NOT NULL DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`vmInstanceUuid`),
    CONSTRAINT `fkVmMetadataFlushStateVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`)  REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
