-- ZSV-13141: update existing ZMigrate VM types
UPDATE `zstack`.`VmInstanceEO` vm
INNER JOIN `zstack`.`SystemTagVO` st
        ON st.`resourceUuid` = vm.`uuid`
       AND st.`resourceType` = 'VmInstanceVO'
       AND st.`tag` IN (
           'ZMigrateManagementNodeVm',
           'ZMigrateGatewayVm'
       )
SET vm.`type` = 'MevocoVm'
WHERE vm.`deleted` IS NULL
  AND vm.`type` <> 'MevocoVm';

-- Store UUIDs without hyphens (32 chars); convert to and from ZNS's 36-char UUIDs at the API boundary.
-- accessKeyId and physicalNetwork are not UUIDs, so they retain varchar(64).
CREATE TABLE IF NOT EXISTS ZnsInfoVO (
    uuid char(32) NOT NULL,
    computerManagerUuid char(32) NOT NULL,
    name varchar(255),
    description varchar(2048),
    ip varchar(255),
    username varchar(255),
    password text,
    vendorType varchar(255),
    vendorVersion varchar(255),
    accessKeyId varchar(64),
    accessKeySecret text,
    connectionGeneration bigint NOT NULL DEFAULT 1,
    status varchar(32),
    lastSyncDate timestamp NULL,
    createDate timestamp NULL,
    lastOpDate timestamp NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY `ukZnsInfoVOComputerManagerUuid` (computerManagerUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZnsControllerApiCompatibilityVO` (
    `uuid` varchar(32) NOT NULL,
    `controllerUuid` varchar(32) NOT NULL,
    `token` varchar(64) NOT NULL,
    `status` varchar(32) NOT NULL,
    `expectedVersion` varchar(32) DEFAULT NULL,
    `actualVersion` varchar(32) DEFAULT NULL,
    `source` varchar(32) NOT NULL,
    `differenceDetails` text DEFAULT NULL,
    `impact` text DEFAULT NULL,
    `fallbackSolution` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukControllerToken` (`controllerUuid`, `token`),
    INDEX `idxControllerUuid` (`controllerUuid`),
    CONSTRAINT `fkZnsControllerApiCompatibilityVOZnsInfoVO`
        FOREIGN KEY (`controllerUuid`) REFERENCES `zstack`.`ZnsInfoVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsTransportZoneRefVO (
    vSwitchUuid char(32) NOT NULL,
    znsInfoUuid char(32) NOT NULL,
    znsTransportZoneUuid char(32) NOT NULL,
    name varchar(255),
    description varchar(2048),
    transportType varchar(32),
    physicalNetwork varchar(64),
    status varchar(32),
    defaultZone bit NOT NULL DEFAULT 0,
    lastSyncDate timestamp NULL,
    PRIMARY KEY (vSwitchUuid),
    UNIQUE KEY `ukZnsTransportZoneRefVOIdentity` (znsInfoUuid, znsTransportZoneUuid),
    INDEX `idxZnsTransportZoneRefVOZnsInfoUuid` (`znsInfoUuid`),
    CONSTRAINT fkZnsTransportZoneRefVOZnsInfoVO FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsSegmentRefVO (
    l2PortGroupUuid char(32) NOT NULL,
    znsInfoUuid char(32) NOT NULL,
    znsSegmentUuid char(32) NOT NULL,
    znsTransportZoneUuid char(32),
    tenantRouterUuid char(32) DEFAULT NULL,
    transportType varchar(32),
    virtualNetworkId int,
    configVersion bigint,
    state varchar(32),
    lastOperation varchar(32),
    lastError varchar(2048),
    lastSyncDate timestamp NULL,
    PRIMARY KEY (l2PortGroupUuid),
    UNIQUE KEY `ukZnsSegmentRefVOIdentity` (znsInfoUuid, znsSegmentUuid),
    INDEX `idxZnsSegmentRefVOZnsInfoUuid` (`znsInfoUuid`),
    CONSTRAINT fkZnsSegmentRefVOZnsInfoVO FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsSegmentPortRefVO (
    vmNicUuid char(32) NOT NULL,
    znsInfoUuid char(32) NOT NULL,
    znsSegmentPortUuid char(32) NOT NULL,
    znsSegmentUuid char(32),
    configVersion bigint,
    state varchar(32),
    lastOperation varchar(32),
    lastError varchar(2048),
    lastSyncDate timestamp NULL,
    PRIMARY KEY (vmNicUuid),
    UNIQUE KEY `ukZnsSegmentPortRefVOIdentity` (znsInfoUuid, znsSegmentPortUuid),
    INDEX `idxZnsSegmentPortRefVOZnsInfoUuid` (`znsInfoUuid`),
    CONSTRAINT fkZnsSegmentPortRefVOZnsInfoVO FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsTransportZoneHostRefVO (
    uuid char(32) NOT NULL,
    znsInfoUuid char(32) NOT NULL,
    hostUuid char(32) NOT NULL,
    znsTransportZoneUuid char(32) NOT NULL,
    datapathType varchar(32),
    available bit NOT NULL DEFAULT 0,
    lastSeenDate timestamp NULL,
    PRIMARY KEY (uuid),
    UNIQUE KEY `ukZnsTransportZoneHostRefVOIdentity` (znsInfoUuid, hostUuid, znsTransportZoneUuid),
    INDEX `idxZnsTransportZoneHostRefVOZnsInfoUuid` (`znsInfoUuid`),
    CONSTRAINT fkZnsTransportZoneHostRefVOZnsInfoVO FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ZnsTenantRouterVO` (
    `uuid` char(32) NOT NULL,
    `znsInfoUuid` char(32) NOT NULL,
    `znsResourceUuid` char(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `createDate` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukZnsTenantRouterVOIdentity` (`znsInfoUuid`, `znsResourceUuid`),
    CONSTRAINT `fkZnsTenantRouterVOResourceVO` FOREIGN KEY (`uuid`)
        REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkZnsTenantRouterVOZnsInfoVO` FOREIGN KEY (`znsInfoUuid`)
        REFERENCES `ZnsInfoVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
