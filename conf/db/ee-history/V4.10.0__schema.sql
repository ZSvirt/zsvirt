CALL ADD_COLUMN('SNSApplicationEndpointVO', 'connectionStatus', 'varchar(10)', 1, 'UP');

-- Improvement: VM Cdrom Occupant | ZSV-6691

CALL INSERT_COLUMN('VmCdRomVO', 'occupant', 'varchar(64)', 1, null, 'deviceId');
update `VmCdRomVO` set `occupant` = 'ISO' where `isoUuid` is not null;
update `VmCdRomVO` set `occupant` = 'GuestTools'
    where `vmInstanceUuid` in ( select `resourceUuid` from `SystemTagVO` where `tag` = 'guestToolsHasAttached' )
    and `deviceId` = 0;

-- Feature: IAM1 Role And Policy | ZSV-6559

drop table if exists `IAM2TicketFlowCollectionVO`;
drop table if exists `IAM2TicketFlowVO`;

drop table if exists `IAM2ProjectVirtualIDGroupRefVO`;
drop table if exists `IAM2ProjectVirtualIDRefVO`;
drop table if exists `IAM2OrganizationProjectRefVO`;
drop table if exists `IAM2GroupVirtualIDRefVO`;
drop table if exists `IAM2VirtualIDRoleRefVO`;
drop table if exists `IAM2VirtualIDOrganizationRefVO`;
drop table if exists `IAM2VirtualIDGroupRoleRefVO`;
drop table if exists `IAM2VirtualIDGroupRefVO`;
drop table if exists `IAM2ProjectAccountRefVO`;
drop table if exists `IAM2ProjectResourceRefVO`;

drop table if exists `IAM2OrganizationAttributeVO`;
drop table if exists `IAM2VirtualIDGroupAttributeVO`;
drop table if exists `IAM2ProjectAttributeVO`;
drop table if exists `IAM2VirtualIDAttributeVO`;
drop table if exists `IAM2ProjectRoleVO`;
drop table if exists `IAM2OrganizationVO`;
drop table if exists `IAM2VirtualIDGroupVO`;
drop table if exists `IAM2ProjectTemplateVO`;
drop table if exists `IAM2ProjectVO`;
drop table if exists `IAM2VirtualIDVO`;

delete from `AccountResourceRefVO` where `accountUuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `QuotaVO` where `identityUuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `AccountVO` where `uuid` = '2dce5dc485554d21a3796500c1db007a';
delete from `ResourceVO` where `uuid` = '2dce5dc485554d21a3796500c1db007a';

ALTER TABLE `TwoFactorAuthenticationSecretVO` CHANGE COLUMN `userUuid` `accountUuid` char(32) not null;
call DROP_COLUMN('TwoFactorAuthenticationSecretVO', 'userType');

RENAME TABLE `CCSCertificateUserRefVO` TO `CCSCertificateAccountRefVO`;
ALTER TABLE `CCSCertificateAccountRefVO` CHANGE COLUMN `userUuid` `accountUuid` char(32) not null;

call DROP_COLUMN('AccessKeyVO', 'userUuid');

drop table if exists `UserPolicyRefVO`;
drop table if exists `UserGroupPolicyRefVO`;
drop table if exists `UserGroupUserRefVO`;
drop table if exists `RoleUserGroupRefVO`;
drop table if exists `RoleUserRefVO`;

drop table if exists `UserGroupVO`;
delete from `ResourceVO` where `resourceType` = 'UserGroupVO';

alter table `HybridAccountVO` drop foreign key `fkHybridAccountVOUserVO`;
drop table if exists `UserVO`;
delete from `ResourceVO` where `resourceType` = 'UserVO';

rename table `AccountResourceRefVO` to `AccountResourceRefVODeprecated`;

create table if not exists `zstack`.`AccountResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `accountUuid` char(32) default null,
    `resourceUuid` varchar(32) not null,
    `resourceType` varchar(255) not null,
    `accountPermissionFrom` char(32) default null,
    `resourcePermissionFrom` char(32) default null,
    `type` varchar(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountResourceRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade,
    constraint `fkAccountResourceRefResourceUuid` foreign key (`resourceUuid`) references `ResourceVO` (`uuid`) on delete cascade,
    index `idxAccountResourceRefResourceTypeAccount` (`resourceUuid`, `type`, `accountUuid`)
) ENGINE=InnoDB default CHARSET=utf8;

insert into `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.accountUuid, t.resourceUuid, t.resourceType, 'Own', t.lastOpDate, t.createDate
        from AccountResourceRefVODeprecated t;
insert into `AccountResourceRefVO`
    (`accountUuid`,`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.receiverAccountUuid, t.resourceUuid, t.resourceType, 'Share', t.lastOpDate, t.createDate
        from SharedResourceVO t
        where t.toPublic = 0;
insert into `AccountResourceRefVO`
    (`resourceUuid`,`resourceType`,`type`,`lastOpDate`,`createDate`)
    select t.resourceUuid, t.resourceType, 'SharePublic', t.lastOpDate, t.createDate
        from SharedResourceVO t
        where t.toPublic = 1;

drop table if exists `AccountResourceRefVODeprecated`;
drop table if exists `SystemRoleVO`;
drop table if exists `RolePolicyRefVO`;
drop table if exists `PolicyVO`;
drop table if exists `RolePolicyStatementVO`;
drop table if exists `RoleAccountRefVO`;
delete from `RoleVO`;
delete from `ResourceVO` where resourceType in ('SystemRoleVO', 'RoleVO', 'PolicyVO');

call DROP_COLUMN('RoleVO', 'identity');
call DROP_COLUMN('RoleVO', 'state');

create table if not exists `zstack`.`RolePolicyVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `actions` varchar(255) not null,
    `effect` varchar(32) not null,
    `resourceType` varchar(255) default null,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkRolePolicyRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    index `idxRolePolicyActions` (`actions`)
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`RolePolicyResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `rolePolicyId` bigint unsigned not null,
    `effect` varchar(32) default 'Allow' not null,
    `resourceUuid` char(32) not null,
    primary key (`id`),
    constraint `fkRolePolicyResourceRefRolePolicyId` foreign key (`rolePolicyId`) references `RolePolicyVO` (`id`) on delete cascade
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`RoleAccountRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `accountUuid` char(32) not null,
    `accountPermissionFrom` char(32) default null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkRoleAccountRefRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    constraint `fkRoleAccountRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupVO` (
    `uuid` char(32) not null unique,
    `name` varchar(255) not null,
    `description` varchar(2048) default '',
    `parentUuid` char(32) default null,
    `rootGroupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`uuid`)
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupAccountRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `accountUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupAccountRefAccountUuid` foreign key (`accountUuid`) references `AccountVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupAccountRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupRoleRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `roleUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupRoleRefRoleUuid` foreign key (`roleUuid`) references `RoleVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupRoleRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB default CHARSET=utf8;

create table if not exists `zstack`.`AccountGroupResourceRefVO` (
    `id` bigint unsigned not null unique AUTO_INCREMENT,
    `resourceUuid` char(32) not null,
    `groupUuid` char(32) not null,
    `lastOpDate` timestamp on update CURRENT_TIMESTAMP,
    `createDate` timestamp,
    primary key (`id`),
    constraint `fkAccountGroupResourceRefResourceUuid` foreign key (`resourceUuid`) references `ResourceVO` (`uuid`) on delete cascade,
    constraint `fkAccountGroupResourceRefGroupUuid` foreign key (`groupUuid`) references `AccountGroupVO` (`uuid`) on delete cascade
) ENGINE=InnoDB default CHARSET=utf8;

alter table `zstack`.`AccountResourceRefVO` add constraint fkAccountResourceRefAccountPermissionFrom foreign key (accountPermissionFrom) references AccountGroupVO (uuid) on delete cascade;
alter table `zstack`.`RoleAccountRefVO` add constraint fkRoleAccountRefAccountPermissionFrom foreign key (accountPermissionFrom) references AccountGroupVO (uuid) on delete cascade;
delete from `zstack`.`AccountResourceRefVO` where resourceType in ('HostNetworkInterfaceVO', 'HostNetworkBondingVO');

-- Others

CALL INSERT_COLUMN('ClusterDRSVO', 'lastAdviceGroupUuid', 'char(32)', 1, null, 'balancedState');
update `ClusterDRSVO` set `state` = 'Enabled' where `clusterUuid` in
    (select `resourceUuid` from `ResourceConfigVO` where `name` = 'drs.enable' and `resourceType` = 'ClusterVO' and `value` = 'true');
update `ClusterDRSVO` set `state` = 'Disabled' where `clusterUuid` in
    (select `resourceUuid` from `ResourceConfigVO` where `name` = 'drs.enable' and `resourceType` = 'ClusterVO' and `value` = 'false');
delete from `ResourceConfigVO` where `name` = 'drs.enable' and `resourceType` = 'ClusterVO';
delete from `GlobalConfigVO` where `name` = 'drs.enable';
delete from `SystemTagVO` where `resourceUuid` in (select `uuid` from `HostCapacityVO` where `cpuSockets` != 1) and `tag` like "cpuProcessorNum::%";

CALL INSERT_COLUMN('VmVfNicVO', 'haState', 'varchar(32)', 0, 'Disabled', 'pciDeviceUuid');

CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` bigint unsigned NULL DEFAULT 0,
    `power` bigint unsigned NULL DEFAULT 0,
    `isDriverLoaded` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'passThroughState', 'VARCHAR(32)', 0, 'Disabled');
UPDATE `PciDeviceVO` set `passThroughState` = 'Available' where type in ('Audio_Controller', 'USB_Controller', 'Serial_Controller', 'Moxa_Device', 'Generic', 'Custom');
UPDATE `PciDeviceVO` set `passThroughState` = 'Enabled' where type in ('GPU_Video_Controller', 'GPU_Audio_Controller', 'GPU_USB_Controller', 'GPU_Serial_Controller', 'GPU_3D_Controller');

CALL ADD_COLUMN('PciDeviceSpecVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);

DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
DELIMITER $$
CREATE PROCEDURE MdevDeviceAddVendor()
    BEGIN
        DECLARE vendor VARCHAR(128);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendor FROM PciDeviceVO pci;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vendor, pciDeviceUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE MdevDeviceVO SET vendor = vendor WHERE parentUuid = pciDeviceUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call MdevDeviceAddVendor;
DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;

CREATE TABLE IF NOT EXISTS `HostHwMonitorStatusVO`
(
    `uuid` varchar(32)  NOT NULL UNIQUE,
    `cpuStatus` varchar(32) NOT NULL,
    `memoryStatus` varchar(32) NOT NULL,
    `diskStatus` varchar(32) NOT NULL,
    `nicStatus` varchar(32) NOT NULL,
    `gpuStatus` varchar(32) NOT NULL,
    `powerSupplyStatus` varchar(32) NOT NULL,
    `fanStatus` varchar(32) NOT NULL,
    `raidStatus` varchar(32) NOT NULL,
    `temperatureStatus` varchar(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostHwMonitorStatusVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP PROCEDURE IF EXISTS `CreateGpuDeviceVO`;
DELIMITER $$
CREATE PROCEDURE CreateGpuDeviceVO()
    BEGIN
        DECLARE uuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid FROM PciDeviceVO pci where pci.type in ('GPU_Video_Controller', 'GPU_3D_Controller');
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO uuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            insert into GpuDeviceVO (uuid) values (uuid);
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call CreateGpuDeviceVO;
DROP PROCEDURE IF EXISTS `CreateGpuDeviceVO`;

DROP PROCEDURE IF EXISTS `addPciDeviceVendor`;
DELIMITER $$
CREATE PROCEDURE addPciDeviceVendor()
    BEGIN
        DECLARE pciUuid VARCHAR(32);
        DECLARE vendorId VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendorId FROM PciDeviceVO pci where pci.type in ('GPU_Video_Controller', 'GPU_3D_Controller');
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO pciUuid, vendorId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF vendorId = '1d94' then
                update PciDeviceVO set vendor = 'Haiguang' where uuid = pciUuid;
            ELSEIF vendorId = '10de' then
                update PciDeviceVO set vendor = 'NVIDIA' where uuid = pciUuid;
            ELSEIF vendorId = '1002' then
                update PciDeviceVO set vendor = 'AMD' where uuid = pciUuid;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call addPciDeviceVendor;
DROP PROCEDURE IF EXISTS `addPciDeviceVendor`;

CREATE TABLE IF NOT EXISTS `zstack`.`HostPhysicalCpuVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `socketDesignation` varchar(255) DEFAULT NULL,
    `version` varchar(255) DEFAULT NULL,
    `serialNumber` varchar(255) NOT NULL,
    `currentSpeed` varchar(32) DEFAULT NULL,
    `coreCount` varchar(32) DEFAULT NULL,
    `threadCount` varchar(32) DEFAULT NULL,
    `hostUuid` char(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostPhysicalCpuVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
