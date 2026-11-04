-- This is SQL schema for zsv 4.1.0 (based on cloud 4.8.0)

UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Detached' WHERE `attachStatus` = '0' or `attachStatus` = '1' or `attachStatus` = '2';
UPDATE `zstack`.`L2NetworkHostRefVO` set `attachStatus` = 'Attached' WHERE `attachStatus` = '3';

ALTER TABLE ConsoleProxyVO ADD COLUMN `expiredDate` timestamp NOT NULL;
UPDATE ImageEO SET md5sum = NULL where md5sum != 'not calculated';

ALTER TABLE `zstack`.`KVMHostVO` ADD COLUMN `iscsiInitiatorName` varchar(256) DEFAULT NULL;

delete from `SystemTagVO` where resourceUuid ='5d3bb9d271a349b283893317f531f723';
delete from `SystemTagVO` where resourceUuid ='5z6gsgkc5kccpylj9ocgbd647p2700b7';
delete from `SystemTagVO` where resourceUuid ='66dfdee6fd314aac96ca3779774ad977';
delete from `SystemTagVO` where resourceUuid ='ded02f9786444c6296e9bc3efb8eb484';
delete from `SystemTagVO` where resourceUuid ='fuz2p4fa71urf4fd7cknoxsalvj60ynk';
delete from `SystemTagVO` where resourceUuid ='ue0x30t7wfyuba87nwk6ywu3ub5svtwk';
delete from `SystemTagVO` where resourceUuid ='d0b35ac37c58e358cb74e664532f1044';
delete from `SystemTagVO` where resourceUuid ='uhgfoh0soh6e1qai005elfa9c6h2s2y0';
delete from `SystemTagVO` where resourceUuid ='829d96de006043c3b34202861ca82078';

delete from `AlarmVO` where uuid = '369eef54655548eab2a4d2d7ef061c79';
delete from `AlarmVO` where uuid = 'ue0x30t7wfyuba87nwk6ywu3ub5svtwk';
delete from `AlarmVO` where uuid = 'f3389a28b7d64e35875992d254ff4f96';

delete from `EventSubscriptionVO` where uuid = 'bd0163e7028644a5b482534c2711d2d9';
delete from `EventSubscriptionVO` where uuid = '79b0dad6607a429cb235ad2f701718a0';
delete from `EventSubscriptionVO` where uuid = 'eef29da3aff8486093d6afabb05cddbf';
delete from `EventSubscriptionVO` where uuid = '842e20d7d9844ee3a3c2a4224235a7df';


delete from `ResourceVO` where uuid = '369eef54655548eab2a4d2d7ef061c79';
delete from `ResourceVO` where uuid = 'ue0x30t7wfyuba87nwk6ywu3ub5svtwk';
delete from `ResourceVO` where uuid = 'f3389a28b7d64e35875992d254ff4f96';

delete from `ResourceVO` where uuid = 'bd0163e7028644a5b482534c2711d2d9';
delete from `ResourceVO` where uuid = '79b0dad6607a429cb235ad2f701718a0';
delete from `ResourceVO` where uuid = 'eef29da3aff8486093d6afabb05cddbf';
delete from `ResourceVO` where uuid = '842e20d7d9844ee3a3c2a4224235a7df';


delete from `AccountResourceRefVO` where resourceUuid = '369eef54655548eab2a4d2d7ef061c79';
delete from `AccountResourceRefVO` where resourceUuid = 'ue0x30t7wfyuba87nwk6ywu3ub5svtwk';
delete from `AccountResourceRefVO` where resourceUuid = 'f3389a28b7d64e35875992d254ff4f96';

delete from `AccountResourceRefVO` where resourceUuid = 'bd0163e7028644a5b482534c2711d2d9';
delete from `AccountResourceRefVO` where resourceUuid = '79b0dad6607a429cb235ad2f701718a0';
delete from `AccountResourceRefVO` where resourceUuid = 'eef29da3aff8486093d6afabb05cddbf';
delete from `AccountResourceRefVO` where resourceUuid = '842e20d7d9844ee3a3c2a4224235a7df';

delete from `EventSubscriptionVO` where uuid = '33de14ed204948daa850f9b9a3a02e89';
delete from `EventSubscriptionVO` where uuid = '39d2b6689efa4e4a96c239716cb6f3ea';
delete from `EventSubscriptionVO` where uuid = 'a3d9fd893fbb4468867a7880b6b91ba6';
delete from `EventSubscriptionVO` where uuid = 'd59397479d2548d7abfe4ad31a575390';

delete from `ResourceVO` where uuid = '33de14ed204948daa850f9b9a3a02e89';
delete from `ResourceVO` where uuid = '39d2b6689efa4e4a96c239716cb6f3ea';
delete from `ResourceVO` where uuid = 'a3d9fd893fbb4468867a7880b6b91ba6';
delete from `ResourceVO` where uuid = 'd59397479d2548d7abfe4ad31a575390';


delete from `AccountResourceRefVO` where resourceUuid = '33de14ed204948daa850f9b9a3a02e89';
delete from `AccountResourceRefVO` where resourceUuid = '39d2b6689efa4e4a96c239716cb6f3ea';
delete from `AccountResourceRefVO` where resourceUuid = 'a3d9fd893fbb4468867a7880b6b91ba6';
delete from `AccountResourceRefVO` where resourceUuid = 'd59397479d2548d7abfe4ad31a575390';

delete from `AlarmVO` where uuid = '582ea79cb57d45a8bfd4d2030244c1c4';
delete from `ResourceVO` where uuid = '582ea79cb57d45a8bfd4d2030244c1c4';
delete from `AccountResourceRefVO` where resourceUuid = '582ea79cb57d45a8bfd4d2030244c1c4';

delete from `SystemTagVO` where resourceUuid = '4a3cb114b10d41e19545ab693222c134' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '1a7a3eb433904df89f5c42a1fa4e0716' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '7aef5229f2bb4f8ca8f2db678a148619' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'd1d122f95c194c958ba1be4a3568ebd0' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '9a593ad138bf44138b72e0f0dd989f27' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '6nz3vn2e0rdwu5hzmuetzv37ak0nj248' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'rlwalvvqyoujj3ign3o309p2zulwbhwm' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'krdu1hs2314kt18ttgqndaynxchs2ufc' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '8tlwqj65mus1gdolu3w61yy35pvwinhz' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'g0eviogong06nubt1kj54z63pcka81sw' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'eccfc93109cd4c71b56a2612d84a2773' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '559ca06aa8bba6990d10c255e4c9ab5b' and tag like 'name::cn::%';

delete from `SystemTagVO` where resourceUuid = '5e75230bd2ea4f47abf6ff92fa816a20' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'f56795b8c34b452f84bcf25cb89bded2' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '8tlwqj65mus1gdolu3w61yy35pvwinhz' and tag like 'name::cn::%';

delete from `SystemTagVO` where resourceUuid = '44e6f054a59a451fb1b535accff64fc2' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '98f9c802604e4852bd84716f66cf4f73' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '55365763fed244c39b4642bef6c5daf9' and tag like 'name::cn::%';

delete from `SystemTagVO` where resourceUuid = 'a391bb01fd954ed3b6c0569ecc7b5764' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '6nz3vn2e0rdwu5hzmuetzv37ak0nj248' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'ppfazo1y3tjvup4jfetxz36y3su98ngc' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'rlwalvvqyoujj3ign3o309p2zulwbhwm' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'krdu1hs2314kt18ttgqndaynxchs2ufc' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = 'eccfc93109cd4c71b56a2612d84a2773' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '98536fa94e3f4481a38331a989132b7c' and tag like 'name::cn::%';
delete from `SystemTagVO` where resourceUuid = '4a3494bcdbac4eaab9e9e56e27d74a2a' and tag like 'name::cn::%';

delete from `SystemTagVO` where resourceUuid = '4a3494bcdbac4eaab9e9e56e27d74a2a' and tag like 'name::cn::%';

-- moved from V4.8.0.2__schema.sql

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL2NetworkVO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES L2NetworkEO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES L3NetworkEO (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceUsedIpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostKernelInterfaceUsedIpVOUsedIpVO` FOREIGN KEY (`uuid`) REFERENCES UsedIpVO (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHostKernelInterfaceUsedIpVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostKernelInterfaceTrafficTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostKernelInterfaceUuid` varchar(32) NOT NULL,
    `trafficType` varchar(128) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkHostKernelInterfaceTrafficTypeVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES HostKernelInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- moved from V4.8.0.3__schema.sql

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceLldpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `interfaceUuid` varchar(32) NOT NULL UNIQUE,
    `mode` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkHostNetworkInterfaceLldpVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES HostNetworkInterfaceVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HostNetworkInterfaceLldpRefVO` (
    `lldpUuid` varchar(32) NOT NULL,
    `chassisId` varchar(32) NOT NULL,
    `timeToLive` int(32) NOT NULL,
    `managementAddress` varchar(32) DEFAULT NULL,
    `systemName` varchar(32) NOT NULL,
    `systemDescription` varchar(255) NOT NULL,
    `systemCapabilities` varchar(32) NOT NULL,
    `portId` varchar(32) NOT NULL,
    `portDescription` varchar(255) DEFAULT NULL,
    `vlanId` int(32) DEFAULT NULL,
    `aggregationPortId` bigint unsigned DEFAULT NULL,
    `mtu` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`lldpUuid`),
    CONSTRAINT `fkHostNetworkInterfaceLldpRefVOHostNetworkInterfaceLldpVO` FOREIGN KEY (`lldpUuid`) REFERENCES HostNetworkInterfaceLldpVO (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `zstack`.`HostNetworkInterfaceLldpVO` (`uuid`, `interfaceUuid`, `mode`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'rx_only', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM `zstack`.`HostNetworkInterfaceVO` t;
