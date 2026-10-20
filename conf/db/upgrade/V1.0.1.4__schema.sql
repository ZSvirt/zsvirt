-- Feature: PCI/MDEV/USB definition move to headers module

UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.PciDeviceVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.PciDeviceVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.PciDeviceOfferingVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.PciDeviceOfferingVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.gpu.GpuDeviceVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.gpu.GpuDeviceVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.specification.pci.PciDeviceSpecVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceVO'
                  WHERE concreteResourceType = 'org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO';
UPDATE ResourceVO SET concreteResourceType = 'org.zstack.usbDevice.entity.UsbDeviceVO'
                  WHERE concreteResourceType = 'org.zstack.usbDevice.UsbDeviceVO';

-- Feature: VmHaVO replaces vmFencedBy system tag | ZSV-13383

CALL INSERT_COLUMN('VmHaVO', 'lastFencerName', 'varchar(64)', 1, NULL, 'inhibitionTime');
CALL INSERT_COLUMN('VmHaVO', 'lastFenceHostUuid', 'varchar(32)', 1, NULL, 'lastFencerName');
CALL INSERT_COLUMN('VmHaVO', 'lastFenceTime', 'timestamp NULL', 1, NULL, 'lastFenceHostUuid');

UPDATE `zstack`.`VmHaVO` v
  INNER JOIN `zstack`.`SystemTagVO` t
    ON t.resourceUuid = v.uuid AND t.tag LIKE 'vmFencedBy::%'
SET v.lastFencerName = SUBSTRING(t.tag, LENGTH('vmFencedBy::') + 1),
    v.lastFenceTime = t.createDate;

DELETE FROM `zstack`.`SystemTagVO` WHERE `tag` LIKE 'vmFencedBy::%';

UPDATE GarbageCollectorVO SET runnerClass = 'org.zstack.ha.gc.NeverStopVmGC'
    WHERE runnerClass = 'org.zstack.ha.NeverStopVmGC';

DROP TABLE IF EXISTS `zstack`.`FaultToleranceVmInstanceGroupHostPortRefVO`;
DROP TABLE IF EXISTS `zstack`.`FaultToleranceVmGroupVO`;

DROP TABLE IF EXISTS `zstack`.`HostHaStateVO`;
CREATE TABLE `zstack`.`HostHaStateVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` char(32) NOT NULL,
    `primaryStorageUuid` char(32) NOT NULL,
    `state` varchar(32),
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkHostHaStateVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostHaStateVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

CREATE TABLE IF NOT EXISTS ZnsCapabilitySnapshotVO (
    znsUuid char(32) NOT NULL,
    valid bit NOT NULL DEFAULT 0,
    productVersion varchar(64) DEFAULT NULL,
    connectionGeneration bigint NOT NULL DEFAULT 0,
    probeEpoch bigint NOT NULL DEFAULT 0,
    capabilitiesJson text DEFAULT NULL,
    lastAttemptDate timestamp NULL DEFAULT NULL,
    lastSuccessDate timestamp NULL DEFAULT NULL,
    lastErrorCode varchar(64) DEFAULT NULL,
    PRIMARY KEY (znsUuid),
    CONSTRAINT fkZnsCapabilitySnapshotVOZnsInfoVO FOREIGN KEY (znsUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE CASCADE
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
