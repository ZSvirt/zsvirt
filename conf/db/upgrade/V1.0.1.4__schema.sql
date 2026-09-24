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

CREATE TABLE IF NOT EXISTS ZnsInfoVO (
    uuid varchar(32) NOT NULL, computerManagerUuid varchar(64) NOT NULL,
    name varchar(255), description varchar(2048), ip varchar(255), username varchar(255), password text,
    vendorType varchar(255), vendorVersion varchar(255), accessKeyId varchar(64),
    accessKeySecret text, status varchar(32), lastSyncDate timestamp NULL,
    createDate timestamp NULL, lastOpDate timestamp NULL,
    PRIMARY KEY (uuid), UNIQUE KEY uk_zns_info_cm_uuid (computerManagerUuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsTransportZoneRefVO (
    vSwitchUuid varchar(32) NOT NULL, znsInfoUuid varchar(32) NOT NULL,
    znsTransportZoneUuid varchar(64) NOT NULL,
    name varchar(255), description varchar(2048), transportType varchar(32), physicalNetwork varchar(64),
    status varchar(32), defaultZone bit NOT NULL DEFAULT 0, lastSyncDate timestamp NULL,
    PRIMARY KEY (vSwitchUuid), UNIQUE KEY uk_zns_tz_uuid (znsInfoUuid, znsTransportZoneUuid),
    KEY idx_zns_tz_info (znsInfoUuid),
    CONSTRAINT fk_zns_tz_info FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsSegmentRefVO (
    l2PortGroupUuid varchar(32) NOT NULL, znsInfoUuid varchar(32) NOT NULL, znsSegmentUuid varchar(64) NOT NULL,
    znsTransportZoneUuid varchar(64), transportType varchar(32), virtualNetworkId int,
    configVersion bigint, state varchar(32), lastOperation varchar(32), lastError varchar(2048), lastSyncDate timestamp NULL,
    PRIMARY KEY (l2PortGroupUuid), UNIQUE KEY uk_zns_segment_uuid (znsInfoUuid, znsSegmentUuid),
    KEY idx_zns_segment_info (znsInfoUuid),
    CONSTRAINT fk_zns_segment_info FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsSegmentPortRefVO (
    vmNicUuid varchar(32) NOT NULL, znsInfoUuid varchar(32) NOT NULL,
    znsSegmentPortUuid varchar(64) NOT NULL, znsSegmentUuid varchar(64),
    configVersion bigint, state varchar(32), lastOperation varchar(32), lastError varchar(2048), lastSyncDate timestamp NULL,
    PRIMARY KEY (vmNicUuid), UNIQUE KEY uk_zns_segment_port_uuid (znsInfoUuid, znsSegmentPortUuid),
    KEY idx_zns_segment_port_info (znsInfoUuid),
    CONSTRAINT fk_zns_segment_port_info FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS ZnsTransportZoneHostRefVO (
    uuid varchar(32) NOT NULL, znsInfoUuid varchar(32) NOT NULL,
    hostUuid varchar(32) NOT NULL, vSwitchUuid varchar(32) NOT NULL,
    znsTransportZoneUuid varchar(64) NOT NULL, datapathType varchar(32), available bit NOT NULL DEFAULT 0,
    lastSeenDate timestamp NULL, PRIMARY KEY (uuid),
    UNIQUE KEY uk_zns_host_tz (znsInfoUuid, hostUuid, znsTransportZoneUuid),
    KEY idx_zns_host_tz_info (znsInfoUuid),
    CONSTRAINT fk_zns_host_tz_info FOREIGN KEY (znsInfoUuid) REFERENCES ZnsInfoVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `ZnsTenantRouterVO` (
    `uuid` varchar(32) NOT NULL,
    `znsInfoUuid` varchar(32) NOT NULL,
    `znsResourceUuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `createDate` timestamp NULL DEFAULT NULL,
    `lastOpDate` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uk_zns_tenant_router_identity` (`znsInfoUuid`, `znsResourceUuid`),
    CONSTRAINT `fk_zns_tenant_router_resource` FOREIGN KEY (`uuid`)
        REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fk_zns_tenant_router_info` FOREIGN KEY (`znsInfoUuid`)
        REFERENCES `ZnsInfoVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
