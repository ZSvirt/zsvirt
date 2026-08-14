USE `zstack`;

CREATE TABLE `AccessControlListEntryVO` (
  `uuid` varchar(32) NOT NULL,
  `aclUuid` varchar(32) NOT NULL,
  `ipEntries` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(32) NOT NULL DEFAULT 'IpEntry',
  `name` varchar(32) DEFAULT NULL,
  `matchMethod` varchar(32) DEFAULT NULL,
  `criterion` varchar(32) DEFAULT NULL,
  `domain` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `redirectRule` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkACLRuleVOAccessControlListVO` (`aclUuid`),
  CONSTRAINT `fkACLRuleVOAccessControlListVO` FOREIGN KEY (`aclUuid`) REFERENCES `AccessControlListVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccessControlListVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `ipVersion` int(10) unsigned DEFAULT '4',
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccessControlRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `strategy` varchar(64) NOT NULL,
  `rule` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccessKeyVO` (
  `uuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `AccessKeyID` varchar(128) NOT NULL,
  `AccessKeySecret` varchar(128) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountGroupAccountRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `accountUuid` char(32) NOT NULL,
  `groupUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAccountGroupAccountRefAccountUuid` (`accountUuid`),
  KEY `fkAccountGroupAccountRefGroupUuid` (`groupUuid`),
  CONSTRAINT `fkAccountGroupAccountRefAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountGroupAccountRefGroupUuid` FOREIGN KEY (`groupUuid`) REFERENCES `AccountGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountGroupResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` char(32) NOT NULL,
  `groupUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAccountGroupResourceRefResourceUuid` (`resourceUuid`),
  KEY `fkAccountGroupResourceRefGroupUuid` (`groupUuid`),
  CONSTRAINT `fkAccountGroupResourceRefResourceUuid` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountGroupResourceRefGroupUuid` FOREIGN KEY (`groupUuid`) REFERENCES `AccountGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountGroupRoleRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `roleUuid` char(32) NOT NULL,
  `groupUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAccountGroupRoleRefRoleUuid` (`roleUuid`),
  KEY `fkAccountGroupRoleRefGroupUuid` (`groupUuid`),
  CONSTRAINT `fkAccountGroupRoleRefRoleUuid` FOREIGN KEY (`roleUuid`) REFERENCES `RoleVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountGroupRoleRefGroupUuid` FOREIGN KEY (`groupUuid`) REFERENCES `AccountGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountGroupVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT '',
  `parentUuid` char(32) DEFAULT NULL,
  `rootGroupUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountPriceTableRefVO` (
  `tableUuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`tableUuid`,`accountUuid`),
  UNIQUE KEY `accountUuid` (`accountUuid`),
  CONSTRAINT `fkAccountPriceTableRefVOPriceTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PriceTableVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountPriceTableRefVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `accountUuid` char(32) DEFAULT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `accountPermissionFrom` char(32) DEFAULT NULL,
  `resourcePermissionFrom` char(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAccountResourceRefAccountUuid` (`accountUuid`),
  KEY `idxAccountResourceRefResourceTypeAccount` (`resourceUuid`,`type`,`accountUuid`),
  KEY `fkAccountResourceRefAccountPermissionFrom` (`accountPermissionFrom`),
  CONSTRAINT `fkAccountResourceRefAccountPermissionFrom` FOREIGN KEY (`accountPermissionFrom`) REFERENCES `AccountGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountResourceRefAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountResourceRefResourceUuid` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountThirdPartyAccountSourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `credentials` varchar(255) NOT NULL,
  `accountSourceUuid` char(32) NOT NULL,
  `accountUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `credentialsAccountSourceUuid` (`credentials`,`accountSourceUuid`) USING BTREE,
  KEY `fkAccountSourceRefVOThirdPartyAccountSourceVO` (`accountSourceUuid`),
  KEY `fkAccountSourceRefVOAccountVO` (`accountUuid`),
  CONSTRAINT `fkAccountSourceRefVOThirdPartyAccountSourceVO` FOREIGN KEY (`accountSourceUuid`) REFERENCES `ThirdPartyAccountSourceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAccountSourceRefVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AccountVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'account uuid',
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL COMMENT 'password',
  `type` varchar(128) NOT NULL COMMENT 'account type',
  `state` varchar(128) NOT NULL DEFAULT 'Enabled',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `description` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `name` (`name`),
  KEY `idxAccountVOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ActiveAlarmTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `alarmName` varchar(255) NOT NULL,
  `comparisonOperator` varchar(128) NOT NULL,
  `period` int(10) unsigned NOT NULL,
  `repeatInterval` int(10) unsigned NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `metricName` varchar(512) NOT NULL,
  `threshold` double NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `repeatCount` int(11) DEFAULT NULL,
  `enableRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `ActiveAlarmTemplateVO` VALUE
('180dcd21d9c64e1190ac09c825023a3f','Host-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/Host','MemoryUsedInPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('1c43bab11c9b454281827a0af3ccb02e','Host-DiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/Host','DiskAllUsedCapacityInPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('231b35bf21d5406d992286ba4c0bf749','Host-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/Host','CPUAverageUsedUtilization',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('383d9dcd547d46c9ac5f1031905a9b54','VM-DiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','DiskAllUsedCapacityInPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('64ff18b8628443d58dbf66c9bbad37e6','VRouter-VRouterDiskAllUsedCapacityInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterDiskAllUsedCapacityInPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('65c8af4f0a2342e8a5a79511b546f750','VRouter-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterMemoryUsedPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('69d2840e61fa49d280948ce8f7112e46','VM-OperatingSystemMemoryUsedPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','OperatingSystemMemoryUsedPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('94fcd41cac524a57b47452a78d14cfab','VM-MemoryUsedInPercent','GreaterThanOrEqualTo',300,1800,'ZStack/VM','MemoryUsedInPercent',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('c9e6cdca107140bea62b4ca919ff9e88','VRouter-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VRouter','VRouterCPUAverageUsedUtilization',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('ccc249938ad34e7f92d6a1cc7e123b38','VM-CPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VM','CPUAverageUsedUtilization',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL),
('fa6ead4d89064002b1b96ed2abf6ecb5','VM-OperatingSystemCPUAverageUsedUtilization','GreaterThanOrEqualTo',300,1800,'ZStack/VM','OperatingSystemCPUAverageUsedUtilization',80,'2026-07-30 18:02:25','2026-07-30 18:02:25',-1,0,'Important',NULL);

CREATE TABLE `ActiveAlarmVO` (
  `uuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  `namespace` varchar(128) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `alarmUuid` (`alarmUuid`),
  KEY `fkActiveAlarmVOActiveAlarmTemplateVO` (`templateUuid`),
  CONSTRAINT `fkActiveAlarmVOActiveAlarmTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `ActiveAlarmTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AddingNewInstanceRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `AdjustmentType` varchar(256) NOT NULL,
  `adjustmentValue` int(10) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AddressPoolVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkAddressPoolVOIpRangeEO` FOREIGN KEY (`uuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AffinityGroupUsageVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `affinityGroupUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAffinityGroupUsageVOcreateAffinityGroupVO` (`affinityGroupUuid`),
  CONSTRAINT `fkAffinityGroupUsageVOcreateAffinityGroupVO` FOREIGN KEY (`affinityGroupUuid`) REFERENCES `AffinityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AffinityGroupVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `policy` varchar(255) NOT NULL,
  `version` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `appliance` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `state` varchar(128) DEFAULT 'Enabled',
  `zoneUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AgentVersionVO` (
  `uuid` varchar(32) NOT NULL,
  `agentType` varchar(255) DEFAULT NULL,
  `currentVersion` varchar(255) DEFAULT NULL,
  `expectVersion` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AiSiNoSecretResourcePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `managementIp` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `route` varchar(32) NOT NULL,
  `clientID` varchar(32) NOT NULL,
  `clientSecrete` varchar(32) NOT NULL,
  `appId` varchar(8) NOT NULL,
  `keyNumSM2` varchar(8) NOT NULL,
  `keyNumSM4` varchar(8) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkAiSiNoSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmActionVO` (
  `alarmUuid` varchar(32) NOT NULL,
  `actionUuid` varchar(32) NOT NULL,
  `actionType` varchar(128) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '2018-05-10 06:04:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '2018-05-10 06:04:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`alarmUuid`,`actionUuid`),
  CONSTRAINT `fkAlarmActionVOAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmLabelVO` (
  `uuid` varchar(32) NOT NULL,
  `key` varchar(128) NOT NULL,
  `value` text NOT NULL,
  `operator` varchar(128) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `alarmUuid` (`alarmUuid`,`key`),
  CONSTRAINT `fkAlarmLabelVOAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmRecordsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `accountUuid` varchar(32) DEFAULT NULL,
  `alarmName` varchar(255) NOT NULL,
  `alarmStatus` varchar(64) DEFAULT NULL,
  `alarmUuid` varchar(32) DEFAULT NULL,
  `comparisonOperator` varchar(128) DEFAULT NULL,
  `context` text,
  `dataUuid` varchar(32) DEFAULT NULL,
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` text,
  `metricName` varchar(256) DEFAULT NULL,
  `metricValue` double DEFAULT NULL,
  `namespace` varchar(256) DEFAULT NULL,
  `period` int(10) unsigned NOT NULL,
  `readStatus` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `operatorAccountUuid` char(32) DEFAULT NULL,
  `resourceType` varchar(256) NOT NULL,
  `resourceUuid` text,
  `threshold` double NOT NULL,
  `hour` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataUuid` (`dataUuid`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxAccountUuid` (`accountUuid`),
  KEY `idxAccountUuidCreateTime` (`accountUuid`,`createTime`),
  KEY `idxAlarmUuid` (`alarmUuid`),
  KEY `idxAccountUuidHourEmergencyLevel` (`accountUuid`,`hour`,`emergencyLevel`),
  KEY `idxCreateTimeReadStatusEmergencyLevel` (`createTime`,`emergencyLevel`,`readStatus`,`accountUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `comparisonOperator` varchar(128) NOT NULL,
  `period` int(10) unsigned NOT NULL,
  `repeatInterval` int(10) unsigned NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `metricName` varchar(512) NOT NULL,
  `threshold` double NOT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(32) NOT NULL,
  `repeatCount` int(11) DEFAULT NULL,
  `enableRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlertDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `alertType` varchar(255) NOT NULL,
  `ackPeriod` int(10) unsigned NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `ackDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `resumeAlert` tinyint(1) NOT NULL DEFAULT '0',
  `operatorAccountUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`),
  KEY `resourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlertVO` (
  `uuid` varchar(32) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `triggerStatus` varchar(64) NOT NULL,
  `content` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunDiskVO` (
  `uuid` varchar(32) NOT NULL,
  `diskId` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `identityZoneUuid` varchar(32) NOT NULL,
  `ecsInstanceUuid` varchar(32) DEFAULT NULL,
  `diskType` varchar(16) NOT NULL,
  `diskCategory` varchar(16) DEFAULT NULL,
  `diskChargeType` varchar(16) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `sizeWithGB` int(10) unsigned DEFAULT NULL,
  `deviceInfo` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunDiskVOIdentityZoneVO` (`identityZoneUuid`),
  KEY `fkAliyunDiskVOEcsInstanceVO` (`ecsInstanceUuid`),
  CONSTRAINT `fkAliyunDiskVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`),
  CONSTRAINT `fkAliyunDiskVOEcsInstanceVO` FOREIGN KEY (`ecsInstanceUuid`) REFERENCES `EcsInstanceVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunEbsBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `ossBucketUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunEbsBackupStorageVOOssBucketVO` (`ossBucketUuid`),
  CONSTRAINT `fkAliyunEbsBackupStorageVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `OssBucketVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunEbsPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `panguAppName` varchar(255) DEFAULT NULL,
  `panguPartitionName` varchar(255) DEFAULT NULL,
  `defaultIoType` varchar(16) NOT NULL,
  `identityZoneUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunEbsPrimaryStorageVOIdentityZoneVO` (`identityZoneUuid`),
  CONSTRAINT `fkAliyunEbsPrimaryStorageVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasAccessGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `type` varchar(16) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunNasAccessGroupVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkAliyunNasAccessGroupVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasAccessRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `accessGroupUuid` varchar(32) NOT NULL,
  `rule` varchar(16) NOT NULL,
  `priority` int(10) unsigned DEFAULT NULL,
  `sourceCidr` varchar(32) NOT NULL,
  `userAccess` varchar(32) NOT NULL,
  `ruleId` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunNasAccessRuleVOAliyunNasAccessGroupVO` (`accessGroupUuid`),
  CONSTRAINT `fkAliyunNasAccessRuleVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `AliyunNasAccessGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasFileSystemVO` (
  `uuid` varchar(32) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `storageType` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunNasFileSystemVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkAliyunNasFileSystemVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasMountTargetVO` (
  `uuid` varchar(32) NOT NULL,
  `accessGroupUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunNasMountTargetVOAliyunNasAccessGroupVO` (`accessGroupUuid`),
  CONSTRAINT `fkAliyunNasMountTargetVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `AliyunNasAccessGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasMountVolumeRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) DEFAULT NULL,
  `imageUuid` varchar(32) DEFAULT NULL,
  `nasMountUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `sourceType` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `dataPath` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAliyunNasMountVolumeRefVOVolumeEO` (`volumeUuid`),
  KEY `fkAliyunNasMountVolumeRefVOImageEO` (`imageUuid`),
  KEY `fkAliyunNasMountVolumeRefVOHostEO` (`hostUuid`),
  KEY `fkAliyunNasMountVolumeRefVOAliyunNasMountTargetVO` (`nasMountUuid`),
  CONSTRAINT `fkAliyunNasMountVolumeRefVOAliyunNasMountTargetVO` FOREIGN KEY (`nasMountUuid`) REFERENCES `AliyunNasMountTargetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAliyunNasMountVolumeRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAliyunNasMountVolumeRefVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAliyunNasMountVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasPrimaryStorageFileSystemRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `nasFileSystemUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPSNasRefVONasFileSystemVO` (`nasFileSystemUuid`),
  KEY `fkPSNasRefVOPrimaryStorageVO` (`primaryStorageUuid`),
  CONSTRAINT `fkPSNasRefVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `NasFileSystemVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPSNasRefVOPrimaryStorageVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunNasPrimaryStorageMountPointVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `mountUrl` varchar(512) NOT NULL,
  `mountPath` varchar(512) NOT NULL,
  `lastErrInfo` varchar(1024) DEFAULT NULL,
  `checkTimes` bigint(20) unsigned NOT NULL,
  `errorTimes` bigint(20) unsigned DEFAULT '0',
  `lastNormalDistance` bigint(20) unsigned DEFAULT '0',
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxMountPointVOhostUuid` (`hostUuid`),
  KEY `fkMountPointVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkMountPointVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMountPointVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunPanguPartitionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `identityZoneUuid` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `appName` varchar(255) NOT NULL,
  `partitionName` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunPanguPartitionVOIdentityZoneVO` (`identityZoneUuid`),
  CONSTRAINT `fkAliyunPanguPartitionVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunProxyVSwitchVO` (
  `uuid` varchar(32) NOT NULL,
  `vpcL3NetworkUuid` varchar(32) NOT NULL,
  `aliyunProxyVpcUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `isDefault` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  KEY `fkAliyunProxyVSwitchVOAliyunProxyVpcVO` (`aliyunProxyVpcUuid`),
  KEY `fkAliyunProxyVSwitchVOL3NetworkEO` (`vpcL3NetworkUuid`),
  CONSTRAINT `fkAliyunProxyVSwitchVOL3NetworkEO` FOREIGN KEY (`vpcL3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`),
  CONSTRAINT `fkAliyunProxyVSwitchVOAliyunProxyVpcVO` FOREIGN KEY (`aliyunProxyVpcUuid`) REFERENCES `AliyunProxyVpcVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunProxyVpcVO` (
  `uuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `vpcName` varchar(128) NOT NULL,
  `cidrBlock` varchar(128) NOT NULL,
  `vRouterUuid` varchar(32) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `isDefault` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunProxyVpcVOVmInstanceEO` (`vRouterUuid`),
  CONSTRAINT `fkAliyunProxyVpcVOVmInstanceEO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunRouterInterfaceVO` (
  `uuid` varchar(32) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `routerInterfaceId` varchar(64) NOT NULL,
  `virtualRouterUuid` varchar(32) NOT NULL,
  `accessPointUuid` varchar(32) DEFAULT NULL,
  `vRouterType` varchar(16) NOT NULL,
  `role` varchar(16) NOT NULL,
  `spec` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `oppositeInterfaceUuid` varchar(32) DEFAULT NULL,
  `description` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVirtualRouterInterfaceVOConnectionAccessPointVO` (`accessPointUuid`),
  KEY `fkVirtualRouterInterfaceVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkVirtualRouterInterfaceVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `ConnectionAccessPointVO` (`uuid`),
  CONSTRAINT `fkVirtualRouterInterfaceVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunSmsSNSTextTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `sign` varchar(24) NOT NULL,
  `alarmTemplateCode` varchar(24) NOT NULL,
  `eventTemplateCode` varchar(24) NOT NULL,
  `eventTemplate` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkAliyunSmsSNSTextTemplateVOSNSTextTemplateVO` FOREIGN KEY (`uuid`) REFERENCES `SNSTextTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AliyunSnapshotVO` (
  `uuid` varchar(32) NOT NULL,
  `snapshotId` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `diskUuid` varchar(32) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `aliyunSnapshotUsage` varchar(16) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAliyunSnapshotVOAliyunDiskVO` (`diskUuid`),
  CONSTRAINT `fkAliyunSnapshotVOAliyunDiskVO` FOREIGN KEY (`diskUuid`) REFERENCES `AliyunDiskVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AppBuildSystemVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `storageType` varchar(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `status` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AppBuildSystemZoneRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `buildSystemUuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkAppBuildSystemZoneRefVOZoneEO` (`zoneUuid`),
  KEY `fkAppBuildSystemZoneRefVOAppBuildSystemVO` (`buildSystemUuid`),
  CONSTRAINT `fkAppBuildSystemZoneRefVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAppBuildSystemZoneRefVOAppBuildSystemVO` FOREIGN KEY (`buildSystemUuid`) REFERENCES `AppBuildSystemVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ApplianceVmFirewallRuleVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `applianceVmUuid` varchar(32) NOT NULL,
  `protocol` varchar(16) DEFAULT NULL,
  `sourceIp` varchar(128) DEFAULT NULL,
  `destIp` varchar(128) DEFAULT NULL,
  `startPort` int(10) unsigned DEFAULT '0',
  `endPort` int(10) unsigned DEFAULT '0',
  `allowCidr` varchar(32) DEFAULT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `identity` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkApplianceVmFirewallRuleVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `fkApplianceVmFirewallRuleVOVmInstanceEO` (`applianceVmUuid`),
  KEY `idxApplianceVmFirewallRuleVOprotocol` (`protocol`),
  KEY `idxApplianceVmFirewallRuleVOstartPort` (`startPort`),
  KEY `idxApplianceVmFirewallRuleVOendPort` (`endPort`),
  KEY `idxApplianceVmFirewallRuleVOallowCidr` (`allowCidr`),
  KEY `idxApplianceVmFirewallRuleVOsourceIp` (`sourceIp`),
  KEY `idxApplianceVmFirewallRuleVOdestIp` (`destIp`),
  KEY `idxApplianceVmFirewallRuleVOidentity` (`identity`),
  CONSTRAINT `fkApplianceVmFirewallRuleVOVmInstanceEO` FOREIGN KEY (`applianceVmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkApplianceVmFirewallRuleVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ApplianceVmVO` (
  `uuid` varchar(32) NOT NULL,
  `applianceVmType` varchar(64) NOT NULL,
  `managementNetworkUuid` varchar(32) DEFAULT NULL,
  `defaultRouteL3NetworkUuid` varchar(32) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `agentPort` int(10) unsigned DEFAULT '7759',
  `haStatus` varchar(255) DEFAULT 'NoHa',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkApplianceVmVOL3NetworkEO` (`managementNetworkUuid`),
  KEY `fkApplianceVmVOL3NetworkEO1` (`defaultRouteL3NetworkUuid`),
  CONSTRAINT `fkApplianceVmVOL3NetworkEO` FOREIGN KEY (`managementNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`),
  CONSTRAINT `fkApplianceVmVOL3NetworkEO1` FOREIGN KEY (`defaultRouteL3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`),
  CONSTRAINT `fkApplianceVmVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ArchiveTicketStatusHistoryVO` (
  `uuid` varchar(32) NOT NULL,
  `historyUuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `ticketUuid` varchar(32) NOT NULL,
  `fromStatus` varchar(255) NOT NULL,
  `toStatus` varchar(255) NOT NULL,
  `comment` text,
  `operatorUuid` varchar(32) NOT NULL,
  `operatorType` varchar(255) NOT NULL,
  `operationContext` text,
  `operationContextType` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `sequence` int(11) NOT NULL AUTO_INCREMENT,
  `flowName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `sequence` (`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ArchiveTicketVO` (
  `uuid` varchar(32) NOT NULL,
  `ticketUuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  `status` varchar(255) NOT NULL,
  `accountSystemType` varchar(255) NOT NULL,
  `accountSystemContext` text,
  `requests` text NOT NULL,
  `flowCollectionUuid` varchar(32) NOT NULL,
  `currentFlowUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ticketTypeUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AsyncRestVO` (
  `uuid` varchar(32) NOT NULL,
  `requestData` longtext,
  `state` varchar(64) NOT NULL,
  `result` mediumtext,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AuditsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `apiName` varchar(2048) NOT NULL,
  `clientBrowser` varchar(64) NOT NULL,
  `clientIp` varchar(64) NOT NULL,
  `duration` int(10) unsigned NOT NULL,
  `error` text,
  `operator` varchar(256) DEFAULT NULL,
  `requestDump` text,
  `resourceType` varchar(256) NOT NULL,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `requestUuid` varchar(32) DEFAULT NULL,
  `responseDump` text,
  `success` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'api call success or failed',
  `operatorAccountUuid` varchar(32) DEFAULT NULL,
  `signedText` text,
  `resourceName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxResourceUuid` (`resourceUuid`),
  KEY `idxSuccess` (`success`),
  KEY `idxOperatorAccountUuid` (`operatorAccountUuid`),
  KEY `idxRequestUuid` (`requestUuid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupActivityVO` (
  `uuid` varchar(32) NOT NULL,
  `scalingGroupUuid` varchar(32) NOT NULL,
  `activityAction` varchar(128) NOT NULL,
  `scalingGroupRuleUuid` varchar(32) DEFAULT NULL,
  `name` varchar(256) NOT NULL,
  `cause` varchar(128) NOT NULL,
  `status` varchar(128) NOT NULL,
  `activityActionResultMessage` text,
  `description` varchar(256) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `endDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `instanceUuids` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAutoScalingGroupActivityVOAutoScalingGroupVO` (`scalingGroupUuid`),
  KEY `fkAutoScalingGroupActivityVOAutoScalingRuleVO` (`scalingGroupRuleUuid`),
  KEY `indexActivityVOLastOpDate` (`lastOpDate`),
  CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`),
  CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingRuleVO` FOREIGN KEY (`scalingGroupRuleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `instanceUuid` varchar(32) NOT NULL,
  `scalingGroupUuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) DEFAULT NULL,
  `scalingGroupActivityUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `healthStatus` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `description` varchar(256) DEFAULT NULL,
  `protectionStrategy` varchar(128) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `instanceUuid` (`instanceUuid`),
  KEY `fkAutoScalingGroupInstanceVOAutoScalingGroupVO` (`scalingGroupUuid`),
  KEY `fkAutoScalingGroupInstanceVOAutoScalingTemplateVO` (`templateUuid`),
  KEY `fkAutoScalingGroupInstanceVOAutoScalingGroupActivityVO` (`scalingGroupActivityUuid`),
  CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`),
  CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `scalingResourceType` varchar(256) NOT NULL,
  `removalPolicy` varchar(256) NOT NULL,
  `minResourceSize` int(10) NOT NULL,
  `maxResourceSize` int(10) NOT NULL,
  `state` varchar(256) NOT NULL,
  `defaultCooldown` mediumtext NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleAlarmTriggerVO` (
  `uuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `alarmUuid` (`alarmUuid`),
  CONSTRAINT `fkAutoScalingRuleInstanceAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleSchedulerJobTriggerVO` (
  `uuid` varchar(32) NOT NULL,
  `schedulerJobUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `schedulerJobUuid` (`schedulerJobUuid`),
  CONSTRAINT `fkAutoScalingRuleSchedulerJobTriggerVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleTriggerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `ruleUuid` varchar(32) NOT NULL,
  `type` varchar(256) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `state` varchar(256) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAutoScalingRuleTriggerVOAutoScalingRuleVO` (`ruleUuid`),
  CONSTRAINT `fkAutoScalingRuleTriggerVOAutoScalingRuleVO` FOREIGN KEY (`ruleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `scalingGroupUuid` varchar(32) NOT NULL,
  `type` varchar(256) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `cooldown` mediumtext,
  `state` varchar(256) NOT NULL,
  `status` varchar(256) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkAutoScalingRuleVOAutoScalingGroupVO` (`scalingGroupUuid`),
  CONSTRAINT `fkAutoScalingRuleVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingTemplateGroupRefVO` (
  `groupUuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`groupUuid`),
  UNIQUE KEY `groupUuid` (`groupUuid`),
  KEY `fkAutoScalingTemplateGroupRefVOAutoScalingTemplateVO` (`templateUuid`),
  CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `type` varchar(256) NOT NULL,
  `state` varchar(256) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingVmTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `vmInstanceName` varchar(256) NOT NULL,
  `vmInstanceDescription` varchar(256) DEFAULT NULL,
  `vmInstanceType` varchar(256) NOT NULL,
  `vmInstanceOfferingUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `l3NetworkUuids` text,
  `rootDiskOfferingUuid` varchar(32) DEFAULT NULL,
  `dataDiskOfferingUuids` text,
  `vmInstanceZoneUuid` varchar(32) DEFAULT NULL,
  `vmInstanceClusterUuid` varchar(32) DEFAULT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `primaryStorageUuidForRootVolume` varchar(32) DEFAULT NULL,
  `defaultL3NetworkUuid` varchar(32) DEFAULT NULL,
  `strategy` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BackupStorageEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'backup storage name',
  `url` varchar(2048) NOT NULL COMMENT 'url, can be ip or fqdn name or type specific string',
  `description` varchar(2048) DEFAULT NULL COMMENT 'backup storage description',
  `totalCapacity` bigint(20) unsigned NOT NULL COMMENT 'total capacity of backup storage in bytes',
  `availableCapacity` bigint(20) unsigned NOT NULL,
  `state` varchar(32) NOT NULL COMMENT 'backup storage state',
  `status` varchar(32) NOT NULL COMMENT 'backup storage status',
  `type` varchar(32) NOT NULL COMMENT 'backup storage type',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxBackupStorageEOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BackupStorageZoneRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `backupStorageUuid` varchar(32) NOT NULL COMMENT 'uuid of backup storage',
  `zoneUuid` varchar(32) NOT NULL COMMENT 'uuid of zone',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkBackupStorageZoneRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkBackupStorageZoneRefVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkBackupStorageZoneRefVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBackupStorageZoneRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2BillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bareMetal2ChassisOfferingUUid` varchar(32) NOT NULL,
  `bareMetal2ChassisOfferingName` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2BondingNicRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nicUuid` varchar(32) DEFAULT NULL,
  `instanceUuid` varchar(32) NOT NULL,
  `bondingUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `provisionNicUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fkinstance` (`instanceUuid`),
  KEY `fknic` (`nicUuid`),
  KEY `fkbonding` (`bondingUuid`),
  KEY `fkBareMetal2BondingNicRefVOProvisionNicVO` (`provisionNicUuid`),
  CONSTRAINT `fkBareMetal2BondingNicRefVOProvisionNicVO` FOREIGN KEY (`provisionNicUuid`) REFERENCES `BareMetal2InstanceProvisionNicVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkbonding` FOREIGN KEY (`bondingUuid`) REFERENCES `BareMetal2BondingVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkinstance` FOREIGN KEY (`instanceUuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fknic` FOREIGN KEY (`nicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2BondingVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `slaves` varchar(255) NOT NULL,
  `opts` varchar(255) DEFAULT NULL,
  `chassisUuid` varchar(32) NOT NULL,
  `mode` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `fkchassisUuid` (`chassisUuid`),
  CONSTRAINT `fkchassisUuid` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ChassisDiskVO` (
  `uuid` varchar(32) NOT NULL,
  `chassisUuid` varchar(32) NOT NULL,
  `type` varchar(32) DEFAULT '',
  `diskSize` bigint(20) unsigned NOT NULL COMMENT 'disk size in bytes',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `wwn` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBareMetal2ChassisDiskVOChassisVO` (`chassisUuid`),
  CONSTRAINT `fkBareMetal2ChassisDiskVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ChassisNicVO` (
  `uuid` varchar(32) NOT NULL,
  `chassisUuid` varchar(32) NOT NULL,
  `mac` varchar(32) NOT NULL,
  `speed` varchar(32) DEFAULT NULL,
  `isProvisionNic` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `nicName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `mac` (`mac`),
  KEY `fkBareMetal2ChassisNicVOChassisVO` (`chassisUuid`),
  CONSTRAINT `fkBareMetal2ChassisNicVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ChassisOfferingVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `architecture` varchar(32) NOT NULL,
  `cpuModelName` varchar(255) NOT NULL,
  `cpuNum` int(10) unsigned NOT NULL,
  `memorySize` bigint(20) unsigned NOT NULL COMMENT 'memory size in bytes',
  `bootMode` varchar(32) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `provisionType` varchar(32) NOT NULL DEFAULT 'Remote',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ChassisVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `chassisOfferingUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `powerStatus` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `provisionType` varchar(32) NOT NULL DEFAULT 'Remote',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBareMetal2ChassisVOZoneEO` (`zoneUuid`),
  KEY `fkBareMetal2ChassisVOClusterEO` (`clusterUuid`),
  KEY `fkBareMetal2ChassisVOOfferingVO` (`chassisOfferingUuid`),
  CONSTRAINT `fkBareMetal2ChassisVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBareMetal2ChassisVOOfferingVO` FOREIGN KEY (`chassisOfferingUuid`) REFERENCES `BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2ChassisVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2GatewayClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `clusterUuid` varchar(32) NOT NULL,
  `gatewayUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `ukBareMetal2GatewayClusterRefVO` (`clusterUuid`,`gatewayUuid`),
  KEY `fkBareMetal2GatewayVOGatewayVO` (`gatewayUuid`),
  CONSTRAINT `fkBareMetal2GatewayVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBareMetal2GatewayVOGatewayVO` FOREIGN KEY (`gatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2GatewayProvisionNicVO` (
  `uuid` varchar(32) NOT NULL,
  `networkUuid` varchar(32) NOT NULL,
  `interfaceName` varchar(17) NOT NULL,
  `ip` varchar(128) NOT NULL,
  `netmask` varchar(128) NOT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `metaData` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBareMetal2GatewayProvisionNicVONetworkVO` (`networkUuid`),
  CONSTRAINT `fkBareMetal2GatewayProvisionNicVOGatewayVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBareMetal2GatewayProvisionNicVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2GatewayVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkBareMetal2GatewayVOKVMHostVO` FOREIGN KEY (`uuid`) REFERENCES `KVMHostVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2InstanceProvisionNicVO` (
  `uuid` varchar(32) NOT NULL,
  `networkUuid` varchar(32) NOT NULL,
  `mac` varchar(17) NOT NULL,
  `ip` varchar(128) NOT NULL,
  `netmask` varchar(128) NOT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `metaData` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `mac` (`mac`),
  KEY `fkBareMetal2InstanceProvisionNicVONetworkVO` (`networkUuid`),
  CONSTRAINT `fkBareMetal2InstanceProvisionNicVOInstanceVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBareMetal2InstanceProvisionNicVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2InstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `chassisUuid` varchar(32) DEFAULT NULL,
  `lastChassisUuid` varchar(32) DEFAULT NULL,
  `gatewayUuid` varchar(32) DEFAULT NULL,
  `lastGatewayUuid` varchar(32) DEFAULT NULL,
  `chassisOfferingUuid` varchar(32) DEFAULT NULL,
  `gatewayAllocatorStrategy` varchar(64) DEFAULT NULL,
  `provisionType` varchar(32) NOT NULL DEFAULT 'Remote',
  `agentVersion` varchar(32) DEFAULT NULL,
  `isLatestAgent` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBareMetal2InstanceVOChassisVO` (`chassisUuid`),
  KEY `fkBareMetal2InstanceVOChassisVO1` (`lastChassisUuid`),
  KEY `fkBareMetal2InstanceVOGatewayVO` (`gatewayUuid`),
  KEY `fkBareMetal2InstanceVOGatewayVO1` (`lastGatewayUuid`),
  KEY `fkBareMetal2InstanceVOChassisOfferingVO` (`chassisOfferingUuid`),
  CONSTRAINT `fkBareMetal2InstanceVOChassisOfferingVO` FOREIGN KEY (`chassisOfferingUuid`) REFERENCES `BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2InstanceVOChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2InstanceVOChassisVO1` FOREIGN KEY (`lastChassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2InstanceVOGatewayVO` FOREIGN KEY (`gatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2InstanceVOGatewayVO1` FOREIGN KEY (`lastGatewayUuid`) REFERENCES `BareMetal2GatewayVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBareMetal2InstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2IpmiChassisVO` (
  `uuid` varchar(32) NOT NULL,
  `ipmiAddress` varchar(32) NOT NULL,
  `ipmiPort` int(10) unsigned NOT NULL,
  `ipmiUsername` varchar(255) NOT NULL,
  `ipmiPassword` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukBareMetal2IpmiChassisVO` (`ipmiAddress`,`ipmiPort`),
  CONSTRAINT `fkBareMetal2IpmiChassisVOChassisVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ProvisionNetworkClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `clusterUuid` varchar(32) NOT NULL,
  `networkUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `ukBareMetal2ProvisionNetworkClusterRefVO` (`clusterUuid`,`networkUuid`),
  KEY `fkBareMetal2ProvisionNetworkVONetworkVO` (`networkUuid`),
  CONSTRAINT `fkBareMetal2ProvisionNetworkVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBareMetal2ProvisionNetworkVONetworkVO` FOREIGN KEY (`networkUuid`) REFERENCES `BareMetal2ProvisionNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2ProvisionNetworkVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `dhcpInterface` varchar(128) NOT NULL,
  `dhcpRangeStartIp` varchar(32) NOT NULL,
  `dhcpRangeEndIp` varchar(32) NOT NULL,
  `dhcpRangeNetmask` varchar(32) NOT NULL,
  `dhcpRangeGateway` varchar(32) DEFAULT NULL,
  `dhcpRangeNetworkCidr` varchar(64) DEFAULT NULL,
  `state` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBareMetal2ProvisionNetworkVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkBareMetal2ProvisionNetworkVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2UsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmName` varchar(255) DEFAULT NULL,
  `state` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  KEY `idxBareMetal2VmUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `BareMetal2UsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmName` varchar(255) DEFAULT NULL,
  `state` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  KEY `idxBareMetal2VmUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalBondingVO` (
  `uuid` varchar(32) NOT NULL,
  `chassisUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `mode` tinyint(3) unsigned NOT NULL,
  `slaves` varchar(2048) NOT NULL,
  `opts` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalChassisVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `ipmiAddress` varchar(32) NOT NULL COMMENT 'baremetal chassis ipmi address',
  `ipmiUsername` varchar(255) NOT NULL COMMENT 'baremetal chassis ipmi username',
  `ipmiPassword` varchar(255) NOT NULL COMMENT 'baremetal chassis ipmi password',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `name` varchar(255) DEFAULT NULL COMMENT 'baremetal chassis name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal chassis description',
  `ipmiPort` int(11) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL COMMENT 'baremetal chassis status',
  `state` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `pxeServerUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukBaremetalChassisVO` (`ipmiAddress`,`ipmiPort`),
  KEY `fkBaremetalChassisVOZoneEO` (`zoneUuid`),
  KEY `fkBaremetalChassisVOClusterEO` (`clusterUuid`),
  KEY `fkBaremetalChassisVOBaremetalPxeServerVO` (`pxeServerUuid`),
  CONSTRAINT `fkBaremetalChassisVOBaremetalPxeServerVO` FOREIGN KEY (`pxeServerUuid`) REFERENCES `BaremetalPxeServerVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalChassisVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`),
  CONSTRAINT `fkBaremetalChassisVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalHardwareInfoVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `chassisUuid` varchar(32) NOT NULL COMMENT 'baremetal chassis uuid',
  `type` varchar(255) DEFAULT NULL,
  `content` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBaremetalHardwareInfoVOBaremetalChassisVO` (`chassisUuid`),
  CONSTRAINT `fkBaremetalHardwareInfoVOBaremetalChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BaremetalChassisVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalImageCacheVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `imageUuid` varchar(32) DEFAULT NULL,
  `url` varchar(1024) NOT NULL,
  `installUrl` varchar(1024) NOT NULL,
  `mediaType` varchar(64) NOT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `actualSize` bigint(20) unsigned NOT NULL,
  `md5sum` varchar(255) DEFAULT NULL,
  `utilization` bigint(20) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `pxeServerUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkBaremetalImageCacheVOImageEO` (`imageUuid`),
  KEY `fkBaremetalImageCacheVOBaremetalPxeServerVO` (`pxeServerUuid`),
  CONSTRAINT `fkBaremetalImageCacheVOBaremetalPxeServerVO` FOREIGN KEY (`pxeServerUuid`) REFERENCES `BaremetalPxeServerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBaremetalImageCacheVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalInstanceSequenceNumberVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `internalId` bigint(20) unsigned NOT NULL,
  `zoneUuid` varchar(32) DEFAULT NULL,
  `clusterUuid` varchar(32) DEFAULT NULL,
  `chassisUuid` varchar(32) DEFAULT NULL,
  `imageUuid` varchar(32) DEFAULT NULL,
  `platform` varchar(255) NOT NULL,
  `managementIp` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `port` int(10) unsigned DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `status` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `pxeServerUuid` varchar(32) DEFAULT NULL,
  `templateUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBaremetalInstanceVOZoneEO` (`zoneUuid`),
  KEY `fkBaremetalInstanceVOClusterEO` (`clusterUuid`),
  KEY `fkBaremetalInstanceVOBaremetalChassisVO` (`chassisUuid`),
  KEY `fkBaremetalInstanceVOImageEO` (`imageUuid`),
  KEY `fkBaremetalInstanceVOBaremetalPxeServerVO` (`pxeServerUuid`),
  KEY `fkBaremetalInstanceVOPreconfigurationTemplateVO` (`templateUuid`),
  CONSTRAINT `fkBaremetalInstanceVOPreconfigurationTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `PreconfigurationTemplateVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalInstanceVOBaremetalChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BaremetalChassisVO` (`uuid`),
  CONSTRAINT `fkBaremetalInstanceVOBaremetalPxeServerVO` FOREIGN KEY (`pxeServerUuid`) REFERENCES `BaremetalPxeServerVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalInstanceVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalInstanceVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalInstanceVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalNicVO` (
  `uuid` varchar(32) NOT NULL,
  `baremetalInstanceUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) DEFAULT NULL,
  `usedIpUuid` varchar(32) DEFAULT NULL,
  `mac` varchar(255) DEFAULT NULL,
  `ip` varchar(128) NOT NULL,
  `netmask` varchar(128) DEFAULT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `metaData` varchar(255) DEFAULT NULL,
  `pxe` tinyint(1) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `baremetalBondingUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `usedIpUuid` (`usedIpUuid`),
  UNIQUE KEY `ukBaremetalNicVO` (`mac`,`baremetalBondingUuid`),
  KEY `fkBaremetalNicVOBaremetalInstanceVO` (`baremetalInstanceUuid`),
  KEY `fkBaremetalNicVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkBaremetalNicVOBaremetalInstanceVO` FOREIGN KEY (`baremetalInstanceUuid`) REFERENCES `BaremetalInstanceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBaremetalNicVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkBaremetalNicVOUsedIpVO` FOREIGN KEY (`usedIpUuid`) REFERENCES `UsedIpVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalPxeServerClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `clusterUuid` varchar(32) NOT NULL,
  `pxeServerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkBaremetalPxeServerClusterRefVOClusterEO` (`clusterUuid`),
  KEY `fkBaremetalPxeServerClusterRefVOBaremetalPxeServerVO` (`pxeServerUuid`),
  CONSTRAINT `fkBaremetalPxeServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkBaremetalPxeServerClusterRefVOBaremetalPxeServerVO` FOREIGN KEY (`pxeServerUuid`) REFERENCES `BaremetalPxeServerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalPxeServerVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `dhcpInterface` varchar(128) NOT NULL COMMENT 'pxe dhcp interface',
  `dhcpRangeBegin` varchar(32) DEFAULT NULL COMMENT 'dhcp range begin',
  `dhcpRangeEnd` varchar(32) DEFAULT NULL COMMENT 'dhcp range end',
  `dhcpRangeNetmask` varchar(32) DEFAULT NULL COMMENT 'dhcp range netmask',
  `status` varchar(32) DEFAULT NULL COMMENT 'pxe server status',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `name` varchar(255) DEFAULT NULL COMMENT 'baremetal pxeserver name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal pxeserver description',
  `zoneUuid` varchar(32) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `sshUsername` varchar(64) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `storagePath` varchar(2048) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) unsigned DEFAULT '0',
  `dhcpInterfaceAddress` varchar(32) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `hostname` (`hostname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BaremetalVlanNicVO` (
  `uuid` varchar(32) NOT NULL,
  `vlan` int(10) unsigned NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkBaremetalVlanNicVOBaremetalNicVO` FOREIGN KEY (`uuid`) REFERENCES `BaremetalNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BillingResourceLabelVO` (
  `resourceUuid` varchar(32) NOT NULL,
  `labelKey` varchar(255) NOT NULL,
  `labelValue` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`resourceUuid`,`labelKey`),
  KEY `resourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `billingType` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceName` varchar(255) DEFAULT NULL,
  `spending` double unsigned NOT NULL,
  `startTime` bigint(20) unsigned NOT NULL,
  `endTime` bigint(20) unsigned NOT NULL,
  `hypervisorType` varchar(64) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `resourceUuid` (`resourceUuid`),
  KEY `acountUuid` (`accountUuid`),
  KEY `idxBillingVOaccountUuid` (`accountUuid`,`startTime`,`endTime`),
  KEY `idxAccountUuidCreateDate` (`accountUuid`,`createDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BlockPrimaryStorageHostRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `initiatorName` varchar(256) DEFAULT NULL,
  `metadata` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `fkBlockPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BlockPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `vendorName` varchar(256) NOT NULL,
  `metadata` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BlockScsiLunVO` (
  `uuid` varchar(32) NOT NULL,
  `target` varchar(256) DEFAULT NULL,
  `name` varchar(256) DEFAULT NULL,
  `id` int(10) unsigned DEFAULT '0',
  `wwn` varchar(256) DEFAULT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `lunMapId` int(10) unsigned DEFAULT '0',
  `lunInitSnapshotID` bigint(20) unsigned DEFAULT '0',
  `usedSize` bigint(20) unsigned DEFAULT '0',
  `encryptedId` smallint(5) unsigned DEFAULT '0',
  `encryptedWwn` varchar(256) DEFAULT NULL,
  `lunType` varchar(256) DEFAULT NULL,
  `volumeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkScsiLunVOVolumeVO` (`volumeUuid`),
  CONSTRAINT `fkScsiLunVOVolumeVO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BlockVolumeVO` (
  `uuid` varchar(32) NOT NULL,
  `iscsiPath` varchar(1024) NOT NULL,
  `vendor` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkBlockVolumeVOVolumeVO` FOREIGN KEY (`uuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BuildAppExportHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `buildAppUuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `path` varchar(2048) DEFAULT NULL,
  `size` bigint(20) unsigned DEFAULT '0',
  `md5Sum` varchar(255) NOT NULL,
  `version` varchar(127) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxBuildAppExportHistoryVObuildAppUuid` (`buildAppUuid`),
  KEY `idxBuildAppExportHistoryVOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BuildAppImageRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `imageUuid` varchar(32) NOT NULL,
  `imageName` varchar(255) NOT NULL,
  `buildAppUuid` varchar(32) NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkBuildAppImageRefVOImageVO` (`imageUuid`),
  KEY `fkBuildAppImageRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkBuildAppImageRefVOBuildApplicationVO` (`buildAppUuid`),
  CONSTRAINT `fkBuildAppImageRefVOImageVO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`),
  CONSTRAINT `fkBuildAppImageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`),
  CONSTRAINT `fkBuildAppImageRefVOBuildApplicationVO` FOREIGN KEY (`buildAppUuid`) REFERENCES `BuildApplicationVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `BuildApplicationVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `buildSystemUuid` varchar(32) DEFAULT NULL,
  `templateContent` mediumtext NOT NULL,
  `appMetaData` mediumtext NOT NULL,
  `appId` varchar(255) NOT NULL,
  `version` varchar(127) NOT NULL,
  `installPath` varchar(1024) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkBuildApplicationVOAppBuildSystemVO` (`buildSystemUuid`),
  CONSTRAINT `fkBuildApplicationVOAppBuildSystemVO` FOREIGN KEY (`buildSystemUuid`) REFERENCES `AppBuildSystemVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CCSCertificateAccountRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `accountUuid` char(32) NOT NULL,
  `certificateUuid` char(32) NOT NULL,
  `state` varchar(10) NOT NULL DEFAULT 'Disabled',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkCCSCertificateUserRefVOCertificateUuid` (`certificateUuid`),
  CONSTRAINT `fkCCSCertificateUserRefVOCertificateUuid` FOREIGN KEY (`certificateUuid`) REFERENCES `CCSCertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CCSCertificateVO` (
  `uuid` varchar(32) NOT NULL,
  `algorithm` varchar(10) NOT NULL DEFAULT 'SM2',
  `format` char(3) NOT NULL DEFAULT 'CER',
  `issuerDN` varchar(255) NOT NULL,
  `subjectDN` varchar(255) NOT NULL,
  `serNumber` varchar(128) NOT NULL,
  `effectiveTime` bigint(20) unsigned NOT NULL DEFAULT '0',
  `expirationTime` bigint(20) unsigned NOT NULL DEFAULT '0',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `subjectDNAndSerNumber` (`subjectDN`,`serNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CacheVolumeRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `backingVolumeUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `volumeUuid` (`volumeUuid`),
  CONSTRAINT `fkCacheVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CaptchaVO` (
  `uuid` varchar(32) NOT NULL,
  `captcha` text NOT NULL,
  `verifyCode` varchar(32) NOT NULL,
  `targetResourceIdentity` varchar(256) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CasClientVO` (
  `uuid` char(32) NOT NULL,
  `loginMNUrl` varchar(255) DEFAULT NULL,
  `redirectUrl` varchar(255) DEFAULT NULL,
  `casServerLoginUrl` varchar(255) NOT NULL,
  `casServerUrlPrefix` varchar(255) NOT NULL,
  `serverName` varchar(255) NOT NULL,
  `state` varchar(128) NOT NULL,
  `usernameProperty` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkCasClientVOThirdPartyAccountSourceVO` FOREIGN KEY (`uuid`) REFERENCES `ThirdPartyAccountSourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CbtTaskResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `taskUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxCbtTaskResourceRefVOtaskUuid` (`taskUuid`),
  KEY `idxCbtTaskResourceRefVOresourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CbtTaskVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CdpPolicyEO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `retentionTimePerDay` int(10) unsigned NOT NULL,
  `recoveryPointPerSecond` int(10) unsigned NOT NULL,
  `state` varchar(32) NOT NULL,
  `deleted` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `dailyRPSinceDay` int(10) unsigned DEFAULT '0',
  `expireTime` int(10) unsigned DEFAULT '0',
  `fullBackupInterval` int(10) unsigned DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CdpTaskResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `taskUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxCdpTaskResourceRefVOtaskUuid` (`taskUuid`),
  KEY `idxCdpTaskResourceRefVOresourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CdpTaskVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `taskType` varchar(32) NOT NULL,
  `policyUuid` varchar(32) NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `backupBandwidth` bigint(20) unsigned NOT NULL,
  `maxCapacity` bigint(20) unsigned NOT NULL,
  `usedCapacity` bigint(20) unsigned NOT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `maxLatency` bigint(20) unsigned DEFAULT '600000',
  `lastLatency` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxCdpTaskVOtaskType` (`taskType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CdpVolumeHistoryVO` (
  `volumeUuid` varchar(32) NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `lastVolumePath` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`volumeUuid`,`backupStorageUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephBackupStorageMonVO` (
  `uuid` varchar(32) NOT NULL,
  `sshUsername` varchar(64) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `monPort` int(10) unsigned NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `monAddr` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkCephBackupStorageMonVOBackupStorageEO` (`backupStorageUuid`),
  CONSTRAINT `fkCephBackupStorageMonVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `fsid` varchar(64) DEFAULT NULL,
  `poolName` varchar(255) NOT NULL,
  `poolAvailableCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `poolUsedCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `poolReplicatedSize` int(10) unsigned DEFAULT NULL,
  `poolSecurityPolicy` varchar(255) DEFAULT 'Copy',
  `poolDiskUtilization` float DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkCephBackupStorageVOBackupStorageEO` FOREIGN KEY (`uuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephCapacityVO` (
  `fsid` varchar(64) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`fsid`),
  UNIQUE KEY `fsid` (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephOsdGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `osds` text NOT NULL,
  `availableCapacity` bigint(20) DEFAULT NULL,
  `availablePhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `totalPhysicalCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `fkPrimaryStorageUuid` (`primaryStorageUuid`),
  CONSTRAINT `fkPrimaryStorageUuid` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephPrimaryStorageMonVO` (
  `uuid` varchar(32) NOT NULL,
  `sshUsername` varchar(64) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `monPort` int(10) unsigned NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `monAddr` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `hostname` (`hostname`,`primaryStorageUuid`),
  KEY `fkCephPrimaryStorageMonVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkCephPrimaryStorageMonVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephPrimaryStoragePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `poolName` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(32) NOT NULL DEFAULT 'Data',
  `aliasName` varchar(255) DEFAULT NULL,
  `availableCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `usedCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `replicatedSize` int(10) unsigned DEFAULT NULL,
  `totalCapacity` bigint(20) unsigned NOT NULL DEFAULT '0',
  `securityPolicy` varchar(255) DEFAULT 'Copy',
  `diskUtilization` float DEFAULT NULL,
  `osdGroupUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkCephPrimaryStoragePoolVOOsdGroupVO` (`osdGroupUuid`),
  CONSTRAINT `fkCephPrimaryStoragePoolVOOsdGroupVO` FOREIGN KEY (`osdGroupUuid`) REFERENCES `CephOsdGroupVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CephPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `fsid` varchar(64) DEFAULT NULL,
  `userKey` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkCephPrimaryStorageVOPrimaryStorageEO` FOREIGN KEY (`uuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CertificateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `certificate` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CloudFormationStackEventVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `stackUuid` varchar(32) NOT NULL,
  `action` varchar(64) NOT NULL,
  `resourceName` varchar(128) NOT NULL,
  `description` varchar(128) DEFAULT '1',
  `content` text NOT NULL,
  `actionStatus` varchar(16) NOT NULL,
  `duration` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkCloudFormationStackEventVOResourceStackVO` (`stackUuid`),
  CONSTRAINT `fkCloudFormationStackEventVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES `ResourceStackVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CloudFormationStackResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `stackUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `reserve` tinyint(1) NOT NULL DEFAULT '1',
  `round` int(10) unsigned DEFAULT NULL,
  `resourceName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkCloudFormationStackResourceRefVOResourceStackVO` (`stackUuid`),
  KEY `fkCloudFormationStackResourceRefVOResourceVO` (`resourceUuid`),
  CONSTRAINT `fkCloudFormationStackResourceRefVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES `ResourceStackVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkCloudFormationStackResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ClusterDRSVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `state` varchar(255) NOT NULL,
  `balancedState` varchar(255) NOT NULL,
  `lastAdviceGroupUuid` char(32) DEFAULT NULL,
  `automationLevel` varchar(255) NOT NULL,
  `thresholds` text NOT NULL,
  `thresholdDuration` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `clusterUuid` (`clusterUuid`),
  CONSTRAINT `fkClusterDRSVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ClusterEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'cluster uuid',
  `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
  `name` varchar(255) NOT NULL COMMENT 'cluster name',
  `type` varchar(255) NOT NULL COMMENT 'cluster name',
  `managementNodeId` varchar(128) DEFAULT NULL COMMENT 'management node id',
  `state` varchar(32) NOT NULL COMMENT 'cluster state',
  `hypervisorType` varchar(64) NOT NULL COMMENT 'hypervisor type',
  `description` varchar(2048) DEFAULT NULL COMMENT 'cluster description',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `architecture` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkClusterEOZoneEO` (`zoneUuid`),
  KEY `idxClusterEOname` (`name`),
  CONSTRAINT `fkClusterEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ConnectionAccessPointVO` (
  `uuid` varchar(32) NOT NULL,
  `accessPointId` varchar(64) NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `hostOperator` varchar(32) NOT NULL,
  `description` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkConnectionAccessPointVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkConnectionAccessPointVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ConnectionRelationShipVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `relationShips` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ConsoleProxyAgentVO` (
  `uuid` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `managementIp` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `consoleProxyOverriddenIp` varchar(255) NOT NULL,
  `consoleProxyPort` int(11) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkConsoleProxyAgentVOManagementNodeVO` FOREIGN KEY (`uuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ConsoleProxyVO` (
  `uuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `agentIp` varchar(32) NOT NULL,
  `proxyHostname` varchar(128) NOT NULL,
  `proxyPort` int(11) NOT NULL,
  `targetHostname` varchar(128) NOT NULL,
  `targetPort` int(11) NOT NULL,
  `status` varchar(32) NOT NULL,
  `scheme` varchar(32) DEFAULT 'http',
  `proxyIdentity` varchar(255) DEFAULT NULL,
  `agentType` varchar(128) NOT NULL,
  `token` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `targetSchema` varchar(32) NOT NULL DEFAULT 'vnc',
  `version` varchar(32) DEFAULT NULL,
  `expiredDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkConsoleProxyVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkConsoleProxyVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CpuFeaturesHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `srcHostUuid` varchar(32) NOT NULL,
  `dstHostUuid` varchar(32) NOT NULL,
  `srcCpuModelName` varchar(64) DEFAULT NULL,
  `supportLiveMigration` tinyint(1) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `CpuFeaturesHistoryVOHostVO` (`srcHostUuid`),
  CONSTRAINT `CpuFeaturesHistoryVOHostVO` FOREIGN KEY (`srcHostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CustomPreconfigurationVO` (
  `uuid` varchar(32) NOT NULL,
  `baremetalInstanceUuid` varchar(32) NOT NULL,
  `param` varchar(255) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkCustomPreconfigurationVOBaremetalInstanceVO` (`baremetalInstanceUuid`),
  CONSTRAINT `fkCustomPreconfigurationVOBaremetalInstanceVO` FOREIGN KEY (`baremetalInstanceUuid`) REFERENCES `BaremetalInstanceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DRSAdviceVO` (
  `uuid` varchar(32) NOT NULL,
  `drsUuid` varchar(32) NOT NULL,
  `adviceGroupUuid` varchar(32) NOT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmSourceHostUuid` varchar(32) NOT NULL,
  `vmTargetHostUuid` varchar(32) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `drsUuid` (`drsUuid`),
  KEY `adviceGroupUuid` (`adviceGroupUuid`),
  CONSTRAINT `fkDRSAdviceVOClusterDRSVO` FOREIGN KEY (`drsUuid`) REFERENCES `ClusterDRSVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DRSVmMigrationActivityVO` (
  `uuid` varchar(32) NOT NULL,
  `drsUuid` varchar(32) DEFAULT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmSourceHostUuid` varchar(32) NOT NULL,
  `vmTargetHostUuid` varchar(32) NOT NULL,
  `status` varchar(255) NOT NULL,
  `endDate` datetime DEFAULT NULL,
  `adviceUuid` varchar(32) DEFAULT NULL,
  `reason` varchar(255) NOT NULL,
  `cause` varchar(64) NOT NULL,
  `result` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `vmUuid` (`vmUuid`),
  KEY `drsUuid` (`drsUuid`),
  KEY `adviceUuid` (`adviceUuid`),
  CONSTRAINT `fkDRSVmMigrationActivityVOClusterDRSVO` FOREIGN KEY (`drsUuid`) REFERENCES `ClusterDRSVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataCenterVO` (
  `uuid` varchar(32) NOT NULL,
  `deleted` varchar(1) DEFAULT NULL,
  `regionName` varchar(1024) NOT NULL,
  `dcType` varchar(32) NOT NULL,
  `regionId` varchar(64) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `endpoint` varchar(127) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeUsageExtensionVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `fkDataVolumeUsageExtensionVODataVolumeUsageVO` FOREIGN KEY (`id`) REFERENCES `DataVolumeUsageVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxDataVolumeUsageVOvolumeUuid` (`accountUuid`,`dateInLong`,`volumeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DatabaseBackupStorageRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `backupStorageUuid` varchar(32) NOT NULL,
  `databaseBackupUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `installPath` varchar(2048) NOT NULL,
  `exportUrl` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkDatabaseBackupStorageRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkDatabaseBackupStorageRefVODatabaseBackupVO` (`databaseBackupUuid`),
  CONSTRAINT `fkDatabaseBackupStorageRefVODatabaseBackupVO` FOREIGN KEY (`databaseBackupUuid`) REFERENCES `DatabaseBackupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkDatabaseBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DatabaseBackupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `state` varchar(64) NOT NULL,
  `status` varchar(64) NOT NULL,
  `metadata` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DeleteVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `voName` varchar(255) NOT NULL,
  `uuid` varchar(32) NOT NULL,
  `foreignVOToDeleteName` varchar(255) DEFAULT NULL,
  `foreignVOToDeleteUuid` varchar(32) DEFAULT NULL,
  `foreignVOName` varchar(255) DEFAULT NULL,
  `foreignVOUuid` varchar(32) DEFAULT NULL,
  `deletedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DirectoryVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `groupName` varchar(2048) NOT NULL COMMENT 'equivalent to a path',
  `parentUuid` varchar(32) DEFAULT NULL,
  `rootDirectoryUuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkDirectoryVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkDirectoryVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DiskOfferingEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) NOT NULL COMMENT 'disk offering name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'disk offering description',
  `diskSize` bigint(20) unsigned NOT NULL COMMENT 'disk size in bytes',
  `sortKey` int(10) unsigned DEFAULT '0' COMMENT 'sort key',
  `state` varchar(32) NOT NULL,
  `type` varchar(255) NOT NULL,
  `allocatorStrategy` varchar(64) DEFAULT NULL COMMENT 'allocator strategy deciding which allocator chain to use',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxDiskOfferingEOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ESXHostVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
  `morval` varchar(128) NOT NULL COMMENT 'MOR value',
  `esxiVersion` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkESXHostVOVCenterVO` (`vCenterUuid`),
  CONSTRAINT `fkESXHostVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkESXHostVOVCenterVO` FOREIGN KEY (`vCenterUuid`) REFERENCES `VCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsImageUsageVO` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `ecsImageUuid` varchar(32) NOT NULL,
  `snapshotUuidOfCreatedImage` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkEcsImageUsageVOEcsImageVO` (`ecsImageUuid`),
  KEY `fkEcsImageUsageVOAliyunSnapshotVO` (`snapshotUuidOfCreatedImage`),
  CONSTRAINT `fkEcsImageUsageVOEcsImageVO` FOREIGN KEY (`ecsImageUuid`) REFERENCES `EcsImageVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkEcsImageUsageVOAliyunSnapshotVO` FOREIGN KEY (`snapshotUuidOfCreatedImage`) REFERENCES `AliyunSnapshotVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsImageVO` (
  `uuid` varchar(32) NOT NULL,
  `localImageUuid` varchar(32) DEFAULT NULL,
  `ecsImageId` varchar(128) NOT NULL,
  `dataCenterUuid` varchar(32) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `ecsImageSize` bigint(20) NOT NULL,
  `platform` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `ossMd5Sum` varchar(128) DEFAULT NULL,
  `format` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `osName` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEcsImageVOImageEO` (`localImageUuid`),
  KEY `fkEcsImageVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkEcsImageVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`),
  CONSTRAINT `fkEcsImageVOImageEO` FOREIGN KEY (`localImageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `localVmInstanceUuid` varchar(32) DEFAULT NULL,
  `ecsInstanceId` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `ecsStatus` varchar(16) NOT NULL,
  `ecsInstanceRootPassword` varchar(32) NOT NULL,
  `cpuCores` int(10) NOT NULL,
  `memorySize` bigint(20) NOT NULL,
  `ecsInstanceType` varchar(32) NOT NULL,
  `ecsBandWidth` bigint(20) NOT NULL,
  `ecsRootVolumeId` varchar(32) NOT NULL,
  `ecsRootVolumeCategory` varchar(32) NOT NULL,
  `ecsRootVolumeSize` bigint(20) NOT NULL,
  `privateIpAddress` varchar(32) NOT NULL,
  `ecsVSwitchUuid` varchar(32) NOT NULL,
  `ecsImageUuid` varchar(32) DEFAULT NULL,
  `ecsSecurityGroupUuid` varchar(32) NOT NULL,
  `identityZoneUuid` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `expireDate` datetime DEFAULT '2999-01-01 00:00:00',
  `chargeType` varchar(16) DEFAULT 'postpaid',
  `publicIpAddress` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEcsInstanceVOEcsImageVO` (`ecsImageUuid`),
  KEY `fkEcsInstanceVOEcsSecurityGroupVO` (`ecsSecurityGroupUuid`),
  KEY `fkEcsInstanceVOEcsVSwitchVO` (`ecsVSwitchUuid`),
  KEY `fkEcsInstanceVOIdentityZoneVO` (`identityZoneUuid`),
  KEY `fkEcsInstanceVOVmInstanceEO` (`localVmInstanceUuid`),
  CONSTRAINT `fkEcsInstanceVOEcsImageVO` FOREIGN KEY (`ecsImageUuid`) REFERENCES `EcsImageVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkEcsInstanceVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `EcsSecurityGroupVO` (`uuid`),
  CONSTRAINT `fkEcsInstanceVOEcsVSwitchVO` FOREIGN KEY (`ecsVSwitchUuid`) REFERENCES `EcsVSwitchVO` (`uuid`),
  CONSTRAINT `fkEcsInstanceVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`),
  CONSTRAINT `fkEcsInstanceVOVmInstanceEO` FOREIGN KEY (`localVmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsSecurityGroupRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `ecsSecurityGroupUuid` varchar(32) NOT NULL,
  `portRange` varchar(32) NOT NULL,
  `cidrIp` varchar(32) NOT NULL,
  `protocol` varchar(32) NOT NULL,
  `nicType` varchar(32) NOT NULL,
  `policy` varchar(32) NOT NULL,
  `direction` varchar(128) NOT NULL,
  `priority` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEcsSecurityGroupRuleVOEcsSecurityGroupVO` (`ecsSecurityGroupUuid`),
  CONSTRAINT `fkEcsSecurityGroupRuleVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `EcsSecurityGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsSecurityGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `ecsVpcUuid` varchar(32) NOT NULL,
  `securityGroupId` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukEcsVpcUuidSecurityGroupId` (`ecsVpcUuid`,`securityGroupId`) USING BTREE,
  CONSTRAINT `fkEcsSecurityGroupVOEcsVpcVO` FOREIGN KEY (`ecsVpcUuid`) REFERENCES `EcsVpcVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsVSwitchVO` (
  `uuid` varchar(32) NOT NULL,
  `vSwitchId` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `cidrBlock` varchar(32) NOT NULL,
  `availableIpAddressCount` int(10) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `ecsVpcUuid` varchar(32) NOT NULL,
  `identityZoneUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEcsVSwitchVOEcsVpcVO` (`ecsVpcUuid`),
  KEY `fkEcsVSwitchVOIdentityZoneVO` (`identityZoneUuid`),
  CONSTRAINT `fkEcsVSwitchVOEcsVpcVO` FOREIGN KEY (`ecsVpcUuid`) REFERENCES `EcsVpcVO` (`uuid`),
  CONSTRAINT `fkEcsVSwitchVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EcsVpcVO` (
  `uuid` varchar(32) NOT NULL,
  `ecsVpcId` varchar(32) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `deleted` varchar(1) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `cidrBlock` varchar(32) NOT NULL,
  `vRouterId` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEcsVpcVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkEcsVpcVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EipVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `vipIp` varchar(128) NOT NULL,
  `state` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) DEFAULT NULL,
  `guestIp` varchar(128) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEipVOVipVO` (`vipUuid`),
  KEY `fkEipVOVmNicVO` (`vmNicUuid`),
  KEY `idxEipVOname` (`name`),
  CONSTRAINT `fkEipVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkEipVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EmailMediaVO` (
  `uuid` varchar(32) NOT NULL,
  `smtpServer` varchar(512) NOT NULL,
  `smtpPort` int(10) unsigned NOT NULL,
  `username` varchar(512) DEFAULT NULL,
  `password` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkEmailMediaVOMediaVO` FOREIGN KEY (`uuid`) REFERENCES `MediaVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EmailTriggerActionVO` (
  `uuid` varchar(32) NOT NULL,
  `email` varchar(512) NOT NULL,
  `mediaUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEmailTriggerActionVOEmailMediaVO` (`mediaUuid`),
  CONSTRAINT `fkEmailTriggerActionVOEmailMediaVO` FOREIGN KEY (`mediaUuid`) REFERENCES `EmailMediaVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkEmailTriggerActionVOMonitorTriggerActionVO` FOREIGN KEY (`uuid`) REFERENCES `MonitorTriggerActionVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EncryptEntityMetadataVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `entityName` varchar(255) NOT NULL,
  `columnName` varchar(255) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EncryptedResourceKeyRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceType` varchar(255) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `providerUuid` varchar(32) DEFAULT NULL,
  `providerName` varchar(255) NOT NULL,
  `keyVersion` int(10) unsigned DEFAULT NULL,
  `kekRef` varchar(255) DEFAULT NULL,
  `wrappedDek` text NOT NULL,
  `algorithm` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxEncryptedResourceKeyRefVOResource` (`resourceType`,`resourceUuid`),
  KEY `idxEncryptedResourceKeyRefVOProviderUuid` (`providerUuid`),
  KEY `idxEncryptedResourceKeyRefVOProviderName` (`providerName`),
  CONSTRAINT `fkEncryptedResourceKeyRefVOProviderUuid` FOREIGN KEY (`providerUuid`) REFERENCES `KeyProviderVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EncryptionIntegrityVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(64) NOT NULL,
  `signedText` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `resource` (`resourceUuid`,`resourceType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EthernetVfPciDeviceVO` (
  `uuid` varchar(32) NOT NULL,
  `hostDevUuid` varchar(32) DEFAULT NULL,
  `interfaceName` varchar(32) DEFAULT NULL,
  `vmUuid` varchar(32) DEFAULT NULL,
  `l3NetworkUuid` varchar(32) DEFAULT NULL,
  `vfStatus` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEthernetVfPciDeviceVOVmInstanceEO` (`vmUuid`),
  KEY `fkEthernetVfPciDeviceVOHostEO` (`hostDevUuid`),
  KEY `fkEthernetVfPciDeviceVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkEthernetVfPciDeviceVOVmInstanceEO` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkEthernetVfPciDeviceVOHostEO` FOREIGN KEY (`hostDevUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkEthernetVfPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkEthernetVfPciDeviceVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventDataAckVO` (
  `alertDataUuid` varchar(32) NOT NULL,
  `eventSubscriptionUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`alertDataUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventLogVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `type` varchar(32) NOT NULL,
  `category` varchar(32) NOT NULL,
  `trackingId` varchar(32) DEFAULT NULL,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `resourceType` varchar(255) DEFAULT NULL,
  `time` bigint(20) unsigned NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxEventLogVOResourceUuid` (`resourceUuid`),
  KEY `idxEventLogVOCategory` (`category`),
  KEY `idxEventLogVOTime` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventRecordsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `accountUuid` varchar(32) DEFAULT NULL,
  `dataUuid` varchar(32) DEFAULT NULL,
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `name` varchar(256) DEFAULT NULL,
  `error` text,
  `labels` text,
  `namespace` varchar(256) DEFAULT NULL,
  `readStatus` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `operatorAccountUuid` char(32) DEFAULT NULL,
  `resourceId` varchar(32) DEFAULT NULL,
  `resourceName` varchar(256) DEFAULT NULL,
  `subscriptionUuid` varchar(32) DEFAULT NULL,
  `hour` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataUuid` (`dataUuid`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxAccountUuid` (`accountUuid`),
  KEY `idxAccountUuidCreateTime` (`accountUuid`,`createTime`),
  KEY `idxName` (`name`(255)),
  KEY `idxSubscriptionUuid` (`subscriptionUuid`),
  KEY `idxAccountUuidHourEmergencyLevel` (`accountUuid`,`hour`,`emergencyLevel`),
  KEY `idxCreateTimeReadStatusEmergencyLevel` (`createTime`,`emergencyLevel`,`readStatus`,`accountUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventRuleTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `monitorTemplateUuid` varchar(32) NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `eventName` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `monitorTemplateUuid` (`monitorTemplateUuid`),
  CONSTRAINT `fkEventRuleTemplateVOMonitorTemplateVO` FOREIGN KEY (`monitorTemplateUuid`) REFERENCES `MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventSubscriptionActionVO` (
  `subscriptionUuid` varchar(32) NOT NULL,
  `actionUuid` varchar(32) NOT NULL,
  `actionType` varchar(128) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '2018-05-10 06:04:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '2018-05-10 06:04:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`subscriptionUuid`,`actionUuid`),
  CONSTRAINT `fkEventSubscriptionActionVOEventSubscriptionVO` FOREIGN KEY (`subscriptionUuid`) REFERENCES `EventSubscriptionVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventSubscriptionLabelVO` (
  `uuid` varchar(32) NOT NULL,
  `key` varchar(1024) NOT NULL,
  `value` text NOT NULL,
  `operator` varchar(128) NOT NULL,
  `subscriptionUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkEventSubscriptionLabelVOEventSubscriptionVO` (`subscriptionUuid`),
  CONSTRAINT `fkEventSubscriptionLabelVOEventSubscriptionVO` FOREIGN KEY (`subscriptionUuid`) REFERENCES `EventSubscriptionVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EventSubscriptionVO` (
  `uuid` varchar(32) NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `eventName` varchar(255) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `name` varchar(255) DEFAULT NULL,
  `emergencyLevel` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ExponBlockVolumeVO` (
  `uuid` varchar(32) NOT NULL,
  `exponStatus` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkExponBlockVolumeVOBlockVolumeVO` FOREIGN KEY (`uuid`) REFERENCES `BlockVolumeVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ExternalBackupMetadataVO` (
  `uuid` varchar(32) NOT NULL,
  `metadata` text,
  PRIMARY KEY (`uuid`),
  CONSTRAINT `fkExternalBackupMetadataVOExternalBackupVO` FOREIGN KEY (`uuid`) REFERENCES `ExternalBackupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ExternalBackupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `installPath` varchar(2048) DEFAULT NULL,
  `totalSize` bigint(20) unsigned DEFAULT NULL,
  `version` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ExternalPrimaryStorageHostRefVO` (
  `id` bigint(20) unsigned NOT NULL,
  `hostId` int(11) DEFAULT NULL,
  `protocol` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ExternalPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `identity` varchar(32) NOT NULL,
  `config` text,
  `password` varchar(255) DEFAULT NULL,
  `addonInfo` varchar(2048) DEFAULT NULL,
  `defaultProtocol` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FaultToleranceVmGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `primaryVmInstanceUuid` varchar(32) DEFAULT NULL,
  `secondaryVmInstanceUuid` varchar(32) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FaultToleranceVmInstanceGroupHostPortRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `nbdServerPortId` bigint(20) unsigned NOT NULL,
  `blockReplicationPortId` bigint(20) unsigned NOT NULL,
  `primaryVmMonitorPortId` bigint(20) unsigned NOT NULL,
  `secondaryVmMonitorPortId` bigint(20) unsigned NOT NULL,
  `reservedVmMigrationPortId` bigint(20) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `vmInstanceUuid` (`vmInstanceUuid`),
  CONSTRAINT `fkShadowVmInstanceHostPortRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FcHbaDeviceVO` (
  `uuid` varchar(32) NOT NULL,
  `portName` varchar(255) DEFAULT NULL,
  `portState` varchar(64) DEFAULT NULL,
  `supportedSpeeds` varchar(255) DEFAULT NULL,
  `speed` varchar(255) DEFAULT NULL,
  `symbolicName` varchar(255) DEFAULT NULL,
  `supportedClasses` varchar(255) DEFAULT NULL,
  `nodeName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkFcHbaDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `HbaDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FiberChannelLunVO` (
  `uuid` varchar(32) NOT NULL,
  `fiberChannelStorageUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkFiberChannelLunVOFiberChannelStorageVO` (`fiberChannelStorageUuid`),
  CONSTRAINT `fkFiberChannelLunVOFiberChannelStorageVO` FOREIGN KEY (`fiberChannelStorageUuid`) REFERENCES `FiberChannelStorageVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FiberChannelStorageVO` (
  `name` varchar(256) DEFAULT NULL,
  `uuid` varchar(32) NOT NULL,
  `wwnn` varchar(256) NOT NULL,
  `state` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FileIntegrityVerificationVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `path` varchar(256) NOT NULL,
  `nodeType` varchar(16) NOT NULL,
  `nodeUuid` varchar(64) NOT NULL,
  `hexType` varchar(16) NOT NULL,
  `digest` varchar(256) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `node` (`nodeUuid`,`nodeType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlkSecSecretResourcePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `encryptResult` varchar(64) DEFAULT NULL,
  `activatedToken` varchar(32) DEFAULT NULL,
  `protectToken` varchar(32) DEFAULT NULL,
  `hmacToken` varchar(32) DEFAULT NULL,
  `ukeyType` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkFlkSecSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlkSecSecurityMachineVO` (
  `uuid` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkFlkSecSecurityMachineVOSecurityMachineVO` FOREIGN KEY (`uuid`) REFERENCES `SecurityMachineVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlowCollectorVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'flow collector uuid',
  `flowMeterUuid` varchar(32) NOT NULL,
  `name` varchar(32) DEFAULT '',
  `description` varchar(128) DEFAULT '',
  `server` varchar(64) NOT NULL,
  `port` varchar(16) DEFAULT '2055',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkFlowCollectorVOFlowMeterVO` (`flowMeterUuid`),
  CONSTRAINT `fkFlowCollectorVOFlowMeterVO` FOREIGN KEY (`flowMeterUuid`) REFERENCES `FlowMeterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlowMeterVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'flow meter uuid',
  `name` varchar(32) DEFAULT '',
  `description` varchar(128) DEFAULT '',
  `version` varchar(16) DEFAULT 'V5',
  `type` varchar(16) DEFAULT 'NetFlow',
  `sample` int(10) unsigned DEFAULT '1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FlowRouterVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'logic flow router uuid for vrouterHA',
  `systemID` int(10) unsigned DEFAULT '0',
  `type` varchar(16) NOT NULL DEFAULT 'normal' COMMENT 'router ha type',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FusionstorBackupStorageMonVO` (
  `uuid` varchar(32) NOT NULL,
  `sshUsername` varchar(64) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `monPort` int(10) unsigned NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkFusionstorBackupStorageMonVOBackupStorageEO` (`backupStorageUuid`),
  CONSTRAINT `fkFusionstorBackupStorageMonVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FusionstorBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `fsid` varchar(64) DEFAULT NULL,
  `poolName` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkFusionstorBackupStorageVOBackupStorageEO` FOREIGN KEY (`uuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FusionstorCapacityVO` (
  `fsid` varchar(64) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`fsid`),
  UNIQUE KEY `fsid` (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FusionstorPrimaryStorageMonVO` (
  `uuid` varchar(32) NOT NULL,
  `sshUsername` varchar(64) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  `monPort` int(10) unsigned NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkFusionstorPrimaryStorageMonVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkFusionstorPrimaryStorageMonVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FusionstorPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `fsid` varchar(64) DEFAULT NULL,
  `rootVolumePoolName` varchar(255) NOT NULL,
  `dataVolumePoolName` varchar(255) NOT NULL,
  `imageCachePoolName` varchar(255) NOT NULL,
  `userKey` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkFusionstorPrimaryStorageVOPrimaryStorageEO` FOREIGN KEY (`uuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GarbageCollectorVO` (
  `runnerClass` varchar(512) NOT NULL,
  `context` text NOT NULL,
  `status` varchar(64) NOT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(1024) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkGarbageCollectorVOManagementNodeVO` (`managementNodeUuid`),
  KEY `idxName` (`name`(255)),
  KEY `idxStatus` (`status`),
  CONSTRAINT `fkGarbageCollectorVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GlobalConfigTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GlobalConfigVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `defaultValue` text,
  `value` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GpuDeviceVO` (
  `uuid` varchar(32) NOT NULL,
  `serialNumber` varchar(255) DEFAULT NULL,
  `memory` bigint(20) unsigned DEFAULT '0',
  `power` bigint(20) unsigned DEFAULT '0',
  `isDriverLoaded` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GuestOsCategoryVO` (
  `uuid` varchar(32) NOT NULL,
  `platform` varchar(32) NOT NULL,
  `name` varchar(32) NOT NULL,
  `version` varchar(32) DEFAULT NULL,
  `osRelease` varchar(64) NOT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GuestToolsStateVO` (
  `vmInstanceUuid` varchar(32) NOT NULL,
  `qgaState` varchar(32) NOT NULL DEFAULT 'NotInstalled',
  `version` varchar(32) DEFAULT NULL,
  `platform` varchar(32) DEFAULT NULL,
  `osType` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `zwatchState` varchar(32) NOT NULL DEFAULT 'NotInstalled',
  PRIMARY KEY (`vmInstanceUuid`),
  UNIQUE KEY `vmInstanceUuid` (`vmInstanceUuid`),
  CONSTRAINT `fkGuestToolsStateVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `GuestToolsVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT '',
  `description` varchar(2048) DEFAULT NULL,
  `managementNodeUuid` varchar(32) NOT NULL,
  `architecture` varchar(32) NOT NULL,
  `hypervisorType` varchar(32) NOT NULL,
  `version` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `agentType` varchar(64) NOT NULL DEFAULT 'WindowsOnKvm',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HaStrategyConditionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) DEFAULT NULL,
  `fencerName` varchar(256) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `HaStrategyConditionVO` VALUE
('d3bed9568c4011f19babfa4856ea8e00','ha strategy condition','hostBusinessNic','Disable','2026-07-30 18:02:28','2026-07-30 18:02:28');

CREATE TABLE `HaiTaiSecretResourcePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `managementIp` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `realm` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkHaiTaiSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HardwareL2VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL,
  `sdnControllerUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkHardwareL2VxlanNetworkPoolVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HbaDeviceVO` (
  `uuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `hbaType` varchar(64) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHBADeviceVOHostVO` (`hostUuid`),
  CONSTRAINT `fkHBADeviceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HistoricalPasswordVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(32) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostAllocatedCpuVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `allocatedCPU` smallint(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `HostAllocatedCpuVO_UniqueIndex_HostUuid_CPUID` (`hostUuid`,`allocatedCPU`),
  CONSTRAINT `HostAllocatedCpuVO_HostEO_uuid_fk` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostCapacityVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `totalMemory` bigint(20) unsigned NOT NULL COMMENT 'total memory of host in bytes',
  `totalCpu` bigint(20) unsigned NOT NULL COMMENT 'total cpu of host in HZ',
  `availableMemory` bigint(20) NOT NULL DEFAULT '0',
  `availableCpu` bigint(20) NOT NULL COMMENT 'used cpu of host in HZ',
  `totalPhysicalMemory` bigint(20) unsigned NOT NULL DEFAULT '0',
  `availablePhysicalMemory` bigint(20) unsigned NOT NULL DEFAULT '0',
  `cpuNum` int(10) unsigned NOT NULL DEFAULT '0',
  `cpuSockets` int(10) unsigned NOT NULL,
  `cpuCoreNum` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxHostCapacityVOtotalMemory` (`totalMemory`),
  KEY `idxHostCapacityVOtotalCpu` (`totalCpu`),
  KEY `idxHostCapacityVOavailableMemory` (`availableMemory`),
  KEY `idxHostCapacityVOavailableCpu` (`availableCpu`),
  KEY `idxHostCapacityVOcpuNum` (`cpuNum`),
  CONSTRAINT `fkHostCapacityVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
  `clusterUuid` varchar(32) NOT NULL COMMENT 'cluster uuid',
  `name` varchar(255) NOT NULL COMMENT 'host name',
  `state` varchar(32) NOT NULL COMMENT 'host state',
  `status` varchar(32) NOT NULL COMMENT 'host connection status',
  `hypervisorType` varchar(64) NOT NULL COMMENT 'hypervisor type',
  `managementIp` varchar(255) NOT NULL COMMENT 'ip of managment nic',
  `nqn` varchar(256) DEFAULT NULL,
  `hostname` varchar(256) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL COMMENT 'host description',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `architecture` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxHostEOuuid` (`uuid`),
  KEY `fkHostEOClusterEO` (`clusterUuid`),
  KEY `fkHostEOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkHostEOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`),
  CONSTRAINT `fkHostEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostHaStateVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `state` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostHwMonitorStatusVO` (
  `uuid` varchar(32) NOT NULL,
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
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkHostHwMonitorStatusVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostIpmiVO` (
  `uuid` varchar(32) NOT NULL,
  `ipmiAddress` varchar(32) DEFAULT NULL,
  `ipmiPort` int(10) unsigned DEFAULT NULL,
  `ipmiUsername` varchar(255) DEFAULT NULL,
  `ipmiPassword` varchar(255) DEFAULT NULL,
  `ipmiPowerStatus` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkHostIpmiVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostKernelInterfaceTrafficTypeVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostKernelInterfaceUuid` varchar(32) NOT NULL,
  `trafficType` varchar(128) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkHostKernelInterfaceTrafficTypeVOHostKernelInterfaceVO` (`hostKernelInterfaceUuid`),
  CONSTRAINT `fkHostKernelInterfaceTrafficTypeVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES `HostKernelInterfaceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostKernelInterfaceUsedIpVO` (
  `uuid` varchar(32) NOT NULL,
  `hostKernelInterfaceUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHostKernelInterfaceUsedIpVOHostKernelInterfaceVO` (`hostKernelInterfaceUuid`),
  CONSTRAINT `fkHostKernelInterfaceUsedIpVOUsedIpVO` FOREIGN KEY (`uuid`) REFERENCES `UsedIpVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostKernelInterfaceUsedIpVOHostKernelInterfaceVO` FOREIGN KEY (`hostKernelInterfaceUuid`) REFERENCES `HostKernelInterfaceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostKernelInterfaceVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `l2NetworkUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHostKernelInterfaceVOHostVO` (`hostUuid`),
  KEY `fkHostKernelInterfaceVOL2NetworkVO` (`l2NetworkUuid`),
  KEY `fkHostKernelInterfaceVOL3NetworkVO` (`l3NetworkUuid`),
  CONSTRAINT `fkHostKernelInterfaceVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostKernelInterfaceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostKernelInterfaceVOL2NetworkVO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostKeyIdentityVO` (
  `hostUuid` varchar(32) NOT NULL,
  `publicKey` text NOT NULL,
  `fingerprint` varchar(128) NOT NULL,
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`hostUuid`),
  UNIQUE KEY `hostUuid` (`hostUuid`),
  CONSTRAINT `fkHostKeyIdentityVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkBondingServiceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `bondingUuid` varchar(32) NOT NULL,
  `vlanId` int(32) NOT NULL DEFAULT '0',
  `serviceType` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkHostNetworkBodnuingServiceRefVOHostNetworkBondingVO` (`bondingUuid`),
  CONSTRAINT `fkHostNetworkBodnuingServiceRefVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES `HostNetworkBondingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkBondingVO` (
  `uuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `bondingName` varchar(128) NOT NULL,
  `mode` varchar(32) NOT NULL,
  `xmitHashPolicy` varchar(32) DEFAULT NULL,
  `miiStatus` varchar(32) DEFAULT NULL,
  `miimon` bigint(20) unsigned DEFAULT NULL,
  `mac` varchar(17) DEFAULT NULL,
  `ipAddresses` varchar(255) DEFAULT NULL,
  `allSlavesActive` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` char(32) DEFAULT 'unknown',
  `speed` bigint(20) unsigned DEFAULT NULL,
  `bondingType` varchar(32) DEFAULT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `callBackIp` varchar(128) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxHostNetworkBondingVOhostUuid` (`hostUuid`),
  CONSTRAINT `fkHostNetworkBondingVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkInterfaceLldpRefVO` (
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
  `aggregationPortId` bigint(20) unsigned DEFAULT NULL,
  `mtu` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`lldpUuid`),
  CONSTRAINT `fkHostNetworkInterfaceLldpRefVOHostNetworkInterfaceLldpVO` FOREIGN KEY (`lldpUuid`) REFERENCES `HostNetworkInterfaceLldpVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkInterfaceLldpVO` (
  `uuid` varchar(32) NOT NULL,
  `interfaceUuid` varchar(32) NOT NULL,
  `mode` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `interfaceUuid` (`interfaceUuid`),
  CONSTRAINT `fkHostNetworkInterfaceLldpVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES `HostNetworkInterfaceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkInterfaceServiceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `interfaceUuid` varchar(32) NOT NULL,
  `vlanId` int(32) NOT NULL DEFAULT '0',
  `serviceType` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkHostNetworkInterfaceServiceRefVOHostNetworkInterfaceVO` (`interfaceUuid`),
  CONSTRAINT `fkHostNetworkInterfaceServiceRefVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES `HostNetworkInterfaceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNetworkInterfaceVO` (
  `uuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `bondingUuid` varchar(32) DEFAULT NULL,
  `interfaceName` varchar(32) NOT NULL,
  `interfaceType` varchar(32) NOT NULL,
  `mac` varchar(128) DEFAULT NULL,
  `speed` bigint(20) unsigned DEFAULT NULL,
  `ipAddresses` varchar(255) DEFAULT NULL,
  `pciDeviceAddress` varchar(32) DEFAULT NULL,
  `slaveActive` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `carrierActive` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `offloadStatus` varchar(128) DEFAULT NULL,
  `virtStatus` varchar(32) DEFAULT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `callBackIp` varchar(128) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `interfaceModel` varchar(255) DEFAULT NULL,
  `vendorId` varchar(64) DEFAULT NULL,
  `deviceId` varchar(64) DEFAULT NULL,
  `deviceName` varchar(255) DEFAULT NULL,
  `vendorName` varchar(255) DEFAULT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `subvendorName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxHostNetworkInterfaceVOhostUuid` (`hostUuid`),
  KEY `idxHostNetworkInterfaceVObondingUuid` (`bondingUuid`),
  CONSTRAINT `fkHostNetworkInterfaceVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostNetworkInterfaceVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES `HostNetworkBondingVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostNumaNodeVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `nodeID` int(11) NOT NULL,
  `nodeCPUs` text NOT NULL,
  `nodeMemSize` bigint(20) NOT NULL,
  `nodeDistance` varchar(512) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `HostNumaNodeVO_HostEO_uuid_fk` (`hostUuid`),
  CONSTRAINT `HostNumaNodeVO_HostEO_uuid_fk` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostOsCategoryVO` (
  `uuid` char(32) NOT NULL COMMENT 'uuid',
  `architecture` varchar(32) NOT NULL,
  `osReleaseVersion` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostPhysicalCpuVO` (
  `uuid` char(32) NOT NULL,
  `socketDesignation` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `serialNumber` varchar(255) NOT NULL,
  `currentSpeed` varchar(32) DEFAULT NULL,
  `coreCount` varchar(32) DEFAULT NULL,
  `threadCount` varchar(32) DEFAULT NULL,
  `hostUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHostPhysicalCpuVOHostVO` (`hostUuid`),
  CONSTRAINT `fkHostPhysicalCpuVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostPhysicalMemoryVO` (
  `uuid` varchar(32) NOT NULL,
  `manufacturer` varchar(255) DEFAULT NULL,
  `size` varchar(32) DEFAULT NULL,
  `locator` varchar(255) DEFAULT NULL,
  `serialNumber` varchar(255) NOT NULL,
  `speed` varchar(32) DEFAULT NULL,
  `clockSpeed` varchar(32) DEFAULT NULL,
  `rank` varchar(32) DEFAULT NULL,
  `voltage` varchar(32) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHostPhysicalMemoryVOHostVO` (`hostUuid`),
  CONSTRAINT `fkHostPhysicalMemoryVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostPortVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `port` int(10) unsigned DEFAULT NULL,
  `portUsage` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `hostUuid` (`hostUuid`),
  CONSTRAINT `fkHostPortVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostSchedulingRuleGroupRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostGroupUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `hostGroupUuid_hostUuid` (`hostGroupUuid`,`hostUuid`) USING BTREE,
  KEY `fkHostVORefVO` (`hostUuid`),
  CONSTRAINT `fkHostSchedulingRuleGroupRefVO` FOREIGN KEY (`hostGroupUuid`) REFERENCES `HostSchedulingRuleGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostVORefVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostSchedulingRuleGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HostTagVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `tag` varchar(128) NOT NULL COMMENT 'host tag',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridAccountVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `akey` varchar(32) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `current` varchar(64) NOT NULL DEFAULT 'false',
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `hybridAccountId` varchar(32) DEFAULT NULL,
  `hybridUserId` varchar(32) DEFAULT NULL,
  `hybridUserName` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uniqAccountUuid` (`accountUuid`,`akey`,`type`),
  KEY `fkHybridAccountVOUserVO` (`userUuid`),
  CONSTRAINT `fkHybridAccountVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridConnectionRefVO` (
  `uuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `connectionType` varchar(32) NOT NULL,
  `connectionUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHybridConnectionRefVOConnectionRelationShipVO` (`connectionUuid`),
  CONSTRAINT `fkHybridConnectionRefVOConnectionRelationShipVO` FOREIGN KEY (`connectionUuid`) REFERENCES `ConnectionRelationShipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridEipAddressVO` (
  `uuid` varchar(32) NOT NULL,
  `eipId` varchar(32) NOT NULL,
  `bandWidth` varchar(32) NOT NULL,
  `eipAddress` varchar(32) NOT NULL,
  `allocateResourceUuid` varchar(32) DEFAULT NULL,
  `allocateResourceType` varchar(32) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `eipType` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(128) NOT NULL DEFAULT 'Unknown',
  `dataCenterUuid` varchar(32) NOT NULL,
  `chargeType` varchar(32) NOT NULL DEFAULT 'PayByTraffic',
  `allocateTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkHybridEipAddressVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkHybridEipAddressVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IPsecConnectionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `peerAddress` varchar(255) NOT NULL,
  `authMode` varchar(255) NOT NULL,
  `authKey` text NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `ikeAuthAlgorithm` varchar(32) NOT NULL,
  `ikeEncryptionAlgorithm` varchar(32) NOT NULL,
  `ikeDhGroup` int(10) unsigned NOT NULL,
  `policyAuthAlgorithm` varchar(32) NOT NULL,
  `policyEncryptionAlgorithm` varchar(32) NOT NULL,
  `pfs` varchar(32) DEFAULT NULL,
  `policyMode` varchar(32) NOT NULL,
  `transformProtocol` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `state` varchar(255) NOT NULL DEFAULT 'Enabled',
  `status` varchar(255) NOT NULL DEFAULT 'Ready',
  `ikeVersion` varchar(16) NOT NULL DEFAULT 'ikev1',
  `idType` varchar(16) DEFAULT NULL,
  `remoteId` varchar(128) DEFAULT NULL,
  `localId` varchar(128) DEFAULT NULL,
  `ikeLifeTime` int(10) DEFAULT '0',
  `lifeTime` int(10) DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIPsecConnectionVOVipVO` (`vipUuid`),
  CONSTRAINT `fkIPsecConnectionVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IPsecL3NetworkRefVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `connectionUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIPsecL3NetworkRefVOIPsecConnectionVO` (`connectionUuid`),
  KEY `fkIPsecL3NetworkRefVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkIPsecL3NetworkRefVOIPsecConnectionVO` FOREIGN KEY (`connectionUuid`) REFERENCES `IPsecConnectionVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkIPsecL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IPsecPeerCidrVO` (
  `uuid` varchar(32) NOT NULL,
  `cidr` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `connectionUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIPsecPeerCidrVOIPsecConnectionVO` (`connectionUuid`),
  CONSTRAINT `fkIPsecPeerCidrVOIPsecConnectionVO` FOREIGN KEY (`connectionUuid`) REFERENCES `IPsecConnectionVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IdentityZoneVO` (
  `uuid` varchar(32) NOT NULL,
  `zoneId` varchar(64) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `zoneName` varchar(128) NOT NULL,
  `closed` varchar(1) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIdentityZoneVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkIdentityZoneVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageBackupStorageRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `backupStorageUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `installPath` varchar(2048) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `status` varchar(32) NOT NULL,
  `exportMd5Sum` varchar(255) DEFAULT NULL,
  `exportUrl` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkImageBackupStorageRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkImageBackupStorageRefVOImageEO` (`imageUuid`),
  CONSTRAINT `fkImageBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkImageBackupStorageRefVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageCacheShadowVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) DEFAULT NULL,
  `installUrl` varchar(1024) NOT NULL,
  `mediaType` varchar(64) NOT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `md5sum` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageCacheVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) DEFAULT NULL,
  `installUrl` varchar(1024) NOT NULL,
  `mediaType` varchar(64) NOT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `md5sum` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkImageCacheVOImageEO` (`imageUuid`),
  KEY `fkImageCacheShadowVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkImageCacheShadowVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkImageCacheVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageCacheVolumeRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `imageCacheId` bigint(20) unsigned NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkImageCacheVolumeRefVOImageCacheVO` (`imageCacheId`),
  KEY `fkImageCacheVolumeRefVOPrimaryStorageEO` (`primaryStorageUuid`),
  KEY `fkImageCacheVolumeRefVOVolumeEO` (`volumeUuid`),
  CONSTRAINT `fkImageCacheVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkImageCacheVolumeRefVOImageCacheVO` FOREIGN KEY (`imageCacheId`) REFERENCES `ImageCacheVO` (`id`),
  CONSTRAINT `fkImageCacheVolumeRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `size` bigint(20) unsigned DEFAULT NULL COMMENT 'image size',
  `md5sum` varchar(255) DEFAULT NULL COMMENT 'md5sum of image',
  `name` varchar(255) NOT NULL COMMENT 'image name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'image description',
  `url` varchar(1024) NOT NULL COMMENT 'image url',
  `installUrl` varchar(1024) DEFAULT NULL COMMENT 'url where image installed on secondary storage',
  `mediaType` varchar(32) NOT NULL,
  `format` varchar(32) NOT NULL,
  `system` tinyint(3) unsigned DEFAULT '0',
  `platform` varchar(255) DEFAULT NULL,
  `type` varchar(255) NOT NULL COMMENT 'image type',
  `guestOsType` varchar(255) DEFAULT 'other' COMMENT 'guest os type string',
  `state` varchar(32) NOT NULL COMMENT 'image state',
  `status` varchar(32) NOT NULL COMMENT 'image status',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `actualSize` bigint(20) unsigned DEFAULT NULL,
  `architecture` varchar(32) DEFAULT NULL,
  `virtio` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxImageEOname` (`name`),
  KEY `idxDeleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageOpsJournalVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `backupStorageUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `action` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(32) NOT NULL DEFAULT 'Image',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkImageOpsJournalVOBackupStorageEO` (`backupStorageUuid`),
  CONSTRAINT `fkImageOpsJournalVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImagePackageVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vmUuid` char(32) DEFAULT NULL,
  `backupStorageUuid` char(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `exportUrl` varchar(2048) DEFAULT NULL,
  `md5Sum` char(32) DEFAULT NULL,
  `format` varchar(16) DEFAULT NULL,
  `size` bigint(20) unsigned DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkImagePackageVOVmInstanceEO` (`vmUuid`),
  KEY `fkImagePackageVOBackupStorageEO` (`backupStorageUuid`),
  CONSTRAINT `fkImagePackageVOVmInstanceEO` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkImagePackageVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageReplicationGroupBackupStorageRefVO` (
  `backupStorageUuid` varchar(32) NOT NULL,
  `replicationGroupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`backupStorageUuid`),
  CONSTRAINT `fkImageReplicationGroupBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageReplicationGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageReplicationHistoryVO` (
  `backupStorageUuid` varchar(32) NOT NULL,
  `lastIndex` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`backupStorageUuid`),
  CONSTRAINT `fkImageReplicationHistoryVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ImageStoreBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `hostname` (`hostname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `InfoSecSecretResourcePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `connectionMode` int(10) unsigned NOT NULL,
  `activatedToken` varchar(32) DEFAULT NULL,
  `protectToken` varchar(32) DEFAULT NULL,
  `hmacToken` varchar(32) DEFAULT NULL,
  `encryptPublicKey` text,
  `encryptSubjectDN` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkInfoSecSecretResourcePoolVOSecretResourcePoolVO` FOREIGN KEY (`uuid`) REFERENCES `SecretResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `InfoSecSecurityMachineVO` (
  `uuid` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkInfoSecSecurityMachineVOSecurityMachineVO` FOREIGN KEY (`uuid`) REFERENCES `SecurityMachineVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `InsertVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `voName` varchar(255) NOT NULL,
  `uuid` varchar(32) NOT NULL,
  `foreignVOName` varchar(255) DEFAULT NULL,
  `foreignVOUuid` varchar(32) DEFAULT NULL,
  `insertDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `InstallPathRecycleVO` (
  `trashId` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(32) NOT NULL,
  `storageUuid` varchar(32) NOT NULL,
  `storageType` varchar(32) NOT NULL,
  `installPath` varchar(1024) NOT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `hypervisorType` varchar(32) DEFAULT NULL,
  `trashType` varchar(32) NOT NULL,
  `isFolder` tinyint(1) NOT NULL DEFAULT '0',
  `size` bigint(20) unsigned NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`trashId`),
  UNIQUE KEY `trashId` (`trashId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `InstanceOfferingEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) NOT NULL COMMENT 'instance offering name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'instance offering description',
  `cpuNum` int(10) unsigned NOT NULL COMMENT 'number of cpus',
  `cpuSpeed` bigint(20) unsigned NOT NULL COMMENT 'cpu speed in hz',
  `memorySize` bigint(20) unsigned NOT NULL COMMENT 'memory size in bytes',
  `state` varchar(32) NOT NULL,
  `sortKey` int(10) unsigned DEFAULT '0' COMMENT 'sort key',
  `type` varchar(255) NOT NULL COMMENT 'offering type',
  `duration` varchar(255) NOT NULL,
  `allocatorStrategy` varchar(64) DEFAULT NULL COMMENT 'allocator strategy deciding which allocator chain to use',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `reservedMemorySize` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxInstanceOfferingEOname` (`name`),
  KEY `idxDeleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IpRangeEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `startIp` varchar(64) NOT NULL COMMENT 'start ip',
  `endIp` varchar(64) NOT NULL COMMENT 'end ip',
  `netmask` varchar(64) NOT NULL COMMENT 'netmask',
  `gateway` varchar(64) NOT NULL COMMENT 'gateway',
  `networkCidr` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `ipVersion` int(10) unsigned DEFAULT '4',
  `addressMode` varchar(64) DEFAULT NULL,
  `prefixLen` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIpRangeEOL3NetworkEO` (`l3NetworkUuid`),
  KEY `idxIpRangeEOname` (`name`),
  KEY `idxIpRangeEOstartIp` (`startIp`),
  KEY `idxIpRangeEOendIp` (`endIp`),
  KEY `idxIpRangeEOnetmask` (`netmask`),
  KEY `idxIpRangeEOgateway` (`gateway`),
  CONSTRAINT `fkIpRangeEOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiFileSystemBackendPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `hostname` varchar(255) NOT NULL,
  `sshUsername` varchar(255) NOT NULL,
  `sshPassword` varchar(255) NOT NULL,
  `filesystemType` varchar(255) NOT NULL,
  `chapUsername` varchar(255) DEFAULT NULL,
  `chapPassword` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `hostname` (`hostname`),
  CONSTRAINT `fkIscsiFileSystemBackendPrimaryStorageVOPrimaryStorageEO` FOREIGN KEY (`uuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiIsoVO` (
  `uuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `target` varchar(128) DEFAULT NULL,
  `hostname` varchar(128) DEFAULT NULL,
  `path` varchar(512) DEFAULT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `lun` int(10) unsigned DEFAULT NULL,
  `port` int(10) unsigned DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkIscsiIsoVOPrimaryStorageEO` (`primaryStorageUuid`),
  KEY `fkIscsiIsoVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkIscsiIsoVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkIscsiIsoVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiLunVO` (
  `uuid` varchar(32) NOT NULL,
  `iscsiTargetUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkIscsiLunVOIscsiTargetVO` (`iscsiTargetUuid`),
  CONSTRAINT `fkIscsiLunVOIscsiTargetVO` FOREIGN KEY (`iscsiTargetUuid`) REFERENCES `IscsiTargetVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiServerClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `clusterUuid` varchar(32) NOT NULL,
  `iscsiServerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkIscsiServerClusterRefVOIscsiServerVO` (`iscsiServerUuid`),
  KEY `fkIscsiServerClusterRefVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkIscsiServerClusterRefVOIscsiServerVO` FOREIGN KEY (`iscsiServerUuid`) REFERENCES `IscsiServerVO` (`uuid`),
  CONSTRAINT `fkIscsiServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiServerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `ip` varchar(64) NOT NULL,
  `port` smallint(5) unsigned DEFAULT '3260',
  `state` varchar(32) NOT NULL,
  `chapUserName` varchar(256) DEFAULT NULL,
  `chapUserPassword` varchar(256) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IscsiTargetVO` (
  `uuid` varchar(32) NOT NULL,
  `iqn` varchar(256) NOT NULL,
  `state` varchar(32) NOT NULL,
  `iscsiServerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `fkIscsiTargetVOIscsiServerVO` (`iscsiServerUuid`),
  CONSTRAINT `fkIscsiTargetVOIscsiServerVO` FOREIGN KEY (`iscsiServerUuid`) REFERENCES `IscsiServerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `JobQueueEntryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `jobQueueId` bigint(20) unsigned NOT NULL,
  `state` varchar(128) NOT NULL,
  `context` blob,
  `owner` varchar(255) DEFAULT NULL,
  `issuerManagementNodeId` varchar(32) DEFAULT NULL,
  `restartable` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `inDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `doneDate` timestamp NULL DEFAULT NULL,
  `errText` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkJobQueueEntryVOJobQueueVO` (`jobQueueId`),
  KEY `fkJobQueueEntryVOManagementNodeVO` (`issuerManagementNodeId`),
  CONSTRAINT `fkJobQueueEntryVOManagementNodeVO` FOREIGN KEY (`issuerManagementNodeId`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkJobQueueEntryVOJobQueueVO` FOREIGN KEY (`jobQueueId`) REFERENCES `JobQueueVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `JobQueueVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `workerManagementNodeId` varchar(32) DEFAULT NULL,
  `takenDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `fkJobQueueVOManagementNodeVO` (`workerManagementNodeId`),
  CONSTRAINT `fkJobQueueVOManagementNodeVO` FOREIGN KEY (`workerManagementNodeId`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `JsonLabelVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `labelKey` varchar(128) NOT NULL,
  `labelValue` mediumtext,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `labelKey` (`labelKey`),
  KEY `fkJsonLabelVOResourceVO` (`resourceUuid`),
  CONSTRAINT `fkJsonLabelVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KVMHostVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `port` int(10) unsigned DEFAULT '22',
  `osDistribution` varchar(64) DEFAULT NULL,
  `osRelease` varchar(64) DEFAULT NULL,
  `osVersion` varchar(64) DEFAULT NULL,
  `iscsiInitiatorName` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkKVMHostVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KeyProviderVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `connected` tinyint(1) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukKeyProviderVOName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KeyValueBinaryVO` (
  `uuid` varchar(32) NOT NULL,
  `contents` longblob NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KeyValueVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(32) NOT NULL,
  `className` varchar(128) NOT NULL,
  `entityKey` text NOT NULL,
  `entityValue` text NOT NULL,
  `valueType` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkKeyValueVOKeyValueBinaryVO` (`uuid`),
  CONSTRAINT `fkKeyValueVOKeyValueBinaryVO` FOREIGN KEY (`uuid`) REFERENCES `KeyValueBinaryVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KmsIdentityVO` (
  `uuid` varchar(32) NOT NULL,
  `kmsUuid` varchar(32) NOT NULL,
  `identityType` varchar(32) NOT NULL,
  `clientCertPem` text,
  `clientKeyPem` text,
  `csrPem` text,
  `certExpiredDate` timestamp NULL DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukKmsIdentityVOKmsUuidType` (`kmsUuid`,`identityType`),
  KEY `idxKmsIdentityVOKmsUuid` (`kmsUuid`),
  CONSTRAINT `fkKmsIdentityVOKmsVO` FOREIGN KEY (`kmsUuid`) REFERENCES `KmsVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KmsVO` (
  `uuid` varchar(32) NOT NULL,
  `endpoint` varchar(255) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `kmipVersion` varchar(32) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `trustState` varchar(32) NOT NULL DEFAULT 'MUTUAL_UNTRUSTED',
  `activeIdentityUuid` varchar(32) DEFAULT NULL,
  `serverCertExpiredDate` timestamp NULL DEFAULT NULL,
  `serverCertPem` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxKmsVOActiveIdentityUuid` (`activeIdentityUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KvmHostHypervisorMetadataVO` (
  `uuid` char(32) NOT NULL COMMENT 'uuid',
  `categoryUuid` char(32) NOT NULL,
  `managementNodeUuid` char(32) NOT NULL,
  `hypervisor` varchar(32) NOT NULL,
  `version` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `KvmHostHypervisorMetadataVOHostOsCategoryVO` (`categoryUuid`),
  CONSTRAINT `KvmHostHypervisorMetadataVOHostOsCategoryVO` FOREIGN KEY (`categoryUuid`) REFERENCES `HostOsCategoryVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `KvmHypervisorInfoVO` (
  `uuid` char(32) NOT NULL COMMENT 'uuid',
  `hypervisor` varchar(32) NOT NULL,
  `version` varchar(64) NOT NULL,
  `matchState` char(10) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `KvmHypervisorInfoVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2NetworkClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `l2NetworkUuid` varchar(32) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `l2ProviderType` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uqL2NetworkClusterRefVO` (`l2NetworkUuid`,`clusterUuid`),
  KEY `fkL2NetworkClusterRefVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkL2NetworkClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkL2NetworkClusterRefVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2NetworkEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'l2 network uuid',
  `name` varchar(255) NOT NULL COMMENT 'name',
  `type` varchar(128) NOT NULL COMMENT 'type',
  `vSwitchType` varchar(32) NOT NULL DEFAULT 'LinuxBridge',
  `virtualNetworkId` int(10) unsigned NOT NULL DEFAULT '0',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
  `physicalInterface` varchar(1024) NOT NULL COMMENT 'physical nic that this L2 network attaches to',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkL2NetworkEOZoneEO` (`zoneUuid`),
  KEY `idxL2NetworkEOname` (`name`),
  CONSTRAINT `fkL2NetworkEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2NetworkHostRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `l2NetworkUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `l2ProviderType` varchar(32) DEFAULT NULL,
  `bridgeName` varchar(16) DEFAULT NULL,
  `skipDeletion` tinyint(1) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `ukL2NetworkHost` (`l2NetworkUuid`,`hostUuid`) USING BTREE,
  KEY `fkL2NetworkHostRefVOHostEO` (`hostUuid`),
  CONSTRAINT `fkL2NetworkHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkL2NetworkHostRefVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2PortGroupNetworkVO` (
  `uuid` varchar(32) NOT NULL,
  `vSwitchUuid` varchar(32) NOT NULL,
  `vlanMode` varchar(32) NOT NULL DEFAULT 'ACCESS',
  `vlanId` int(10) unsigned NOT NULL,
  `vlanRanges` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkL2PortGroupNetworkVOL2VirtualSwitchNetworkVO` (`vSwitchUuid`),
  CONSTRAINT `fkL2PortGroupNetworkVOL2VirtualSwitchNetworkVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES `L2VirtualSwitchNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2VirtualSwitchNetworkVO` (
  `uuid` varchar(32) NOT NULL,
  `vSwitchIndex` int(10) unsigned DEFAULT NULL,
  `isDistributed` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L2VlanNetworkVO` (
  `uuid` varchar(32) NOT NULL,
  `vlan` int(10) unsigned NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkL2VlanNetworkVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L3NetworkDnsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `dns` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkL3NetworkDnsVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkL3NetworkDnsVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L3NetworkEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l2 network uuid that this l3 network belongs to',
  `name` varchar(255) NOT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `type` varchar(128) NOT NULL COMMENT 'type',
  `dnsDomain` varchar(255) DEFAULT NULL,
  `system` tinyint(3) unsigned DEFAULT '0',
  `state` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `category` varchar(255) NOT NULL DEFAULT 'Private' COMMENT 'the type network used for',
  `ipVersion` int(10) unsigned DEFAULT '0',
  `enableIPAM` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkL3NetworkEOL2NetworkEO` (`l2NetworkUuid`),
  KEY `fkL3NetworkEOZoneEO` (`zoneUuid`),
  KEY `idxL3NetworkEOname` (`name`),
  CONSTRAINT `fkL3NetworkEOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`),
  CONSTRAINT `fkL3NetworkEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `L3NetworkHostRouteVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `prefix` varchar(255) NOT NULL,
  `nexthop` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkL3NetworkHostRouteVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkL3NetworkHostRouteVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LdapServerVO` (
  `uuid` varchar(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `base` varchar(1024) NOT NULL,
  `username` varchar(1024) NOT NULL,
  `password` varchar(1024) NOT NULL,
  `encryption` varchar(1024) NOT NULL,
  `serverType` varchar(32) NOT NULL DEFAULT 'WindowsAD',
  `filter` varchar(2048) DEFAULT NULL,
  `usernameProperty` varchar(255) NOT NULL DEFAULT 'cn',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LicenseAppIdRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `licenseId` varchar(32) NOT NULL,
  `appId` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LicenseHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(32) NOT NULL,
  `quotaType` varchar(64) NOT NULL DEFAULT 'None',
  `quota` int(10) NOT NULL DEFAULT '0',
  `expiredDate` bigint(20) unsigned NOT NULL DEFAULT '0',
  `issuedDate` bigint(20) unsigned NOT NULL DEFAULT '0',
  `uploadDate` bigint(20) unsigned NOT NULL DEFAULT '0',
  `licenseType` varchar(32) NOT NULL,
  `userName` varchar(64) NOT NULL,
  `prodInfo` varchar(32) NOT NULL DEFAULT '',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `hash` char(32) NOT NULL DEFAULT 'unknown',
  `source` varchar(16) NOT NULL,
  `managementNodeUuid` varchar(32) NOT NULL DEFAULT 'none',
  `mergedTo` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerListenerACLRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `aclUuid` varchar(32) NOT NULL,
  `listenerUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `serverGroupUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkLoadbalancerListenerACLRefVOLoadBalancerListenerVO` (`listenerUuid`) USING BTREE,
  KEY `fkLoadbalancerListenerACLRefVOAccessControlListVO` (`aclUuid`) USING BTREE,
  KEY `fkLoadBalancerListenerACLRefVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  CONSTRAINT `fkLoadbalancerListenerACLRefVOAccessControlListVO` FOREIGN KEY (`aclUuid`) REFERENCES `AccessControlListVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadbalancerListenerACLRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`),
  CONSTRAINT `fkLoadBalancerListenerACLRefVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerListenerCertificateRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `listenerUuid` varchar(32) NOT NULL,
  `certificateUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkLoadBalancerListenerCertificateRefVOLoadBalancerListenerVO` (`listenerUuid`),
  KEY `fkLoadBalancerListenerCertificateRefVOCertificateVO` (`certificateUuid`),
  CONSTRAINT `fkLoadBalancerListenerCertificateRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadBalancerListenerCertificateRefVOCertificateVO` FOREIGN KEY (`certificateUuid`) REFERENCES `CertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerListenerServerGroupRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `listenerUuid` varchar(32) NOT NULL,
  `serverGroupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkLoadBalancerListenerServerGroupRefVOLoadBalancerListenerVO` (`listenerUuid`),
  KEY `fkLoadBalancerListenerServerGroupRefVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  CONSTRAINT `fkLoadBalancerListenerServerGroupRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadBalancerListenerServerGroupRefVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerListenerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `loadBalancerUuid` varchar(32) NOT NULL,
  `instancePort` int(10) unsigned NOT NULL,
  `loadBalancerPort` int(10) unsigned NOT NULL,
  `protocol` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `serverGroupUuid` varchar(32) DEFAULT NULL,
  `securityPolicyType` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkLoadBalancerListenerVOLoadBalancerVO` (`loadBalancerUuid`),
  KEY `fkLoadBalancerListenerVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  CONSTRAINT `fkLoadBalancerListenerVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkLoadBalancerListenerVOLoadBalancerVO` FOREIGN KEY (`loadBalancerUuid`) REFERENCES `LoadBalancerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerListenerVmNicRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `listenerUuid` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkLoadBalancerListenerVmNicRefVOLoadBalancerListenerVO` (`listenerUuid`),
  KEY `fkLoadBalancerListenerVmNicRefVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkLoadBalancerListenerVmNicRefVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadBalancerListenerVmNicRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerServerGroupServerIpVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serverGroupUuid` varchar(32) NOT NULL,
  `ipAddress` varchar(128) NOT NULL,
  `weight` bigint(20) unsigned NOT NULL DEFAULT '100',
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkLoadBalancerServerGroupServerIpVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  CONSTRAINT `fkLoadBalancerServerGroupServerIpVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerServerGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `loadBalancerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkLoadBalancerServerGroupVOLoadBalancerVO` (`loadBalancerUuid`),
  CONSTRAINT `fkLoadBalancerServerGroupVOLoadBalancerVO` FOREIGN KEY (`loadBalancerUuid`) REFERENCES `LoadBalancerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerServerGroupVmNicRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serverGroupUuid` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) NOT NULL,
  `weight` bigint(20) unsigned NOT NULL DEFAULT '100',
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkLoadBalancerServerGroupVmNicRefVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  KEY `fkLoadBalancerServerGroupVmNicRefVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkLoadBalancerServerGroupVmNicRefVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLoadBalancerServerGroupVmNicRefVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoadBalancerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `providerType` varchar(255) DEFAULT NULL,
  `state` varchar(64) NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(255) DEFAULT 'Shared',
  `serverGroupUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkLoadBalancerVOVipVO` (`vipUuid`),
  KEY `fkLoadBalancerVOLoadBalancerServerGroupVO` (`serverGroupUuid`),
  CONSTRAINT `fkLoadBalancerVOLoadBalancerServerGroupVO` FOREIGN KEY (`serverGroupUuid`) REFERENCES `LoadBalancerServerGroupVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkLoadBalancerVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LocalStorageHostRefVO` (
  `hostUuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) NOT NULL DEFAULT '0',
  `totalPhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `availablePhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `systemUsedCapacity` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`hostUuid`,`primaryStorageUuid`),
  KEY `fkLocalStorageHostRefVOPrimaryStorageEO` (`primaryStorageUuid`),
  KEY `idxLocalStorageHostRefVOtotalCapacity` (`totalCapacity`),
  KEY `idxLocalStorageHostRefVOavailableCapacity` (`availableCapacity`),
  KEY `idxLocalStorageHostRefVOtotalPhysicalCapacity` (`totalPhysicalCapacity`),
  KEY `idxLocalStorageHostRefVOavailablePhysicalCapacity` (`availablePhysicalCapacity`),
  CONSTRAINT `fkLocalStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkLocalStorageHostRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LocalStorageResourceRefVO` (
  `resourceUuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL DEFAULT '',
  `size` bigint(20) unsigned DEFAULT '0',
  `resourceType` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`resourceUuid`,`hostUuid`,`primaryStorageUuid`),
  KEY `fkLocalStorageResourceRefVOHostEO` (`hostUuid`),
  KEY `fkLocalStorageResourceRefVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkLocalStorageResourceRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LogVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `content` text,
  `type` varchar(32) NOT NULL,
  `level` varchar(32) DEFAULT NULL,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LoginAttemptsVO` (
  `uuid` varchar(32) NOT NULL,
  `targetResourceIdentity` varchar(256) NOT NULL,
  `attempts` int(10) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `locked` tinyint(1) unsigned NOT NULL,
  `forceChangePassword` tinyint(1) unsigned NOT NULL,
  `unlockDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `successCount` int(10) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LongJobVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `apiId` varchar(32) NOT NULL,
  `jobName` varchar(255) NOT NULL,
  `jobData` mediumtext NOT NULL,
  `jobResult` mediumtext,
  `state` varchar(255) NOT NULL,
  `targetResourceUuid` varchar(32) DEFAULT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `executeTime` int(10) unsigned DEFAULT NULL,
  `parentUuid` char(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkLongJobVOManagementNodeVO` (`managementNodeUuid`),
  KEY `idxLongJobVOapiId` (`apiId`),
  KEY `idxLongJobVOtargetResourceUuid` (`targetResourceUuid`),
  CONSTRAINT `fkLongJobVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `LunVO` (
  `name` varchar(256) DEFAULT NULL,
  `uuid` varchar(32) NOT NULL,
  `wwid` varchar(256) NOT NULL,
  `vendor` varchar(256) DEFAULT NULL,
  `model` varchar(256) DEFAULT NULL,
  `wwn` varchar(256) DEFAULT NULL,
  `serial` varchar(256) DEFAULT NULL,
  `hctl` varchar(64) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  `path` varchar(128) DEFAULT NULL,
  `source` varchar(128) DEFAULT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `state` varchar(64) DEFAULT NULL,
  `multipathDeviceUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ManagementNodeContextVO` (
  `id` bigint(20) unsigned NOT NULL,
  `inventory` blob,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ManagementNodeVO` (
  `uuid` varchar(32) NOT NULL,
  `hostName` varchar(255) DEFAULT NULL,
  `port` int(10) unsigned DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `joinDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `heartBeat` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MdevDeviceSpecVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `specification` text,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MdevDeviceVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `parentUuid` varchar(32) DEFAULT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `mdevSpecUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `chooser` varchar(32) DEFAULT 'None',
  `mttyUuid` varchar(32) DEFAULT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxMdevDeviceVOtype` (`type`),
  KEY `idxMdevDeviceVOhostUuid` (`hostUuid`),
  KEY `idxMdevDeviceVOparentUuid` (`parentUuid`),
  KEY `idxMdevDeviceVOmdevSpecUuid` (`mdevSpecUuid`),
  KEY `fkMdevDeviceVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkMdevDeviceVOMttyDeviceVO` (`mttyUuid`),
  CONSTRAINT `fkMdevDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMdevDeviceVOMdevSpecVO` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkMdevDeviceVOMttyDeviceVO` FOREIGN KEY (`mttyUuid`) REFERENCES `MttyDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMdevDeviceVOPciDeviceVO` FOREIGN KEY (`parentUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMdevDeviceVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MediaVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MetricDataHttpReceiverVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `url` varchar(256) NOT NULL,
  `state` varchar(128) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MetricRuleTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `monitorTemplateUuid` varchar(32) NOT NULL,
  `comparisonOperator` varchar(128) NOT NULL,
  `period` int(10) unsigned NOT NULL,
  `repeatInterval` int(10) unsigned NOT NULL,
  `namespace` varchar(255) NOT NULL,
  `metricName` varchar(512) NOT NULL,
  `threshold` double NOT NULL,
  `repeatCount` int(11) DEFAULT NULL,
  `enableRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` varchar(4096) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `monitorTemplateUuid` (`monitorTemplateUuid`),
  CONSTRAINT `fkMetricRuleTemplateVOMonitorTemplateVO` FOREIGN KEY (`monitorTemplateUuid`) REFERENCES `MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MetricTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `receiverUuid` varchar(32) NOT NULL,
  `template` varchar(4096) NOT NULL,
  `namespace` varchar(64) NOT NULL,
  `metricName` varchar(128) NOT NULL,
  `labelsJsonStr` varchar(256) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkMetricTemplateVOMetricDataHttpReceiverVO` (`receiverUuid`),
  CONSTRAINT `fkMetricTemplateVOMetricDataHttpReceiverVO` FOREIGN KEY (`receiverUuid`) REFERENCES `MetricDataHttpReceiverVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MiniStorageHostRefVO` (
  `id` bigint(20) unsigned NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) unsigned DEFAULT '0',
  `totalPhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `availablePhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MiniStorageResourceReplicationVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'mini storage replications',
  `type` varchar(32) NOT NULL COMMENT 'resource type',
  `state` varchar(32) NOT NULL COMMENT 'replication state',
  `resourceUuid` varchar(32) NOT NULL COMMENT 'resource uuid',
  `size` bigint(20) unsigned DEFAULT '0' COMMENT 'resource size',
  `port` bigint(20) unsigned DEFAULT '0' COMMENT 'resource port on host',
  `hostUuid` varchar(32) NOT NULL COMMENT 'host',
  `primaryStorageUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
  `networkStatus` varchar(32) DEFAULT NULL COMMENT 'replication network status',
  `diskStatus` varchar(32) DEFAULT NULL COMMENT 'replication disk status',
  `role` varchar(32) DEFAULT NULL COMMENT 'replication role',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkMiniStorageResourceReplicationVOPrimaryStorageEO` (`primaryStorageUuid`),
  CONSTRAINT `fkMiniStorageResourceReplicationVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MiniStorageVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'mini storage',
  `miniStorageType` varchar(32) NOT NULL COMMENT 'type',
  `diskIdentifier` varchar(255) DEFAULT NULL COMMENT 'disk wwid/wwn/etc',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MirrorNetworkUsedIpVO` (
  `uuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkMirrorNetworkUsedIpVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `fkMirrorNetworkUsedIpVOHostEO` (`hostUuid`),
  KEY `fkMirrorNetworkUsedIpVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkMirrorNetworkUsedIpVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorNetworkUsedIpVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorNetworkUsedIpVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorGroupAlarmVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `alarmUuid` varchar(32) NOT NULL,
  `metricRuleTemplateUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
  CONSTRAINT `fkMonitorGroupAlarmVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorGroupEventSubscriptionVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `eventSubscriptionUuid` varchar(32) NOT NULL,
  `eventRuleTemplateUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `groupUuid` (`groupUuid`),
  CONSTRAINT `fkMonitorGroupEventSubscriptionVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorGroupInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `instanceResourceType` varchar(128) NOT NULL,
  `instanceUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `groupUuidInstanceUuid` (`groupUuid`,`instanceUuid`),
  KEY `groupUuid` (`groupUuid`),
  KEY `instanceUuid` (`instanceUuid`),
  CONSTRAINT `fkMonitorGroupInstanceVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `MonitorGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorGroupTemplateRefVO` (
  `uuid` varchar(32) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `isApplied` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `groupUuidTemplateUuid` (`groupUuid`,`templateUuid`),
  KEY `fkMonitorGroupTemplateRefVOMonitorTemplateVO` (`templateUuid`),
  CONSTRAINT `fkMonitorGroupTemplateRefVOMonitorGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `MonitorGroupVO` (`uuid`),
  CONSTRAINT `fkMonitorGroupTemplateRefVOMonitorTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `MonitorTemplateVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `actions` varchar(4096) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionRefVO` (
  `actionUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`actionUuid`,`triggerUuid`),
  KEY `fkMonitorTriggerActionRefVOMonitorTriggerVO` (`triggerUuid`),
  CONSTRAINT `fkMonitorTriggerActionRefVOMonitorTriggerActionVO` FOREIGN KEY (`actionUuid`) REFERENCES `MonitorTriggerActionVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMonitorTriggerActionRefVOMonitorTriggerVO` FOREIGN KEY (`triggerUuid`) REFERENCES `MonitorTriggerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `expression` varchar(2048) NOT NULL,
  `recoveryExpression` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `contextData` text,
  `duration` int(10) unsigned NOT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `lastStatusChangeTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MttyDeviceVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `virtStatus` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkDeviceVOHostEO` (`hostUuid`),
  CONSTRAINT `fkDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MulticastRouterRendezvousPointVO` (
  `uuid` varchar(32) NOT NULL,
  `multicastRouterUuid` varchar(32) NOT NULL,
  `rpAddress` varchar(64) NOT NULL,
  `groupAddress` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkMultiCastRouterRendezvousPointVOMulticastRouterVO` (`multicastRouterUuid`),
  CONSTRAINT `fkMultiCastRouterRendezvousPointVOMulticastRouterVO` FOREIGN KEY (`multicastRouterUuid`) REFERENCES `MulticastRouterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MulticastRouterVO` (
  `uuid` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MulticastRouterVpcVRouterRefVO` (
  `uuid` varchar(32) NOT NULL,
  `vpcRouterUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkMulticastRouterVpcVRouterRefVOVpcRouterVmVO` (`vpcRouterUuid`),
  CONSTRAINT `fkMulticastRouterVpcVRouterRefVOMulticastRouterVO` FOREIGN KEY (`uuid`) REFERENCES `MulticastRouterVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMulticastRouterVpcVRouterRefVOVpcRouterVmVO` FOREIGN KEY (`vpcRouterUuid`) REFERENCES `VpcRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NasFileSystemVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `protocol` varchar(16) NOT NULL,
  `fileSystemId` varchar(32) NOT NULL,
  `type` varchar(16) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NasMountTargetVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `nasFileSystemUuid` varchar(32) NOT NULL,
  `mountDomain` varchar(255) NOT NULL,
  `type` varchar(16) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkNasMountTargetVONasFileSystemVO` (`nasFileSystemUuid`),
  CONSTRAINT `fkNasMountTargetVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `NasFileSystemVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkRouterAreaRefVO` (
  `uuid` varchar(32) NOT NULL,
  `routerAreaUuid` varchar(32) NOT NULL,
  `vRouterUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `applianceVmType` varchar(255) DEFAULT 'vpcvrouter',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkNetworkRouterAreaRefVORouterAreaVO` (`routerAreaUuid`),
  KEY `fkNetworkRouterAreaRefVOL3NetworkVO` (`l3NetworkUuid`),
  KEY `fkNetworkRouterAreaRefVOVpcRouterVmVO` (`vRouterUuid`),
  CONSTRAINT `fkNetworkRouterAreaRefVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNetworkRouterAreaRefVORouterAreaVO` FOREIGN KEY (`routerAreaUuid`) REFERENCES `RouterAreaVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkRouterFlowMeterRefVO` (
  `uuid` varchar(32) NOT NULL,
  `flowMeterUuid` varchar(32) NOT NULL,
  `vFlowRouterUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkNetworkRouterFlowMeterRefVOFlowMeterVO` (`flowMeterUuid`),
  KEY `fkNetworkRouterFlowMeterRefVOL3NetworkVO` (`l3NetworkUuid`),
  KEY `fkNetworkRouterFlowMeterRefVOFlowRouterVmVO` (`vFlowRouterUuid`),
  CONSTRAINT `fkNetworkRouterFlowMeterRefVOFlowMeterVO` FOREIGN KEY (`flowMeterUuid`) REFERENCES `FlowMeterVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNetworkRouterFlowMeterRefVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNetworkRouterFlowMeterRefVOFlowRouterVmVO` FOREIGN KEY (`vFlowRouterUuid`) REFERENCES `FlowRouterVO` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkServiceL3NetworkRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `networkServiceProviderUuid` varchar(32) NOT NULL,
  `networkServiceType` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkNetworkServiceL3NetworkRefVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `fkNetworkServiceL3NetworkRefVONetworkServiceProviderVO` (`networkServiceProviderUuid`),
  CONSTRAINT `fkNetworkServiceL3NetworkRefVONetworkServiceProviderVO` FOREIGN KEY (`networkServiceProviderUuid`) REFERENCES `NetworkServiceProviderVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNetworkServiceL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkServiceProviderL2NetworkRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `networkServiceProviderUuid` varchar(32) NOT NULL,
  `l2NetworkUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uqNetworkServiceProviderL2NetworkRefVO` (`networkServiceProviderUuid`,`l2NetworkUuid`),
  KEY `fkNetworkServiceProviderL2NetworkRefVOL2NetworkEO` (`l2NetworkUuid`),
  CONSTRAINT `fkNetworkServiceProviderL2NetworkRefVONetworkServiceProviderVO` FOREIGN KEY (`networkServiceProviderUuid`) REFERENCES `NetworkServiceProviderVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNetworkServiceProviderL2NetworkRefVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkServiceProviderVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `type` (`type`),
  KEY `idxNetworkServiceProviderVOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NetworkServiceTypeVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `networkServiceProviderUuid` varchar(32) NOT NULL,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkNetworkServiceTypeVONetworkServiceProviderVO` (`networkServiceProviderUuid`),
  CONSTRAINT `fkNetworkServiceTypeVONetworkServiceProviderVO` FOREIGN KEY (`networkServiceProviderUuid`) REFERENCES `NetworkServiceProviderVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NkpVO` (
  `uuid` varchar(32) NOT NULL,
  `kdf` varchar(64) NOT NULL,
  `saltPolicy` varchar(64) NOT NULL,
  `backedUp` tinyint(1) NOT NULL DEFAULT '0',
  `currentVersion` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NormalIpRangeVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkNormalIpRangeVOIpRangeEO` FOREIGN KEY (`uuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NotificationSubscriptionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `notificationName` varchar(1024) NOT NULL,
  `filter` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NotificationVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(1024) NOT NULL,
  `content` text NOT NULL,
  `arguments` text,
  `sender` varchar(1024) NOT NULL,
  `status` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `resourceUuid` varchar(255) DEFAULT NULL,
  `resourceType` varchar(255) DEFAULT NULL,
  `opaque` text,
  `time` bigint(20) unsigned DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `dateTime` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`,`dateTime`),
  UNIQUE KEY `uuid` (`uuid`,`dateTime`),
  KEY `notification_resource_uuid_idx` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
/*!50100 PARTITION BY RANGE ( YEAR(dateTime))
(PARTITION p2017 VALUES LESS THAN (2018) ENGINE = InnoDB,
 PARTITION p2018 VALUES LESS THAN (2019) ENGINE = InnoDB,
 PARTITION p2019 VALUES LESS THAN (2020) ENGINE = InnoDB,
 PARTITION p2020 VALUES LESS THAN (2021) ENGINE = InnoDB,
 PARTITION p2021 VALUES LESS THAN (2022) ENGINE = InnoDB,
 PARTITION p2022 VALUES LESS THAN (2023) ENGINE = InnoDB,
 PARTITION p2023 VALUES LESS THAN (2024) ENGINE = InnoDB,
 PARTITION p2024 VALUES LESS THAN (2025) ENGINE = InnoDB,
 PARTITION p2025 VALUES LESS THAN (2026) ENGINE = InnoDB,
 PARTITION p2026 VALUES LESS THAN (2027) ENGINE = InnoDB,
 PARTITION p2027 VALUES LESS THAN (2028) ENGINE = InnoDB,
 PARTITION p9999 VALUES LESS THAN MAXVALUE ENGINE = InnoDB) */;

CREATE TABLE `NvmeLunHostRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `nvmeLunUuid` varchar(32) NOT NULL,
  `hctl` varchar(64) DEFAULT NULL,
  `path` varchar(128) DEFAULT NULL,
  `locate` varchar(16) NOT NULL DEFAULT 'Unknown',
  `transport` varchar(32) NOT NULL DEFAULT '',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  KEY `fkNvmeLunHostRefVONvmeLunVO` (`nvmeLunUuid`),
  KEY `fkNvmeLunHostRefVOHostVO` (`hostUuid`),
  CONSTRAINT `fkNvmeLunHostRefVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNvmeLunHostRefVONvmeLunVO` FOREIGN KEY (`nvmeLunUuid`) REFERENCES `NvmeLunVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeLunVO` (
  `uuid` varchar(32) NOT NULL,
  `nvmeTargetUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkNvmeLunVONvmeTargetVO` (`nvmeTargetUuid`),
  CONSTRAINT `fkNvmeLunVONvmeTargetVO` FOREIGN KEY (`nvmeTargetUuid`) REFERENCES `NvmeTargetVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeServerClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `clusterUuid` varchar(32) NOT NULL,
  `nvmeServerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkNvmeServerClusterRefVONvmeServerVO` (`nvmeServerUuid`),
  KEY `fkNvmeServerClusterRefVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkNvmeServerClusterRefVONvmeServerVO` FOREIGN KEY (`nvmeServerUuid`) REFERENCES `NvmeServerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkNvmeServerClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeServerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `ip` varchar(64) NOT NULL,
  `port` int(10) unsigned DEFAULT '4420',
  `transport` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `NvmeTargetVO` (
  `name` varchar(256) DEFAULT NULL,
  `uuid` varchar(32) NOT NULL,
  `nqn` varchar(256) NOT NULL,
  `state` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `nvmeServerUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkNvmeTargetVONvmeServerVO` (`nvmeServerUuid`),
  CONSTRAINT `fkNvmeTargetVONvmeServerVO` FOREIGN KEY (`nvmeServerUuid`) REFERENCES `NvmeServerVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `OAuth2ClientVO` (
  `uuid` char(32) NOT NULL,
  `clientId` varchar(255) NOT NULL,
  `clientSecret` varchar(255) DEFAULT NULL,
  `grantType` varchar(64) NOT NULL,
  `loginMNUrl` varchar(255) DEFAULT NULL,
  `redirectUrl` varchar(255) DEFAULT NULL,
  `authorizationUrl` varchar(255) DEFAULT NULL,
  `tokenUrl` varchar(255) NOT NULL,
  `userinfoUrl` varchar(255) DEFAULT NULL,
  `logoutUrl` varchar(255) DEFAULT NULL,
  `usernameProperty` varchar(255) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkOAuth2ClientVOThirdPartyAccountSourceVO` FOREIGN KEY (`uuid`) REFERENCES `ThirdPartyAccountSourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `OAuth2TokenVO` (
  `uuid` varchar(32) NOT NULL,
  `accessToken` text,
  `idToken` text,
  `refreshToken` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkOAuth2TokenVOSSOTokenVO` FOREIGN KEY (`uuid`) REFERENCES `SSOTokenVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `OssBucketDomainVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ossBucketUuid` varchar(32) NOT NULL,
  `ossDomain` varchar(256) NOT NULL,
  `ossKey` varchar(127) NOT NULL,
  `ossSecret` varchar(127) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkOssBucketDomainVOOssBucketVO` (`ossBucketUuid`),
  CONSTRAINT `fkOssBucketDomainVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `OssBucketVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `OssBucketVO` (
  `uuid` varchar(32) NOT NULL,
  `bucketName` varchar(64) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `description` varchar(1024) DEFAULT NULL,
  `regionName` varchar(64) DEFAULT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `current` varchar(32) DEFAULT 'false',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkOssBucketVODataCenterVO` (`dataCenterUuid`),
  CONSTRAINT `fkOssBucketVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `OssUploadPartsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uploadId` varchar(32) NOT NULL,
  `ossBucketUuid` varchar(32) NOT NULL,
  `fileKey` varchar(128) NOT NULL,
  `partNumber` int(16) NOT NULL,
  `total` int(16) NOT NULL,
  `eTag` varchar(32) NOT NULL,
  `partSize` bigint(32) NOT NULL,
  `partCRC` bigint(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkOssUploadPartsVOOssBucketVO` (`ossBucketUuid`),
  KEY `uploadId` (`uploadId`),
  CONSTRAINT `fkOssUploadPartsVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `OssBucketVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmName` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceMdevSpecRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `mdevSpecUuid` varchar(32) NOT NULL,
  `effective` tinyint(1) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkSpecRefPciDeviceUuid` (`pciDeviceUuid`),
  KEY `fkSpecRefMdevSpecUuid` (`mdevSpecUuid`),
  CONSTRAINT `fkSpecRefPciDeviceUuid` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSpecRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceOfferingInstanceOfferingRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceOfferingUuid` varchar(32) NOT NULL,
  `instanceOfferingUuid` varchar(32) NOT NULL,
  `metadata` varchar(4096) DEFAULT NULL,
  `pciDeviceCount` int(11) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `PciDeviceOfferingInstanceOfferingRefVOPciDeviceOfferingVO` (`pciDeviceOfferingUuid`),
  KEY `PciDeviceOfferingInstanceOfferingRefVOInstanceOfferingEO` (`instanceOfferingUuid`),
  CONSTRAINT `PciDeviceOfferingInstanceOfferingRefVOPciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `PciDeviceOfferingInstanceOfferingRefVOInstanceOfferingEO` FOREIGN KEY (`instanceOfferingUuid`) REFERENCES `InstanceOfferingEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceOfferingVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ramSize` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDevicePciDeviceOfferingRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `pciDeviceOfferingUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `PciDeviceUsageVOPciDeviceVO` (`pciDeviceUuid`),
  KEY `PciDevicePciDeviceOfferingVO` (`pciDeviceOfferingUuid`),
  CONSTRAINT `PciDeviceUsageVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `PciDevicePciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceSpecVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `romContent` mediumtext,
  `romVersion` varchar(255) DEFAULT NULL,
  `romMd5sum` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `isVirtual` tinyint(1) NOT NULL DEFAULT '0',
  `maxPartNum` int(11) DEFAULT NULL,
  `ramSize` varchar(32) DEFAULT NULL,
  `vendor` varchar(128) DEFAULT NULL,
  `device` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmName` varchar(255) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPciDeviceUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxPciDeviceUsageVOpciDeviceUuid` (`accountUuid`,`dateInLong`,`pciDeviceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmName` varchar(255) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPciDeviceUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `pciDeviceAddress` varchar(32) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `metadata` varchar(4096) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `name` varchar(255) NOT NULL,
  `virtStatus` varchar(32) DEFAULT NULL,
  `parentUuid` varchar(32) DEFAULT NULL,
  `pciSpecUuid` varchar(32) DEFAULT NULL,
  `iommuGroup` varchar(255) DEFAULT NULL,
  `chooser` varchar(32) DEFAULT 'None',
  `vendor` varchar(128) DEFAULT NULL,
  `device` varchar(128) DEFAULT NULL,
  `passThroughState` varchar(32) NOT NULL DEFAULT 'Disabled',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxPciDeviceVOtype` (`type`),
  KEY `idxPciDeviceVOhostUuid` (`hostUuid`),
  KEY `idxPciDeviceVOparentUuid` (`parentUuid`),
  KEY `idxPciDeviceVOpciSpecUuid` (`pciSpecUuid`),
  KEY `fkPciDeviceVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkPciDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPciDeviceVOPciDeviceSpecVO` FOREIGN KEY (`pciSpecUuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkPciDeviceVOPciDeviceVO` FOREIGN KEY (`parentUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPciDeviceVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PhysicalDriveSmartSelfTestHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `raidPhysicalDriveUuid` varchar(32) DEFAULT NULL,
  `runningState` varchar(255) DEFAULT NULL,
  `testResult` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPhysicalDriveSmartSelfTestHistoryVORaidPhysicalDriveVO` (`raidPhysicalDriveUuid`),
  CONSTRAINT `fkPhysicalDriveSmartSelfTestHistoryVORaidPhysicalDriveVO` FOREIGN KEY (`raidPhysicalDriveUuid`) REFERENCES `RaidPhysicalDriveVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PluginDriverVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `type` varchar(64) NOT NULL,
  `vendor` varchar(64) NOT NULL,
  `features` varchar(1024) NOT NULL,
  `optionTypes` text,
  `license` varchar(1024) DEFAULT NULL,
  `version` varchar(1024) DEFAULT NULL,
  `description` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetL3RefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ruleSetUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteRuleSetNicRefVOPolicyRouteRuleSetVO` (`ruleSetUuid`) USING BTREE,
  KEY `fkPolicyRouteRuleSetNicRefVOVmNicVO` (`l3NetworkUuid`) USING BTREE,
  CONSTRAINT `fkPolicyRouteRuleSetNicRefVOVmNicVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleSetNicRefVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `vyosName` varchar(32) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(64) NOT NULL DEFAULT 'User',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleSetVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vRouterUuid` varchar(32) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteRuleSetVRouterRefVOVirtualRouteVMVO` (`vRouterUuid`),
  KEY `fkPolicyRouteRuleSetVRouterRefVOPolicyRouteRuleSetVO` (`ruleSetUuid`),
  CONSTRAINT `fkPolicyRouteRuleSetVRouterRefVOVirtualRouteVMVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleSetVRouterRefVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `ruleNumber` int(4) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `protocol` varchar(32) DEFAULT NULL,
  `tableUuid` varchar(32) DEFAULT NULL,
  `destIp` varchar(255) DEFAULT NULL,
  `sourceIp` varchar(255) DEFAULT NULL,
  `destPort` varchar(255) DEFAULT NULL,
  `sourcePort` varchar(255) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPolicyRouteRuleVOPolicyRouteRuleSetVO` (`ruleSetUuid`),
  KEY `fkPolicyRouteRuleVOPolicyRouteTableVO` (`tableUuid`),
  CONSTRAINT `fkPolicyRouteRuleVOPolicyRouteRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `PolicyRouteRuleSetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteRuleVOPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableRouteEntryVO` (
  `uuid` varchar(32) NOT NULL,
  `tableUuid` varchar(32) NOT NULL,
  `distance` int(10) DEFAULT NULL,
  `destinationCidr` varchar(64) NOT NULL,
  `nextHopIp` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPolicyRouteTableRouteEntryVOPolicyRouteTableVO` (`tableUuid`),
  CONSTRAINT `fkPolicyRouteTableRouteEntryVOPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableVO` (
  `uuid` varchar(255) NOT NULL,
  `tableNumber` int(3) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(64) NOT NULL DEFAULT 'User',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PolicyRouteTableVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tableUuid` varchar(32) NOT NULL,
  `vRouterUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkPolicyRouteTableVRouterRefVcPolicyRouteTableVO` (`tableUuid`),
  KEY `fkPolicyRouteTableVRouterRefVOVirtualRouterVMVO` (`vRouterUuid`),
  CONSTRAINT `fkPolicyRouteTableVRouterRefVOVirtualRouterVMVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPolicyRouteTableVRouterRefVcPolicyRouteTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PolicyRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortForwardingRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `guestIp` varchar(128) DEFAULT NULL,
  `vipIp` varchar(128) NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `vipPortStart` int(11) NOT NULL,
  `vipPortEnd` int(11) NOT NULL,
  `privatePortStart` int(11) NOT NULL,
  `privatePortEnd` int(11) NOT NULL,
  `vmNicUuid` varchar(32) DEFAULT NULL,
  `allowedCidr` varchar(128) DEFAULT NULL,
  `protocolType` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkPortForwardingRuleVOVipVO` (`vipUuid`),
  KEY `fkPortForwardingRuleVOVmNicVO` (`vmNicUuid`),
  KEY `idxPortForwardingRuleVOname` (`name`),
  KEY `idxPortForwardingRuleVOvipPortStart` (`vipPortStart`),
  KEY `idxPortForwardingRuleVOvipPortEnd` (`vipPortEnd`),
  KEY `idxPortForwardingRuleVOprivatePortStart` (`privatePortStart`),
  KEY `idxPortForwardingRuleVOprivatePortEnd` (`privatePortEnd`),
  CONSTRAINT `fkPortForwardingRuleVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkPortForwardingRuleVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `vSwitchUuid` varchar(32) NOT NULL,
  `vlanMode` varchar(32) NOT NULL DEFAULT 'ACCESS',
  `vlanId` int(10) unsigned NOT NULL,
  `vlanRanges` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkPortGroupVOL2VirtualSwitchNetworkVO` (`vSwitchUuid`),
  CONSTRAINT `fkPortGroupVOL2VirtualSwitchNetworkVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES `L2VirtualSwitchNetworkVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortMirrorSessionMirrorNetworkRefVO` (
  `uuid` varchar(32) NOT NULL,
  `sessionUuid` varchar(32) NOT NULL,
  `srcTunnelUuid` varchar(32) NOT NULL,
  `dstTunnelUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) DEFAULT 'GRE',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkMirrorRefVOPortMirrorSessionVO` (`sessionUuid`),
  KEY `fkMirrorRefVOMirrorNetworkUsedIpVOSrc` (`srcTunnelUuid`),
  KEY `fkMirrorRefVOMirrorNetworkUsedIpVODst` (`dstTunnelUuid`),
  CONSTRAINT `fkMirrorRefVOPortMirrorSessionVO` FOREIGN KEY (`sessionUuid`) REFERENCES `PortMirrorSessionVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorRefVOMirrorNetworkUsedIpVOSrc` FOREIGN KEY (`srcTunnelUuid`) REFERENCES `MirrorNetworkUsedIpVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkMirrorRefVOMirrorNetworkUsedIpVODst` FOREIGN KEY (`dstTunnelUuid`) REFERENCES `MirrorNetworkUsedIpVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortMirrorSessionSequenceNumberVO` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortMirrorSessionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `srcEndPoint` varchar(32) NOT NULL,
  `dstEndPoint` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `status` varchar(128) DEFAULT 'Created',
  `internalId` int(10) unsigned NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `portMirrorUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPortMirrorSessionVOPortMirrorVO` (`portMirrorUuid`),
  KEY `fkPortMirrorSessionVOSrcNIcVmNicVO` (`srcEndPoint`),
  CONSTRAINT `fkPortMirrorSessionVOPortMirrorVO` FOREIGN KEY (`portMirrorUuid`) REFERENCES `PortMirrorVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPortMirrorSessionVOSrcNIcVmNicVO` FOREIGN KEY (`srcEndPoint`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PortMirrorVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(128) DEFAULT '',
  `state` varchar(128) DEFAULT 'Enable',
  `mirrorNetworkUuid` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkPortMirrorVOL3NetworkVO` (`mirrorNetworkUuid`),
  CONSTRAINT `fkPortMirrorVOL3NetworkVO` FOREIGN KEY (`mirrorNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PreconfigurationTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `distribution` varchar(64) NOT NULL,
  `type` varchar(32) NOT NULL,
  `content` mediumtext NOT NULL,
  `md5sum` varchar(255) NOT NULL,
  `isPredefined` tinyint(1) unsigned DEFAULT '0',
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PriceBareMetal2ChassisOfferingRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `priceUuid` varchar(32) NOT NULL,
  `bareMetal2ChassisOfferingUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPriceBareMetal2ChassisOfferingRefVOPriceVO` (`priceUuid`),
  KEY `fkPriceBareMetal2ChassisOfferingRefVOBareMetal2ChassisOfferingVO` (`bareMetal2ChassisOfferingUuid`),
  CONSTRAINT `fkPriceBareMetal2ChassisOfferingRefVOPriceVO` FOREIGN KEY (`priceUuid`) REFERENCES `PriceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPriceBareMetal2ChassisOfferingRefVOBareMetal2ChassisOfferingVO` FOREIGN KEY (`bareMetal2ChassisOfferingUuid`) REFERENCES `BareMetal2ChassisOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PricePciDeviceOfferingRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `priceUuid` varchar(32) NOT NULL,
  `pciDeviceOfferingUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPricePciDeviceOfferingRefVOPriceVO` (`priceUuid`),
  KEY `fkPricePciDeviceOfferingRefVOPciDeviceOfferingVO` (`pciDeviceOfferingUuid`),
  CONSTRAINT `fkPricePciDeviceOfferingRefVOPriceVO` FOREIGN KEY (`priceUuid`) REFERENCES `PriceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPricePciDeviceOfferingRefVOPciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PriceTableVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `PriceTableVO` VALUE
('12a087c058cc45d5bf80a605f17c0083','global_default',NULL,'2026-07-30 18:02:24','2026-07-30 18:02:24');

CREATE TABLE `PriceVO` (
  `uuid` varchar(32) NOT NULL,
  `resourceName` varchar(255) NOT NULL,
  `timeUnit` varchar(255) NOT NULL,
  `resourceUnit` varchar(255) DEFAULT NULL,
  `price` double(14,5) DEFAULT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `tableUuid` varchar(32) NOT NULL,
  `endDateInLong` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkPriceVOPriceTableVO` (`tableUuid`),
  CONSTRAINT `fkPriceVOPriceTableVO` FOREIGN KEY (`tableUuid`) REFERENCES `PriceTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageCapacityVO` (
  `uuid` varchar(32) NOT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT '0',
  `availableCapacity` bigint(20) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `totalPhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `availablePhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `systemUsedCapacity` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxPrimaryStorageCapacityVOtotalCapacity` (`totalCapacity`),
  KEY `idxPrimaryStorageCapacityVOavailableCapacity` (`availableCapacity`),
  CONSTRAINT `fkPrimaryStorageCapacityVOPrimaryStorageEO` FOREIGN KEY (`uuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageClusterRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
  `clusterUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uqPrimaryStorageClusterRefVO` (`primaryStorageUuid`,`clusterUuid`),
  KEY `fkPrimaryStorageClusterRefVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkPrimaryStorageClusterRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPrimaryStorageClusterRefVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `zoneUuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL COMMENT 'primary storage name',
  `url` varchar(2048) NOT NULL,
  `mountPath` varchar(2048) NOT NULL,
  `description` varchar(2048) DEFAULT NULL COMMENT 'primary storage description',
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkPrimaryStorageEOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkPrimaryStorageEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageHistoricalUsageBaseVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `resourceType` varchar(32) NOT NULL,
  `totalPhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `usedPhysicalCapacity` bigint(20) unsigned DEFAULT '0',
  `recordDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `primaryStorageUuid` (`primaryStorageUuid`),
  KEY `resourceUuid` (`resourceUuid`),
  KEY `resourceType` (`resourceType`),
  CONSTRAINT `fkUsageVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageHostRefVO` (
  `primaryStorageUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `primaryStorageUuid` (`primaryStorageUuid`,`hostUuid`),
  KEY `fkPrimaryStorageHostRefVOHostEO` (`hostUuid`),
  CONSTRAINT `fkPrimaryStorageHostRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPrimaryStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PrimaryStorageOutputProtocolRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `primaryStorageUuid` varchar(32) NOT NULL,
  `outputProtocol` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPrimaryStorageOutputProtocolRefVOExternalPrimaryStorageVO` (`primaryStorageUuid`),
  CONSTRAINT `fkPrimaryStorageOutputProtocolRefVOExternalPrimaryStorageVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `ExternalPrimaryStorageVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthInBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipIp` varchar(255) DEFAULT NULL,
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthOutBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipIp` varchar(255) DEFAULT NULL,
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipUuid` varchar(32) NOT NULL,
  `vipName` varchar(255) DEFAULT NULL,
  `vipIp` varchar(128) NOT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `vipStatus` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVipBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxPubIpVipBandwidthUsageVOvipUuid` (`accountUuid`,`dateInLong`,`vipUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipUuid` varchar(32) NOT NULL,
  `vipName` varchar(255) DEFAULT NULL,
  `vipIp` varchar(128) NOT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `vipStatus` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVipBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthInBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicIp` varchar(255) DEFAULT NULL,
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthOutBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicIp` varchar(255) DEFAULT NULL,
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `vmNicIp` varchar(128) DEFAULT NULL,
  `vmNicStatus` varchar(64) NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVmNicBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxPubIpVmNicBandwidthUsageVOvmNicUuid` (`accountUuid`,`dateInLong`,`vmNicUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `vmNicIp` varchar(128) DEFAULT NULL,
  `vmNicStatus` varchar(64) NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVmNicBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PublishAppResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `appUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkPublishAppResourceRefVOPublishAppVO` (`appUuid`),
  KEY `fkPublishAppResourceRefVOResourceVO` (`resourceUuid`),
  CONSTRAINT `fkPublishAppResourceRefVOPublishAppVO` FOREIGN KEY (`appUuid`) REFERENCES `PublishAppVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkPublishAppResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PublishAppVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `buildAppUuid` varchar(32) DEFAULT NULL,
  `templateContent` mediumtext NOT NULL,
  `appMetaData` mediumtext NOT NULL,
  `preParams` text,
  `appId` varchar(255) NOT NULL,
  `version` varchar(127) NOT NULL,
  `type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `vmRelationship` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkPublishAppVOBuildApplicationVO` (`buildAppUuid`),
  CONSTRAINT `fkPublishAppVOBuildApplicationVO` FOREIGN KEY (`buildAppUuid`) REFERENCES `BuildApplicationVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `QuartzJdbcJobVO` (
  `uuid` varchar(32) NOT NULL,
  `groupName` varchar(255) NOT NULL,
  `managementNodeId` varchar(128) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `QuotaVO` (
  `name` varchar(255) NOT NULL,
  `identityUuid` varchar(32) DEFAULT NULL,
  `identityType` varchar(255) NOT NULL,
  `value` bigint(20) DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxIdentityUuid` (`identityUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RaidControllerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `sasAddress` varchar(255) DEFAULT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `productName` varchar(255) DEFAULT NULL,
  `adapterNumber` smallint(6) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkRaidControllerVOHostVO` (`hostUuid`),
  CONSTRAINT `fkRaidControllerVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RaidPhysicalDriveVO` (
  `uuid` varchar(32) NOT NULL,
  `raidControllerUuid` varchar(32) NOT NULL,
  `raidLevel` varchar(32) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `deviceModel` varchar(255) DEFAULT NULL,
  `enclosureDeviceId` smallint(6) NOT NULL,
  `slotNumber` smallint(6) NOT NULL,
  `deviceId` smallint(6) DEFAULT NULL,
  `diskGroup` smallint(6) DEFAULT NULL,
  `wwn` varchar(255) DEFAULT NULL,
  `serialNumber` varchar(255) DEFAULT NULL,
  `size` bigint(20) NOT NULL,
  `driveState` varchar(255) DEFAULT NULL,
  `locateStatus` varchar(32) DEFAULT NULL,
  `driveType` varchar(255) DEFAULT NULL,
  `mediaType` varchar(255) DEFAULT NULL,
  `rotationRate` smallint(6) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkRaidPhysicalDriveVORaidControllerVO` (`raidControllerUuid`),
  CONSTRAINT `fkRaidPhysicalDriveVORaidControllerVO` FOREIGN KEY (`raidControllerUuid`) REFERENCES `RaidControllerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RegisterLicenseApplicationVO` (
  `appId` varchar(32) NOT NULL,
  `licenseRequestCode` text NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`appId`),
  UNIQUE KEY `appId` (`appId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RemoteVtepVO` (
  `uuid` varchar(32) NOT NULL,
  `vtepIp` varchar(32) NOT NULL,
  `port` int(11) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukRemoteVtepIpPoolUuidClusterUuid` (`vtepIp`,`poolUuid`,`clusterUuid`) USING BTREE,
  KEY `fkRemoteVtepVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkRemoteVtepVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RemovalInstanceRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `AdjustmentType` varchar(256) NOT NULL,
  `adjustmentValue` int(10) NOT NULL,
  `removalPolicy` varchar(256) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ReplayMessageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `msgDump` text,
  `locationType` varchar(256) NOT NULL,
  `locationUuid` varchar(32) NOT NULL,
  `groupUuid` varchar(32) DEFAULT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `manageJobUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ReservedIpRangeVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `ipVersion` int(10) unsigned DEFAULT '4' COMMENT 'ip range version',
  `startIp` varchar(64) NOT NULL COMMENT 'start ip',
  `endIp` varchar(64) NOT NULL COMMENT 'end ip',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkReservedIpRangeVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkReservedIpRangeVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceAttributeConstraintVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `keyUuid` char(32) NOT NULL,
  `type` varchar(255) NOT NULL,
  `parameter` varchar(2048) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkResourceAttributeConstraintVOResourceAttributeKeyVO` (`keyUuid`),
  CONSTRAINT `fkResourceAttributeConstraintVOResourceAttributeKeyVO` FOREIGN KEY (`keyUuid`) REFERENCES `ResourceAttributeKeyVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceAttributeKeyResourceTypeVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `keyUuid` char(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uqResourceAttributeKeyResourceTypeVO` (`keyUuid`,`resourceType`),
  CONSTRAINT `fkResourceAttributeKeyResourceTypeVOResourceAttributeKeyVO` FOREIGN KEY (`keyUuid`) REFERENCES `ResourceAttributeKeyVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceAttributeKeyVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceAttributeValueVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `keyUuid` char(32) NOT NULL,
  `value` varchar(2048) NOT NULL,
  `resourceUuid` char(32) NOT NULL,
  `resourceType` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkResourceAttributeValueVOResourceAttributeKeyVO` (`keyUuid`),
  KEY `fkResourceAttributeValueVOResourceVO` (`resourceUuid`),
  CONSTRAINT `fkResourceAttributeValueVOResourceAttributeKeyVO` FOREIGN KEY (`keyUuid`) REFERENCES `ResourceAttributeKeyVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkResourceAttributeValueVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceConfigVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `category` varchar(64) NOT NULL,
  `value` text NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(256) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `resourceUuid` (`resourceUuid`,`category`,`name`),
  CONSTRAINT `fkResourceConfigVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceDirectoryRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` varchar(32) NOT NULL,
  `directoryUuid` varchar(32) NOT NULL,
  `resourceType` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkResourceDirectoryRefVOResourceVO` (`resourceUuid`),
  KEY `fkResourceDirectoryRefVODirectoryVO` (`directoryUuid`),
  CONSTRAINT `fkResourceDirectoryRefVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkResourceDirectoryRefVO1` FOREIGN KEY (`directoryUuid`) REFERENCES `DirectoryVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceStackVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `version` varchar(32) NOT NULL,
  `templateContent` text NOT NULL,
  `paramContent` text,
  `status` varchar(32) NOT NULL,
  `reason` varchar(2048) DEFAULT NULL,
  `enableRollback` tinyint(1) NOT NULL DEFAULT '1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `outputs` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceStackVmPortRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `stackUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkResourceStackVmPortRefVOResourceStackVO` (`stackUuid`),
  KEY `fkResourceStackVmPortRefVOVmInstanceVO` (`vmInstanceUuid`),
  CONSTRAINT `fkResourceStackVmPortRefVOResourceStackVO` FOREIGN KEY (`stackUuid`) REFERENCES `ResourceStackVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkResourceStackVmPortRefVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ResourceVO` (
  `uuid` varchar(32) NOT NULL,
  `resourceName` varchar(255) DEFAULT NULL,
  `resourceType` varchar(255) DEFAULT NULL,
  `concreteResourceType` varchar(512) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `ResourceVO` VALUE
('12a087c058cc45d5bf80a605f17c0083','global_default','PriceTableVO','org.zstack.billing.table.PriceTableVO'),
('3b933e9aaf2d49b9a3dcf0c92867790f','CREATE_VM_INSTANCE_TICKET_TYPE','TicketTypeVO','org.zstack.ticket.entity.TicketTypeVO'),
('ce88cd128c4011f19babfa4856ea8e00','zstack.affinity.group.for.virtual.router','AffinityGroupVO','org.zstack.header.affinitygroup.AffinityGroupVO'),
('d3612d528c4011f19babfa4856ea8e00','zstack.affinity.group.for.virtual.router','VmSchedulingRuleGroupVO','org.zstack.header.vmscheduling.VmSchedulingRuleGroupVO');

CREATE TABLE `RoleAccountRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `roleUuid` char(32) NOT NULL,
  `accountUuid` char(32) NOT NULL,
  `accountPermissionFrom` char(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkRoleAccountRefRoleUuid` (`roleUuid`),
  KEY `fkRoleAccountRefAccountUuid` (`accountUuid`),
  KEY `fkRoleAccountRefAccountPermissionFrom` (`accountPermissionFrom`),
  CONSTRAINT `fkRoleAccountRefRoleUuid` FOREIGN KEY (`roleUuid`) REFERENCES `RoleVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkRoleAccountRefAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkRoleAccountRefAccountPermissionFrom` FOREIGN KEY (`accountPermissionFrom`) REFERENCES `AccountGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RolePolicyResourceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rolePolicyId` bigint(20) unsigned NOT NULL,
  `effect` varchar(32) NOT NULL DEFAULT 'Allow',
  `resourceUuid` char(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkRolePolicyResourceRefRolePolicyId` (`rolePolicyId`),
  CONSTRAINT `fkRolePolicyResourceRefRolePolicyId` FOREIGN KEY (`rolePolicyId`) REFERENCES `RolePolicyVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RolePolicyVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `roleUuid` char(32) NOT NULL,
  `actions` varchar(255) NOT NULL,
  `effect` varchar(32) NOT NULL,
  `resourceType` varchar(255) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkRolePolicyRoleUuid` (`roleUuid`),
  KEY `idxRolePolicyActions` (`actions`),
  CONSTRAINT `fkRolePolicyRoleUuid` FOREIGN KEY (`roleUuid`) REFERENCES `RoleVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RoleVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeUsageExtensionVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `fkRootVolumeUsageExtensionVORootVolumeUsageVO` FOREIGN KEY (`id`) REFERENCES `RootVolumeUsageVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxRootVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxRootVolumeUsageVOvolumeUuid` (`accountUuid`,`dateInLong`,`volumeUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxRootVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RouterAreaVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'area uuid',
  `areaId` varchar(64) NOT NULL COMMENT 'area id 32bit with IPv4 address style',
  `type` varchar(16) NOT NULL DEFAULT 'Standard',
  `authentication` varchar(16) NOT NULL DEFAULT 'None',
  `password` varchar(16) DEFAULT NULL,
  `keyId` int(10) unsigned DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSApplicationEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  `platformUuid` varchar(32) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ownerType` varchar(32) DEFAULT 'Customized',
  `connectionStatus` varchar(10) DEFAULT 'UP',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSNSApplicationEndpointVOSNSApplicationPlatformVO` (`platformUuid`),
  CONSTRAINT `fkSNSApplicationEndpointVOSNSApplicationPlatformVO` FOREIGN KEY (`platformUuid`) REFERENCES `SNSApplicationPlatformVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSApplicationPlatformVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSDingTalkAtPersonVO` (
  `uuid` varchar(32) NOT NULL,
  `phoneNumber` varchar(64) NOT NULL,
  `endpointUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `remark` varchar(128) DEFAULT '',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSNSDingTalkAtPersonVOSNSDingTalkEndpointVO` (`endpointUuid`),
  CONSTRAINT `fkSNSDingTalkAtPersonVOSNSDingTalkEndpointVO` FOREIGN KEY (`endpointUuid`) REFERENCES `SNSDingTalkEndpointVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSDingTalkEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `atAll` int(1) unsigned NOT NULL,
  `secret` varchar(128) DEFAULT '',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSDingTalkEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSEmailAddressVO` (
  `uuid` varchar(32) NOT NULL,
  `emailAddress` varchar(1024) NOT NULL,
  `endpointUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSEmailEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  `email` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSEmailEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSEmailPlatformVO` (
  `uuid` varchar(32) NOT NULL,
  `smtpServer` varchar(255) NOT NULL,
  `smtpPort` int(10) unsigned NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSEmailPlatformVOSNSApplicationPlatformVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationPlatformVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSEndpointThirdpartyAlertHistoryVO` (
  `endpointUuid` varchar(32) NOT NULL,
  `alertUuid` varchar(32) NOT NULL,
  `subscriptionUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  KEY `idxEndpointUuid` (`endpointUuid`),
  KEY `idxSubscriptionUuid` (`subscriptionUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSFeiShuAtPersonVO` (
  `uuid` char(32) NOT NULL,
  `userId` varchar(64) NOT NULL,
  `endpointUuid` char(32) NOT NULL,
  `remark` varchar(128) DEFAULT '',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSFeiShuEndpointVO` (
  `uuid` char(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `atAll` int(1) unsigned NOT NULL,
  `secret` varchar(128) DEFAULT '',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSHttpEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `username` varchar(512) DEFAULT NULL,
  `password` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSHttpEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSMicrosoftTeamsEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSMicrosoftTeamsEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSSmsEndpointVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSNSSmsEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`uuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSSmsReceiverVO` (
  `uuid` varchar(32) NOT NULL,
  `phoneNumber` varchar(24) NOT NULL,
  `endpointUuid` varchar(32) NOT NULL,
  `type` varchar(24) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSNSSmsReceiverVOSNSSmsEndpointVO` (`endpointUuid`),
  CONSTRAINT `fkSNSSmsReceiverVOSNSSmsEndpointVO` FOREIGN KEY (`endpointUuid`) REFERENCES `SNSSmsEndpointVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSSnmpPlatformVO` (
  `uuid` varchar(32) NOT NULL,
  `snmpAddress` varchar(128) NOT NULL,
  `snmpPort` smallint(5) unsigned NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `ukipAddrPort` (`snmpAddress`,`snmpPort`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSSubscriberVO` (
  `topicUuid` varchar(32) NOT NULL,
  `endpointUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`topicUuid`,`endpointUuid`),
  KEY `fkSNSSubscriberVOSNSApplicationEndpointVO` (`endpointUuid`),
  CONSTRAINT `fkSNSSubscriberVOSNSTopicVO` FOREIGN KEY (`topicUuid`) REFERENCES `SNSTopicVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSNSSubscriberVOSNSApplicationEndpointVO` FOREIGN KEY (`endpointUuid`) REFERENCES `SNSApplicationEndpointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSTextTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `applicationPlatformType` varchar(128) NOT NULL,
  `template` text NOT NULL,
  `defaultTemplate` int(1) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `recoveryTemplate` text,
  `type` varchar(255) DEFAULT 'ALARM',
  `subject` varchar(2048) DEFAULT NULL,
  `recoverySubject` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSTopicVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(64) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ownerType` varchar(32) DEFAULT 'Customized',
  `locale` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSWeComAtPersonVO` (
  `uuid` char(32) NOT NULL,
  `userId` varchar(64) NOT NULL,
  `endpointUuid` char(32) NOT NULL,
  `remark` varchar(128) DEFAULT '',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SNSWeComEndpointVO` (
  `uuid` char(32) NOT NULL,
  `url` varchar(1024) NOT NULL,
  `atAll` int(1) unsigned NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SSORedirectTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `clientUuid` varchar(255) NOT NULL,
  `redirectTemplate` varchar(2048) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSSORedirectTemplateVOThirdPartyAccountSourceVO` (`clientUuid`),
  CONSTRAINT `fkSSORedirectTemplateVOThirdPartyAccountSourceVO` FOREIGN KEY (`clientUuid`) REFERENCES `ThirdPartyAccountSourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SSOTokenVO` (
  `uuid` varchar(32) NOT NULL,
  `clientUuid` varchar(32) DEFAULT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSSOTokenVOThirdPartyAccountSourceVO` (`clientUuid`),
  KEY `idxSSOTokenVOUserUuid` (`userUuid`),
  CONSTRAINT `fkSSOTokenVOThirdPartyAccountSourceVO` FOREIGN KEY (`clientUuid`) REFERENCES `ThirdPartyAccountSourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobGroupJobRefVO` (
  `schedulerJobUuid` varchar(32) NOT NULL,
  `schedulerJobGroupUuid` varchar(32) NOT NULL,
  `priority` int(11) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`schedulerJobUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobGroupSchedulerTriggerRefVO` (
  `schedulerJobGroupUuid` varchar(32) NOT NULL,
  `schedulerTriggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`schedulerJobGroupUuid`,`schedulerTriggerUuid`),
  KEY `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerTriggerVO` (`schedulerTriggerUuid`),
  CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerJobGroupVO` FOREIGN KEY (`schedulerJobGroupUuid`) REFERENCES `SchedulerJobGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `jobType` varchar(32) DEFAULT NULL,
  `jobClassName` varchar(255) DEFAULT NULL,
  `jobData` text,
  `state` varchar(255) DEFAULT NULL,
  `zoneUuid` varchar(32) DEFAULT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSchedulerJobGroupVOManagementNodeVO` (`managementNodeUuid`),
  CONSTRAINT `fkSchedulerJobGroupVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `triggerUuid` varchar(32) DEFAULT NULL,
  `schedulerJobUuid` varchar(32) NOT NULL,
  `schedulerJobGroupUuid` varchar(32) DEFAULT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `startTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `executeTime` bigint(20) DEFAULT NULL,
  `requestDump` text,
  `resultDump` text,
  `success` tinyint(1) NOT NULL DEFAULT '0',
  `jobType` varchar(255) DEFAULT NULL,
  `fireInstanceId` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxSchedulerJobHistoryVOTriggerUuid` (`triggerUuid`),
  KEY `idxSchedulerJobHistoryVOSchedulerJobUuid` (`schedulerJobUuid`),
  KEY `idxSchedulerJobHistoryVOSchedulerJobGroupUuid` (`schedulerJobGroupUuid`),
  KEY `idxSchedulerJobHistoryVOTargetResourceUuid` (`targetResourceUuid`),
  KEY `idxSchedulerJobHistoryVOStartTime` (`startTime`),
  KEY `idxSchedulerJobHistoryVOFireInstanceId` (`fireInstanceId`),
  KEY `idxSchedulerJobHistoryVOExecuteTime` (`executeTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobSchedulerTriggerRefVO` (
  `uuid` varchar(32) NOT NULL,
  `schedulerJobUuid` varchar(32) NOT NULL,
  `schedulerTriggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSchedulerJobSchedulerTriggerRefVOSchedulerJobVO` (`schedulerJobUuid`),
  KEY `fkSchedulerJobSchedulerTriggerRefVOSchedulerTriggerVO` (`schedulerTriggerUuid`),
  CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerJobVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`),
  CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerJobVO` (
  `uuid` varchar(32) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `jobClassName` varchar(255) DEFAULT NULL,
  `jobData` text,
  `state` varchar(255) DEFAULT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSchedulerJobVOManagementNodeVO` (`managementNodeUuid`),
  CONSTRAINT `fkSchedulerJobVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerTriggerVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `schedulerType` varchar(255) NOT NULL,
  `schedulerInterval` int(10) unsigned DEFAULT NULL,
  `repeatCount` int(10) unsigned DEFAULT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `startTime` timestamp NULL DEFAULT '0000-00-00 00:00:00',
  `stopTime` timestamp NULL DEFAULT '0000-00-00 00:00:00',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `cron` varchar(255) DEFAULT NULL COMMENT 'interval in cron format',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSchedulerTriggerVOManagementNodeVO` (`managementNodeUuid`),
  CONSTRAINT `fkSchedulerTriggerVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SchedulerVO` (
  `uuid` varchar(32) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `schedulerName` varchar(255) NOT NULL,
  `schedulerDescription` varchar(2048) DEFAULT NULL,
  `schedulerType` varchar(255) NOT NULL,
  `schedulerInterval` int(10) unsigned DEFAULT NULL,
  `repeatCount` int(10) unsigned DEFAULT NULL,
  `cronScheduler` varchar(255) DEFAULT NULL,
  `jobName` varchar(255) DEFAULT NULL,
  `jobGroup` varchar(255) DEFAULT NULL,
  `triggerName` varchar(255) DEFAULT NULL,
  `triggerGroup` varchar(255) DEFAULT NULL,
  `jobClassName` varchar(255) DEFAULT NULL,
  `jobData` text,
  `state` varchar(128) DEFAULT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `startTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `stopTime` timestamp NULL DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `schedulerJob` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSchedulerVOManagementNodeVO` (`managementNodeUuid`),
  CONSTRAINT `fkSchedulerVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ScsiLunHostRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `scsiLunUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `hctl` varchar(64) DEFAULT NULL,
  `path` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkScsiLunHostRefVOScsiLunVO` (`scsiLunUuid`),
  KEY `fkScsiLunHostRefVOHostVO` (`hostUuid`),
  CONSTRAINT `fkScsiLunHostRefVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkScsiLunHostRefVOScsiLunVO` FOREIGN KEY (`scsiLunUuid`) REFERENCES `LunVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ScsiLunVmInstanceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `scsiLunUuid` varchar(32) NOT NULL,
  `deviceId` int(10) unsigned DEFAULT NULL,
  `attachMultipath` tinyint(1) NOT NULL DEFAULT '1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkScsiLunVmInstanceRefVOScsiLunVO` (`scsiLunUuid`),
  KEY `fkScsiLunVmInstanceRefVOVmInstanceVO` (`vmInstanceUuid`),
  CONSTRAINT `fkScsiLunVmInstanceRefVOScsiLunVO` FOREIGN KEY (`scsiLunUuid`) REFERENCES `LunVO` (`uuid`),
  CONSTRAINT `fkScsiLunVmInstanceRefVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SdnControllerVO` (
  `uuid` varchar(32) NOT NULL,
  `vendorType` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `ip` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecretResourcePoolVO` (
  `uuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `state` varchar(32) NOT NULL,
  `model` varchar(32) NOT NULL,
  `heartbeatInterval` int(10) unsigned DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `status` varchar(32) NOT NULL DEFAULT 'Connected',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxSecretResourcePoolVOUuid` (`uuid`),
  KEY `fkSecretResourcePoolVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkSecretResourcePoolVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityGroupFailureHostVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `hostUuid` varchar(32) NOT NULL,
  `securityGroupUuid` varchar(32) DEFAULT NULL,
  `managementNodeId` varchar(128) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkSecurityGroupFailureHostVOHostEO` (`hostUuid`),
  KEY `fkSecurityGroupFailureHostVOManagementNodeVO` (`managementNodeId`),
  CONSTRAINT `fkSecurityGroupFailureHostVOManagementNodeVO` FOREIGN KEY (`managementNodeId`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkSecurityGroupFailureHostVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityGroupL3NetworkRefVO` (
  `uuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `securityGroupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uqSecurityGroupL3NetworkRefVO` (`l3NetworkUuid`,`securityGroupUuid`),
  KEY `fkSecurityGroupL3NetworkRefVOSecurityGroupVO` (`securityGroupUuid`),
  CONSTRAINT `fkSecurityGroupL3NetworkRefVOSecurityGroupVO` FOREIGN KEY (`securityGroupUuid`) REFERENCES `SecurityGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSecurityGroupL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityGroupRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `securityGroupUuid` varchar(32) NOT NULL,
  `type` varchar(255) NOT NULL,
  `protocol` varchar(255) NOT NULL,
  `priority` int(11) DEFAULT '-1',
  `action` varchar(32) NOT NULL DEFAULT 'ACCEPT',
  `description` varchar(255) DEFAULT NULL,
  `dstIpRange` varchar(1024) DEFAULT NULL,
  `srcIpRange` varchar(1024) DEFAULT NULL,
  `dstPortRange` varchar(255) DEFAULT NULL,
  `srcPortRange` varchar(255) DEFAULT NULL,
  `allowedCidr` varchar(255) NOT NULL,
  `startPort` int(11) NOT NULL,
  `endPort` int(11) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `remoteSecurityGroupUuid` varchar(255) DEFAULT NULL,
  `ipVersion` int(10) unsigned NOT NULL DEFAULT '4',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSecurityGroupRuleVOSecurityGroupVO` (`securityGroupUuid`),
  KEY `fkSecurityGroupRuleVORemoteSecurityGroupVO` (`remoteSecurityGroupUuid`),
  CONSTRAINT `fkSecurityGroupRuleVORemoteSecurityGroupVO` FOREIGN KEY (`remoteSecurityGroupUuid`) REFERENCES `SecurityGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSecurityGroupRuleVOSecurityGroupVO` FOREIGN KEY (`securityGroupUuid`) REFERENCES `SecurityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityGroupSequenceNumberVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `internalId` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ipVersion` int(10) unsigned DEFAULT '4',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxSecurityGroupVOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SecurityMachineVO` (
  `uuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `secretResourcePoolUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `model` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `managementIp` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxSecurityMachineVOUuid` (`uuid`),
  KEY `idxSecurityMachineVOSecretResourcePoolUuid` (`secretResourcePoolUuid`),
  KEY `fkSecurityMachineVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkSecurityMachineVOSecretResourcePoolVO` FOREIGN KEY (`secretResourcePoolUuid`) REFERENCES `SecretResourcePoolVO` (`uuid`),
  CONSTRAINT `fkSecurityMachineVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SessionVO` (
  `uuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `expiredDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSessionVOAccountVO` (`accountUuid`),
  CONSTRAINT `fkSessionVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SftpBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `hostname` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `sshPort` int(10) unsigned DEFAULT '22',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `hostname` (`hostname`),
  CONSTRAINT `fkSftpBackupStorageVOBackupStorageEO` FOREIGN KEY (`uuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ShareableVolumeVmInstanceRefVO` (
  `uuid` varchar(32) NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(255) NOT NULL,
  `deviceId` int(10) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `volumeUuid` (`volumeUuid`,`vmInstanceUuid`),
  KEY `fkShareableVolumeVmInstanceRefVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkShareableVolumeVmInstanceRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkShareableVolumeVmInstanceRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SharedBlockCapacityVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'shared block uuid',
  `totalCapacity` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'total capacity of shared block in bytes',
  `availableCapacity` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'available capacity of shared block in bytes',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSharedBlockCapacityVOSharedBlockVO` FOREIGN KEY (`uuid`) REFERENCES `SharedBlockVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SharedBlockGroupPrimaryStorageHostRefVO` (
  `hostId` int(11) NOT NULL,
  `id` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SharedBlockGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `sharedBlockGroupType` varchar(128) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SharedBlockVO` (
  `uuid` varchar(32) NOT NULL,
  `sharedBlockGroupUuid` varchar(32) NOT NULL,
  `type` varchar(128) NOT NULL,
  `diskUuid` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(64) NOT NULL,
  `status` varchar(64) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `diskUuid` (`diskUuid`),
  KEY `fkSharedBlockVOSharedBlockGroupVO` (`sharedBlockGroupUuid`),
  CONSTRAINT `fkSharedBlockVOSharedBlockGroupVO` FOREIGN KEY (`sharedBlockGroupUuid`) REFERENCES `SharedBlockGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SharedResourceVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ownerAccountUuid` varchar(32) NOT NULL,
  `receiverAccountUuid` varchar(32) DEFAULT NULL,
  `toPublic` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `resourceType` varchar(256) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `permission` int(10) unsigned DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `ownerAccountUuid` (`ownerAccountUuid`,`receiverAccountUuid`,`resourceUuid`,`toPublic`),
  KEY `fkSharedResourceVOAccountVO1` (`receiverAccountUuid`),
  KEY `fkSharedResourceVOResourceVO` (`resourceUuid`),
  KEY `idxToPublic` (`toPublic`),
  CONSTRAINT `fkSharedResourceVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSharedResourceVOAccountVO` FOREIGN KEY (`ownerAccountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSharedResourceVOAccountVO1` FOREIGN KEY (`receiverAccountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SimulatorHostVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `memoryCapacity` bigint(20) unsigned NOT NULL COMMENT 'total memory of host in bytes',
  `cpuCapacity` bigint(20) unsigned NOT NULL COMMENT 'total cpu of host in HZ',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkSimulatorHostVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SlbGroupL3NetworkRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `slbGroupUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `type` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkSlbGroupL3NetworkRefVOSlbGroupVO` (`slbGroupUuid`),
  KEY `fkSlbGroupL3NetworkRefVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkSlbGroupL3NetworkRefVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSlbGroupL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SlbGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `backendType` varchar(255) NOT NULL,
  `deployType` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `slbOfferingUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSlbGroupVOSlbOfferingVO` (`slbOfferingUuid`),
  CONSTRAINT `fkSlbGroupVOSlbOfferingVO` FOREIGN KEY (`slbOfferingUuid`) REFERENCES `SlbOfferingVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SlbLoadBalancerVO` (
  `uuid` varchar(32) NOT NULL,
  `slbGroupUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSlbLoadBalancerVOSlbGroupVO` (`slbGroupUuid`),
  CONSTRAINT `fkSlbLoadBalancerVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SlbOfferingVO` (
  `uuid` varchar(32) NOT NULL,
  `managementNetworkUuid` varchar(32) NOT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSlbOfferingVOL3NetworkEO` (`managementNetworkUuid`),
  CONSTRAINT `fkSlbOfferingVOInstanceOfferingEO` FOREIGN KEY (`uuid`) REFERENCES `InstanceOfferingEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSlbOfferingVOL3NetworkEO` FOREIGN KEY (`managementNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SlbVmInstanceVO` (
  `uuid` varchar(32) NOT NULL,
  `slbGroupUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkSlbVmInstanceVOSlbGroupVO` (`slbGroupUuid`),
  CONSTRAINT `fkSlbVmInstanceVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkSlbVmInstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SnapShotUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `SnapshotUuid` varchar(32) NOT NULL,
  `SnapshotStatus` varchar(64) NOT NULL,
  `SnapshotName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `SnapshotSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxSnapShotUsageVOaccountUuid` (`accountUuid`,`dateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SnmpAgentVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `version` varchar(32) NOT NULL COMMENT 'snmp authentication version',
  `readCommunity` varchar(32) DEFAULT NULL,
  `userName` varchar(32) DEFAULT NULL,
  `authAlgorithm` varchar(32) DEFAULT NULL,
  `authPassword` varchar(32) DEFAULT NULL,
  `privacyAlgorithm` varchar(32) DEFAULT NULL,
  `privacyPassword` varchar(32) DEFAULT NULL,
  `status` varchar(32) NOT NULL COMMENT 'SNMP agent status. status is enable which means start snmp with mn start.',
  `port` int(10) DEFAULT NULL,
  `securityLevel` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SoftwarePackageVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `hostUuid` char(32) DEFAULT NULL,
  `managementNodeUuid` char(32) DEFAULT NULL,
  `installPath` varchar(2048) DEFAULT NULL,
  `unzipInstallPath` varchar(2048) DEFAULT NULL,
  `type` varchar(1024) DEFAULT NULL,
  `md5sum` char(32) DEFAULT NULL,
  `status` char(32) DEFAULT NULL,
  `size` bigint(20) unsigned DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SshKeyPairRefVO` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resourceUuid` varchar(32) NOT NULL,
  `sshKeyPairUuid` varchar(32) NOT NULL,
  `resourceType` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkSshKeyPairRefVOVmInstanceEO` (`resourceUuid`),
  KEY `fkSshKeyPairRefVOSshKey` (`sshKeyPairUuid`),
  CONSTRAINT `fkSshKeyPairRefVOVmInstanceEO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkSshKeyPairRefVOSshKey` FOREIGN KEY (`sshKeyPairUuid`) REFERENCES `SshKeyPairVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SshKeyPairVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `publicKey` varchar(4096) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `StackTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `version` varchar(32) NOT NULL,
  `state` tinyint(1) unsigned DEFAULT '1',
  `content` mediumtext NOT NULL,
  `md5sum` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `SystemTagVO` (
  `uuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(64) NOT NULL,
  `inherent` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `type` varchar(32) NOT NULL,
  `tag` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxSystemTagVOresourceUuid` (`resourceUuid`),
  KEY `idxSystemTagVOresourceType` (`resourceType`),
  KEY `idxSystemTagVOtag` (`tag`(128)),
  KEY `idxSystemTagVOtype` (`type`),
  CONSTRAINT `fkSystemTagVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TagPatternVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `value` varchar(128) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `color` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `idxTagPatternVOName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TaskProgressVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `apiId` char(32) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `opaque` text,
  `createTime` bigint(20) unsigned NOT NULL,
  `lastOpTime` bigint(20) unsigned NOT NULL,
  `currentStep` bigint(20) unsigned DEFAULT '0',
  `totalStep` bigint(20) unsigned DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idxTaskProgressVOApiId` (`apiId`),
  KEY `idxTaskProgressVOLastOpTime` (`lastOpTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplateConfigVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `category` varchar(64) NOT NULL,
  `templateUuid` varchar(32) NOT NULL,
  `defaultValue` text,
  `value` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `GlobalConfigTemplateVOTemplateConfigVO` (`templateUuid`),
  CONSTRAINT `GlobalConfigTemplateVOTemplateConfigVO` FOREIGN KEY (`templateUuid`) REFERENCES `GlobalConfigTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplateCustomParamVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `templateUuid` varchar(32) NOT NULL,
  `param` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkTemplateCustomParamVOPreconfigurationTemplateVO` (`templateUuid`),
  CONSTRAINT `fkTemplateCustomParamVOPreconfigurationTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `PreconfigurationTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplatedVmInstanceCacheVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `templatedVmInstanceUuid` char(32) NOT NULL,
  `cacheVmInstanceUuid` char(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplatedVmInstanceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `templatedVmInstanceUuid` char(32) NOT NULL,
  `vmInstanceUuid` char(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplatedVmInstanceVO` (
  `uuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ThirdPartyAccountSourceVO` (
  `uuid` char(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `createAccountStrategy` varchar(32) NOT NULL,
  `deleteAccountStrategy` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ThirdpartyOriginalAlertVO` (
  `uuid` varchar(32) NOT NULL,
  `thirdpartyPlatformUuid` varchar(32) NOT NULL,
  `product` varchar(255) NOT NULL,
  `service` varchar(255) DEFAULT NULL,
  `metric` varchar(512) DEFAULT NULL,
  `alertLevel` varchar(64) NOT NULL,
  `alertTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `dimensions` varchar(4096) DEFAULT NULL,
  `message` varchar(4096) NOT NULL,
  `dataSource` varchar(255) NOT NULL,
  `sourceText` text,
  `readStatus` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxThirdpartyPlatformUuid` (`thirdpartyPlatformUuid`),
  CONSTRAINT `fkThirdpartyAlertVOThirdpartyPlatformVO` FOREIGN KEY (`thirdpartyPlatformUuid`) REFERENCES `ThirdpartyPlatformVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ThirdpartyPlatformVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `url` varchar(512) NOT NULL,
  `template` varchar(4096) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastSyncDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketFlowCollectionVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `isDefault` tinyint(1) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `state` varchar(64) NOT NULL,
  `status` varchar(64) NOT NULL,
  `type` varchar(64) NOT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketFlowVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `collectionUuid` varchar(32) NOT NULL,
  `parentFlowUuid` varchar(32) DEFAULT NULL,
  `flowContext` text NOT NULL,
  `flowContextType` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketStatusHistoryVO` (
  `uuid` varchar(32) NOT NULL,
  `ticketUuid` varchar(32) NOT NULL,
  `fromStatus` varchar(255) NOT NULL,
  `toStatus` varchar(255) NOT NULL,
  `comment` text,
  `operatorUuid` varchar(32) NOT NULL,
  `operatorType` varchar(255) NOT NULL,
  `operationContext` text,
  `operationContextType` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `sequence` int(11) NOT NULL AUTO_INCREMENT,
  `flowName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `sequence` (`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketTypeTicketFlowCollectionRefVO` (
  `ticketTypeUuid` varchar(32) NOT NULL,
  `ticketFlowCollectionUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`ticketTypeUuid`,`ticketFlowCollectionUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketTypeVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `requests` varchar(2048) NOT NULL,
  `type` varchar(255) NOT NULL,
  `adminOnly` tinyint(1) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `TicketTypeVO` VALUE
('3b933e9aaf2d49b9a3dcf0c92867790f','CREATE_VM_INSTANCE_TICKET_TYPE',NULL,'','CREATE_VM_INSTANCE_TICKET_TYPE',0,'2026-07-30 18:02:23','2026-07-30 18:02:23');

CREATE TABLE `TicketVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` text,
  `status` varchar(255) NOT NULL,
  `accountSystemType` varchar(255) NOT NULL,
  `accountSystemContext` text,
  `requests` text NOT NULL,
  `flowCollectionUuid` varchar(32) NOT NULL,
  `currentFlowUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ticketTypeUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkTicketVOTicketTypeVO` (`ticketTypeUuid`),
  CONSTRAINT `fkTicketVOTicketTypeVO` FOREIGN KEY (`ticketTypeUuid`) REFERENCES `TicketTypeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TpmVO` (
  `uuid` char(32) NOT NULL,
  `vmInstanceUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkTpmVOVmInstanceVO` (`vmInstanceUuid`),
  CONSTRAINT `fkTpmVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TwoFactorAuthenticationSecretVO` (
  `uuid` varchar(32) NOT NULL,
  `secret` varchar(2048) NOT NULL,
  `accountUuid` char(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `status` varchar(255) NOT NULL DEFAULT 'NewCreated',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UKeyLicenseVO` (
  `keyId` varchar(32) NOT NULL,
  `managementNodeUuid` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `offline` bigint(20) unsigned NOT NULL,
  `online` bigint(20) unsigned NOT NULL,
  `recover` bigint(20) unsigned NOT NULL,
  `license` text NOT NULL,
  PRIMARY KEY (`keyId`),
  UNIQUE KEY `keyId` (`keyId`),
  KEY `idxUKeyLicenseVOmanagementNodeUuid` (`managementNodeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UpdateVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `voName` varchar(255) NOT NULL,
  `uuid` varchar(32) NOT NULL,
  `foreignVOName` varchar(255) DEFAULT NULL,
  `foreignVOUuid` varchar(32) DEFAULT NULL,
  `updateDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UplinkGroupVO` (
  `id` bigint(20) unsigned NOT NULL,
  `interfaceName` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `bondingUuid` varchar(32) DEFAULT NULL,
  `interfaceUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkUplinkGroupVOHostNetworkBondingVO` (`bondingUuid`),
  KEY `fkUplinkGroupVOHostNetworkInterfaceVO` (`interfaceUuid`),
  CONSTRAINT `fkUplinkGroupVOHostNetworkBondingVO` FOREIGN KEY (`bondingUuid`) REFERENCES `HostNetworkBondingVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkUplinkGroupVOHostNetworkInterfaceVO` FOREIGN KEY (`interfaceUuid`) REFERENCES `HostNetworkInterfaceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UsbDeviceVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `busNum` varchar(32) NOT NULL,
  `devNum` varchar(32) NOT NULL,
  `idVendor` varchar(32) NOT NULL,
  `idProduct` varchar(32) NOT NULL,
  `iManufacturer` varchar(1024) DEFAULT NULL,
  `iProduct` varchar(1024) DEFAULT NULL,
  `iSerial` varchar(1024) DEFAULT NULL,
  `usbVersion` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `attachType` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkUsbDeviceVOHostEO` (`hostUuid`),
  CONSTRAINT `fkUsbDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UsedIpVO` (
  `uuid` varchar(32) NOT NULL,
  `ipRangeUuid` varchar(32) DEFAULT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `ip` varchar(128) NOT NULL,
  `ipInLong` bigint(20) unsigned NOT NULL,
  `ipInBinary` varbinary(16) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `gateway` varchar(128) DEFAULT NULL,
  `netmask` varchar(128) DEFAULT NULL,
  `usedFor` varchar(128) DEFAULT NULL,
  `metaData` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `ipVersion` int(10) unsigned DEFAULT '4',
  `vmNicUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxUsedIpVOip` (`ip`),
  KEY `idxUsedIpVOipInLong` (`ipInLong`),
  KEY `idxUsedIpVOipInBinary` (`ipInBinary`),
  KEY `fkUsedIpVOIpRangeEO` (`ipRangeUuid`),
  KEY `fkUsedIpVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `fkUsedIpVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkUsedIpVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkUsedIpVOIpRangeEO` FOREIGN KEY (`ipRangeUuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkUsedIpVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `UserTagVO` (
  `uuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(64) NOT NULL,
  `type` varchar(32) NOT NULL,
  `tag` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `tagPatternUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxUserTagVOresourceUuid` (`resourceUuid`),
  KEY `idxUserTagVOresourceType` (`resourceType`),
  KEY `idxUserTagVOtag` (`tag`(128)),
  KEY `idxUserTagVOtype` (`type`),
  KEY `fkUserTagVOTagPatternVO` (`tagPatternUuid`),
  CONSTRAINT `fkUserTagVOTagPatternVO` FOREIGN KEY (`tagPatternUuid`) REFERENCES `TagPatternVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkUserTagVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `V2VConversionCacheVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `conversionHostUuid` varchar(32) NOT NULL,
  `srcVmUrl` varchar(255) NOT NULL,
  `installPath` varchar(255) NOT NULL,
  `deviceId` int(10) unsigned NOT NULL,
  `virtualSize` bigint(20) unsigned NOT NULL,
  `actualSize` bigint(20) unsigned NOT NULL,
  `bootMode` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `virtioScsi` tinyint(1) NOT NULL,
  `type` varchar(256) NOT NULL,
  `downloadTime` varchar(32) DEFAULT NULL,
  `uploadTime` varchar(32) DEFAULT NULL,
  `deviceName` varchar(32) DEFAULT NULL,
  `deviceAddress` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `V2VConversionHostVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `storagePath` varchar(2048) NOT NULL,
  `state` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `totalSize` bigint(20) unsigned NOT NULL DEFAULT '0',
  `availableSize` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkV2VConversionHostVOHostEO` (`hostUuid`),
  CONSTRAINT `fkV2VConversionHostVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterBackupStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
  `datastore` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVCenterBackupStorageVOVCenterVO` (`vCenterUuid`),
  CONSTRAINT `fkVCenterBackupStorageVOBackupStorageEO` FOREIGN KEY (`uuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVCenterBackupStorageVOVCenterVO` FOREIGN KEY (`vCenterUuid`) REFERENCES `VCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterClusterVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'vcenter cluster uuid',
  `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
  `morval` varchar(64) NOT NULL COMMENT 'MOR value',
  `dataCenterUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVCenterClusterVOVCenterVO` (`vCenterUuid`),
  KEY `fkVCenterDataCenterVOVCenterClusterVO` (`dataCenterUuid`),
  CONSTRAINT `fkVCenterDataCenterVOVCenterClusterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `VCenterDatacenterVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVCenterClusterVOClusterEO` FOREIGN KEY (`uuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVCenterClusterVOVCenterVO` FOREIGN KEY (`vCenterUuid`) REFERENCES `VCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterDatacenterVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'vcenter data-center uuid',
  `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
  `name` varchar(255) NOT NULL COMMENT 'data-center name',
  `morval` varchar(64) NOT NULL COMMENT 'MOR value',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVCenterDatacenterVOVCenterVO` (`vCenterUuid`),
  CONSTRAINT `fkVCenterDatacenterVOVCenterVO` FOREIGN KEY (`vCenterUuid`) REFERENCES `VCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterPrimaryStorageVO` (
  `uuid` varchar(32) NOT NULL,
  `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
  `datastore` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVCenterPrimaryStorageVOVCenterVO` (`vCenterUuid`),
  CONSTRAINT `fkVCenterPrimaryStorageVOPrimaryStorageEO` FOREIGN KEY (`uuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVCenterPrimaryStorageVOVCenterVO` FOREIGN KEY (`vCenterUuid`) REFERENCES `VCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterResourcePoolUsageVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool usage uuid',
  `vCenterResourcePoolUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
  `resourceUuid` varchar(32) NOT NULL COMMENT 'VCenter Resource resource uuid',
  `resourceType` varchar(256) NOT NULL COMMENT 'VCenter Resource resource type',
  `resourceName` varchar(256) DEFAULT NULL COMMENT 'VCenter Resource resource name',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `VCenterResourcePoolUsageVO` (`vCenterResourcePoolUuid`,`resourceUuid`) USING BTREE,
  CONSTRAINT `fkVCenterResourcePoolUsageVOVCenterResourcePoolVO` FOREIGN KEY (`vCenterResourcePoolUuid`) REFERENCES `VCenterResourcePoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterResourcePoolVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'VCenter Resource Pool uuid',
  `vCenterClusterUuid` varchar(32) NOT NULL COMMENT 'VCenter cluster uuid',
  `name` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool name',
  `morVal` varchar(256) NOT NULL COMMENT 'VCenter Resource Pool management object value in vcenter',
  `parentUuid` varchar(32) DEFAULT NULL COMMENT 'Parent Resource Pool uuid or NULL',
  `CPULimit` bigint(64) DEFAULT NULL,
  `CPUOverheadLimit` bigint(64) DEFAULT NULL,
  `CPUReservation` bigint(64) DEFAULT NULL,
  `CPUShares` bigint(64) DEFAULT NULL,
  `CPULevel` varchar(64) DEFAULT NULL,
  `MemoryLimit` bigint(64) DEFAULT NULL,
  `MemoryOverheadLimit` bigint(64) DEFAULT NULL,
  `MemoryReservation` bigint(64) DEFAULT NULL,
  `MemoryShares` bigint(64) DEFAULT NULL,
  `MemoryLevel` varchar(64) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `fkVCenterResourcePoolVOVCenterClusterVO` (`vCenterClusterUuid`),
  CONSTRAINT `fkVCenterResourcePoolVOVCenterClusterVO` FOREIGN KEY (`vCenterClusterUuid`) REFERENCES `VCenterClusterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VCenterVO` (
  `uuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `domainName` varchar(255) NOT NULL,
  `userName` varchar(255) NOT NULL,
  `password` varchar(1024) NOT NULL,
  `https` int(10) unsigned DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `port` int(11) DEFAULT NULL,
  `version` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVCenterVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkVCenterVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VRouterRouteEntryVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `description` varchar(2048) DEFAULT NULL,
  `routeTableUuid` varchar(32) NOT NULL,
  `destination` varchar(64) NOT NULL,
  `target` varchar(64) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `distance` int(10) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkRouteEntryVOVRouterRouteTableVO` (`routeTableUuid`),
  CONSTRAINT `fkRouteEntryVOVRouterRouteTableVO` FOREIGN KEY (`routeTableUuid`) REFERENCES `VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VRouterRouteTableVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VipNetworkServicesRefVO` (
  `uuid` varchar(32) NOT NULL,
  `serviceType` varchar(32) NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`,`serviceType`,`vipUuid`),
  KEY `fkVipNetworkServicesRefVOVipVO` (`vipUuid`),
  CONSTRAINT `fkVipNetworkServicesRefVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VipPeerL3NetworkRefVO` (
  `vipUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`vipUuid`,`l3NetworkUuid`),
  KEY `fkVipPeerL3NetworkRefVOL3NetworkEO` (`l3NetworkUuid`),
  CONSTRAINT `fkVipPeerL3NetworkRefVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVipPeerL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VipQosVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `vipUuid` varchar(32) NOT NULL,
  `port` int(16) unsigned DEFAULT NULL,
  `inboundBandwidth` bigint(20) unsigned DEFAULT NULL,
  `outboundBandwidth` bigint(20) unsigned DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVipQosVOVipVO` (`vipUuid`),
  CONSTRAINT `fkVipQosVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VipVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `ipRangeUuid` varchar(32) NOT NULL,
  `usedIpUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `ip` varchar(128) NOT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `netmask` varchar(128) DEFAULT NULL,
  `useFor` varchar(1024) DEFAULT NULL,
  `serviceProvider` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `prefixLen` int(10) unsigned DEFAULT NULL,
  `system` tinyint(3) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ipRangeUuid` (`ipRangeUuid`,`ip`),
  KEY `fkVipVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `idxVipVOname` (`name`),
  KEY `idxVipVOip` (`ip`),
  CONSTRAINT `fkVipVOIpRangeEO` FOREIGN KEY (`ipRangeUuid`) REFERENCES `IpRangeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVipVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualBorderRouterVO` (
  `uuid` varchar(32) NOT NULL,
  `vbrId` varchar(32) NOT NULL,
  `vlanInterfaceId` varchar(64) NOT NULL,
  `status` varchar(16) NOT NULL,
  `name` varchar(64) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `vlanId` varchar(64) NOT NULL,
  `circuitCode` varchar(32) NOT NULL,
  `localGatewayIp` varchar(32) NOT NULL,
  `peerGatewayIp` varchar(32) NOT NULL,
  `physicalConnectionStatus` varchar(32) NOT NULL,
  `peeringSubnetMask` varchar(32) NOT NULL,
  `physicalConnectionId` varchar(32) NOT NULL,
  `accessPointUuid` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVirtualBorderRouterVODataCenterVO` (`dataCenterUuid`),
  KEY `fkVirtualBorderRouterVOConnectionAccessPointVO` (`accessPointUuid`),
  CONSTRAINT `fkVirtualBorderRouterVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `ConnectionAccessPointVO` (`uuid`),
  CONSTRAINT `fkVirtualBorderRouterVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterBootstrapIsoVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `virtualRouterUuid` varchar(32) NOT NULL,
  `isoPath` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVirtualRouterBootstrapIsoVOVmInstanceEO` (`virtualRouterUuid`),
  CONSTRAINT `fkVirtualRouterBootstrapIsoVOVmInstanceEO` FOREIGN KEY (`virtualRouterUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterEipRefVO` (
  `eipUuid` varchar(32) NOT NULL,
  `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`eipUuid`),
  UNIQUE KEY `eipUuid` (`eipUuid`),
  KEY `fkVirtualRouterEipRefVOVmInstanceEO` (`virtualRouterVmUuid`),
  CONSTRAINT `fkVirtualRouterEipRefVOVmInstanceEO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterEipRefVOEipVO` FOREIGN KEY (`eipUuid`) REFERENCES `EipVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterLoadBalancerRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `virtualRouterVmUuid` varchar(32) NOT NULL,
  `loadBalancerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `virtualRouterVmUuid` (`virtualRouterVmUuid`,`loadBalancerUuid`),
  KEY `fkVirtualRouterLoadBalancerRefVOLoadBalancerVO` (`loadBalancerUuid`),
  CONSTRAINT `fkVirtualRouterLoadBalancerRefVOLoadBalancerVO` FOREIGN KEY (`loadBalancerUuid`) REFERENCES `LoadBalancerVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterLoadBalancerRefVOVirtualRouterVmVO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterMetadataVO` (
  `uuid` varchar(32) NOT NULL,
  `zvrVersion` varchar(32) DEFAULT NULL,
  `vyosVersion` varchar(32) DEFAULT NULL,
  `kernelVersion` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVirtualRouterMetadataVOVirtualRouterVmVO` FOREIGN KEY (`uuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterOfferingVO` (
  `uuid` varchar(32) NOT NULL,
  `managementNetworkUuid` varchar(32) DEFAULT NULL,
  `publicNetworkUuid` varchar(32) DEFAULT NULL,
  `imageUuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) NOT NULL,
  `isDefault` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVirtualRouterOfferingVOImageEO` (`imageUuid`),
  KEY `fkVirtualRouterOfferingVOL3NetworkEO` (`managementNetworkUuid`),
  KEY `fkVirtualRouterOfferingVOL3NetworkEO1` (`publicNetworkUuid`),
  KEY `fkVirtualRouterOfferingVOZoneEO` (`zoneUuid`),
  CONSTRAINT `fkVirtualRouterOfferingVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterOfferingVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterOfferingVOInstanceOfferingEO` FOREIGN KEY (`uuid`) REFERENCES `InstanceOfferingEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterOfferingVOL3NetworkEO` FOREIGN KEY (`managementNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterOfferingVOL3NetworkEO1` FOREIGN KEY (`publicNetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterPortForwardingRuleRefVO` (
  `uuid` varchar(32) NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVirtualRouterPortForwardingRuleRefVOVipVO` (`vipUuid`),
  KEY `fkVirtualRouterPortForwardingRuleRefVOVmInstanceEO` (`virtualRouterVmUuid`),
  CONSTRAINT `fkVirtualRouterPortForwardingRuleRefVOVmInstanceEO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterPortForwardingRuleRefVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterSoftwareVersionVO` (
  `uuid` varchar(32) NOT NULL,
  `softwareName` varchar(32) NOT NULL,
  `currentVersion` varchar(32) DEFAULT NULL,
  `latestVersion` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVirtualRouterSoftwareVersionVOVirtualRouterVmVO` FOREIGN KEY (`uuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterVRouterRouteTableRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `virtualRouterVmUuid` varchar(32) NOT NULL COMMENT 'uuid',
  `routeTableUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `virtualRouterVmUuid` (`virtualRouterVmUuid`),
  KEY `VirutalRouterVRouterRouteTableRefVOVRouterRouteTableVO` (`routeTableUuid`),
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVRouterRouteTableVO` FOREIGN KEY (`routeTableUuid`) REFERENCES `VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVirtualRouterVmVO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterVipVO` (
  `uuid` varchar(32) NOT NULL,
  `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVirtualRouterVipVOVmInstanceEO` (`virtualRouterVmUuid`),
  CONSTRAINT `fkVirtualRouterVipVOVmInstanceEO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVirtualRouterVipVOVipVO` FOREIGN KEY (`uuid`) REFERENCES `VipVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterVmVO` (
  `uuid` varchar(32) NOT NULL,
  `publicNetworkUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVirtualRouterVmVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmCPUBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `cpuNum` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmCdRomVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `deviceId` int(10) unsigned NOT NULL COMMENT 'device id',
  `occupant` varchar(64) DEFAULT NULL,
  `isoUuid` varchar(32) DEFAULT NULL,
  `isoInstallPath` varchar(1024) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `protocol` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `vmInstanceCdRomDeviceId` (`vmInstanceUuid`,`deviceId`),
  KEY `fkVmCdRomVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkVmCdRomVOImageEO` (`isoUuid`),
  CONSTRAINT `fkVmCdRomVOImageEO` FOREIGN KEY (`isoUuid`) REFERENCES `ImageEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmCdRomVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmCrashHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmCustomSpecificationVO` (
  `uuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `platform` varchar(32) NOT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `rootPassword` varchar(255) DEFAULT NULL,
  `generateSID` tinyint(1) DEFAULT '0',
  `domainMode` varchar(32) DEFAULT 'WorkGroup',
  `domainName` varchar(255) DEFAULT NULL,
  `domainUsername` varchar(255) DEFAULT NULL,
  `domainPassword` varchar(255) DEFAULT NULL,
  `organization` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmCustomSpecificationVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkVmCustomSpecificationVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmDnsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` char(32) NOT NULL,
  `vmNicUuid` char(32) DEFAULT NULL,
  `dns` varchar(255) NOT NULL,
  `ipVersion` int(10) unsigned DEFAULT '4',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVmDnsVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkVmDnsVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkVmDnsVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmDnsVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmGuestNetworkInfoVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` char(32) NOT NULL,
  `vmNicUuid` char(32) DEFAULT NULL,
  `ipAddress` varchar(128) DEFAULT NULL,
  `gateway` varchar(128) DEFAULT NULL,
  `dnsList` varchar(255) DEFAULT NULL,
  `routeList` varchar(1024) DEFAULT NULL,
  `ipv6Address` varchar(128) DEFAULT NULL,
  `ipv6Gateway` varchar(128) DEFAULT NULL,
  `dns6List` varchar(255) DEFAULT NULL,
  `route6List` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxVmGuestNetworkInfoVOipAddress` (`ipAddress`),
  KEY `idxVmGuestNetworkInfoVOipv6Address` (`ipv6Address`),
  KEY `fkVmGuestNetworkInfoVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkVmGuestNetworkInfoVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkVmGuestNetworkInfoVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmGuestNetworkInfoVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmHaVO` (
  `uuid` char(32) NOT NULL,
  `haLevel` varchar(64) NOT NULL DEFAULT 'Undefined',
  `haLevelUpdateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `inhibitionReason` varchar(255) DEFAULT NULL,
  `inhibitionTime` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVmHaVOVmInstanceVO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmHostBackupFileVO` (
  `uuid` char(32) NOT NULL,
  `resourceUuid` char(32) NOT NULL,
  `type` varchar(64) NOT NULL COMMENT 'NvRam, TpmState',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVmHostBackupFileVO` (`resourceUuid`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmHostFileContentVO` (
  `uuid` char(32) NOT NULL COMMENT 'VmHostFileVO.uuid or VmHostBackupFileVO.uuid',
  `content` mediumblob,
  `format` varchar(64) NOT NULL COMMENT 'Raw, TarballGzip',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVmHostFileContentVOResourceVO` FOREIGN KEY (`uuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmHostFileVO` (
  `uuid` char(32) NOT NULL,
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
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVmHostFileVO` (`vmInstanceUuid`,`hostUuid`,`type`),
  KEY `idxVmHostFileVOVmInstanceUuid` (`vmInstanceUuid`),
  KEY `idxVmHostFileVOHostUuid` (`hostUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceEO` (
  `uuid` varchar(32) NOT NULL,
  `zoneUuid` varchar(32) DEFAULT NULL,
  `clusterUuid` varchar(32) DEFAULT NULL,
  `imageUuid` varchar(32) DEFAULT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `lastHostUuid` varchar(32) DEFAULT NULL,
  `rootVolumeUuid` varchar(32) DEFAULT NULL,
  `instanceOfferingUuid` varchar(32) DEFAULT NULL,
  `defaultL3NetworkUuid` varchar(32) DEFAULT NULL,
  `cpuNum` int(10) unsigned NOT NULL,
  `cpuSpeed` bigint(20) unsigned NOT NULL,
  `memorySize` bigint(20) unsigned NOT NULL,
  `allocatorStrategy` varchar(64) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `internalId` bigint(20) unsigned NOT NULL,
  `hypervisorType` varchar(64) DEFAULT NULL,
  `state` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `platform` varchar(255) NOT NULL,
  `architecture` varchar(32) DEFAULT NULL,
  `guestOsType` varchar(255) DEFAULT NULL,
  `reservedMemorySize` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmInstanceEOClusterEO` (`clusterUuid`),
  KEY `fkVmInstanceEOHostEO` (`hostUuid`),
  KEY `fkVmInstanceEOHostEO1` (`lastHostUuid`),
  KEY `fkVmInstanceEOImageEO` (`imageUuid`),
  KEY `fkVmInstanceEOInstanceOfferingEO` (`instanceOfferingUuid`),
  KEY `fkVmInstanceEOZoneEO` (`zoneUuid`),
  KEY `idxVmInstanceEOname` (`name`(128)),
  KEY `fkVmInstanceEOVolumeEO` (`rootVolumeUuid`),
  KEY `idxDeleted` (`deleted`),
  CONSTRAINT `fkVmInstanceEOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmInstanceEOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmInstanceEOHostEO1` FOREIGN KEY (`lastHostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmInstanceEOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `ImageEO` (`uuid`),
  CONSTRAINT `fkVmInstanceEOInstanceOfferingEO` FOREIGN KEY (`instanceOfferingUuid`) REFERENCES `InstanceOfferingEO` (`uuid`),
  CONSTRAINT `fkVmInstanceEOVolumeEO` FOREIGN KEY (`rootVolumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmInstanceEOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceMdevDeviceSpecRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `mdevSpecUuid` varchar(32) NOT NULL,
  `mdevDeviceNumber` int(10) unsigned DEFAULT '1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVmMdevSpecRefVmInstanceUuid` (`vmInstanceUuid`),
  KEY `fkVmMdevSpecRefMdevSpecUuid` (`mdevSpecUuid`),
  CONSTRAINT `fkVmMdevSpecRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmMdevSpecRefMdevSpecUuid` FOREIGN KEY (`mdevSpecUuid`) REFERENCES `MdevDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceNumaNodeVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `vNodeID` int(11) NOT NULL,
  `vNodeCPUs` text NOT NULL,
  `vNodeMemSize` bigint(20) NOT NULL,
  `vNodeDistance` varchar(512) NOT NULL,
  `pNodeID` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk` (`vmUuid`),
  CONSTRAINT `VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstancePciDeviceSpecRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `pciSpecUuid` varchar(32) NOT NULL,
  `pciDeviceNumber` int(10) unsigned DEFAULT '1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `vmInstanceUuid` (`vmInstanceUuid`,`pciSpecUuid`),
  KEY `fkVmPciSpecRefPciSpecUuid` (`pciSpecUuid`),
  CONSTRAINT `fkVmPciSpecRefVmInstanceUuid` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmPciSpecRefPciSpecUuid` FOREIGN KEY (`pciSpecUuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceResourceMetadataArchiveVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` char(32) NOT NULL,
  `vmInstanceUuid` char(32) NOT NULL,
  `addressGroupUuid` char(32) NOT NULL,
  `deviceAddress` varchar(128) DEFAULT NULL,
  `metadata` text,
  `metadataClass` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVmInstanceDeviceAddressArchiveVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkVmInstanceDeviceAddressArchiveVOVmInstanceDeviceAddressGroupVO` (`addressGroupUuid`),
  CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceDeviceAddressGroupVO` FOREIGN KEY (`addressGroupUuid`) REFERENCES `VmInstanceResourceMetadataGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceResourceMetadataGroupVO` (
  `uuid` char(32) NOT NULL,
  `resourceUuid` char(32) NOT NULL,
  `vmInstanceUuid` char(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmInstanceDeviceAddressGroupVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkVmInstanceDeviceAddressGroupVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceResourceMetadataVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` char(32) NOT NULL,
  `vmInstanceUuid` char(32) NOT NULL,
  `deviceAddress` varchar(128) DEFAULT NULL,
  `metadata` text,
  `metadataClass` varchar(128) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVmInstanceDeviceAddressVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkVmInstanceDeviceAddressVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceSequenceNumberVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmInstanceVmNicRedirectPortRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `mirrorPortId` bigint(20) unsigned NOT NULL,
  `primaryInPortId` bigint(20) unsigned NOT NULL,
  `secondaryInPortId` bigint(20) unsigned NOT NULL,
  `primaryOutPortId` bigint(20) unsigned NOT NULL,
  `vmNicUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `vmInstanceUuid` (`vmInstanceUuid`),
  KEY `vmNicUuid` (`vmNicUuid`),
  CONSTRAINT `fkVmInstanceVmNicRedirectPortRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmInstanceVmNicRedirectPortRefVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmMemoryBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `memorySize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmMetadataDirtyVO` (
  `vmInstanceUuid` varchar(32) NOT NULL,
  `managementNodeUuid` varchar(32) DEFAULT NULL,
  `dirtyVersion` bigint(20) NOT NULL DEFAULT '1',
  `lastClaimTime` timestamp NULL DEFAULT NULL,
  `storageStructureChange` tinyint(1) NOT NULL DEFAULT '0',
  `retryCount` int(11) NOT NULL DEFAULT '0',
  `nextRetryTime` timestamp NULL DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`vmInstanceUuid`),
  KEY `idx_VmMetadataDirtyVO_unclaimed` (`managementNodeUuid`,`nextRetryTime`,`lastOpDate`),
  CONSTRAINT `fkVmMetadataDirtyVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmMetadataDirtyVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmMetadataFlushStateVO` (
  `vmInstanceUuid` varchar(32) NOT NULL,
  `metadataSnapshot` longtext,
  `lastFlushFinishTime` timestamp NULL DEFAULT NULL,
  `pendingStaleRecovery` tinyint(1) NOT NULL DEFAULT '0',
  `staleRecoveryCount` int(11) NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`vmInstanceUuid`),
  CONSTRAINT `fkVmMetadataFlushStateVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmNicSecurityGroupRefVO` (
  `uuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) NOT NULL,
  `securityGroupUuid` varchar(32) NOT NULL,
  `priority` int(11) DEFAULT '-1',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmNicSecurityGroupRefVOSecurityGroupVO` (`securityGroupUuid`),
  KEY `fkVmNicSecurityGroupRefVOVmInstanceEO` (`vmInstanceUuid`),
  KEY `fkVmNicSecurityGroupRefVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkVmNicSecurityGroupRefVOSecurityGroupVO` FOREIGN KEY (`securityGroupUuid`) REFERENCES `SecurityGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmNicSecurityGroupRefVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmNicSecurityGroupRefVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmNicSecurityPolicyVO` (
  `uuid` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) NOT NULL,
  `ingressPolicy` varchar(32) NOT NULL DEFAULT 'DENY',
  `egressPolicy` varchar(32) NOT NULL DEFAULT 'ALLOW',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmNicSecurityPolicyVOVmNicVO` (`vmNicUuid`),
  CONSTRAINT `fkVmNicSecurityPolicyVOVmNicVO` FOREIGN KEY (`vmNicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmNicVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `vmInstanceUuid` varchar(32) DEFAULT NULL COMMENT 'vm instance uuid',
  `usedIpUuid` varchar(32) DEFAULT NULL COMMENT 'used ip uuid',
  `l3NetworkUuid` varchar(32) DEFAULT NULL COMMENT 'l3 network uuid',
  `metaData` varchar(255) DEFAULT NULL,
  `ip` varchar(128) DEFAULT NULL,
  `mac` varchar(17) NOT NULL COMMENT 'mac address',
  `gateway` varchar(128) DEFAULT NULL,
  `netmask` varchar(128) DEFAULT NULL,
  `internalName` varchar(128) DEFAULT NULL,
  `deviceId` int(10) unsigned NOT NULL COMMENT 'device id',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `hypervisorType` varchar(64) DEFAULT NULL,
  `ipVersion` int(10) unsigned DEFAULT '4',
  `driverType` varchar(64) DEFAULT NULL,
  `type` varchar(32) DEFAULT 'VNIC',
  `state` varchar(255) NOT NULL DEFAULT 'enable',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `usedIpUuid` (`usedIpUuid`),
  KEY `idxVmNicVOip` (`ip`),
  KEY `idxVmNicVOmac` (`mac`),
  KEY `fkVmNicVOL3NetworkEO` (`l3NetworkUuid`),
  KEY `fkVmNicVOVmInstanceEO` (`vmInstanceUuid`),
  CONSTRAINT `fkVmNicVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmNicVOUsedIpVO` FOREIGN KEY (`usedIpUuid`) REFERENCES `UsedIpVO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVmNicVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmPriorityConfigVO` (
  `uuid` varchar(32) NOT NULL,
  `level` varchar(255) NOT NULL,
  `cpuShares` int(11) NOT NULL,
  `oomScoreAdj` int(11) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmSchedHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmInstanceUuid` char(32) NOT NULL,
  `zoneUuid` char(32) DEFAULT NULL,
  `accountUuid` char(32) NOT NULL,
  `schedType` varchar(32) NOT NULL,
  `success` tinyint(1) DEFAULT NULL,
  `lastHostUuid` char(32) DEFAULT NULL,
  `destHostUuid` char(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `schedReason` text,
  `failReason` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxVmSchedHistoryVOVmInstanceUuid` (`vmInstanceUuid`),
  KEY `idxVmSchedHistoryVOZoneUuid` (`zoneUuid`),
  KEY `idxVmSchedHistoryVOSchedType` (`schedType`),
  CONSTRAINT `fkVmSchedHistoryVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `ZoneEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmSchedulingRuleGroupRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmGroupUuid` varchar(32) NOT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `vmGroupUuid_vmUuid` (`vmGroupUuid`,`vmUuid`) USING BTREE,
  KEY `fkVmInstanceVORefVO` (`vmUuid`),
  CONSTRAINT `fkVmSchedulingPolicyGroupRefVO` FOREIGN KEY (`vmGroupUuid`) REFERENCES `VmSchedulingRuleGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmInstanceVORefVO` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmSchedulingRuleGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `appliance` varchar(128) NOT NULL,
  `zoneUuid` varchar(32) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `srcUuid` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `VmSchedulingRuleGroupVO` VALUE
('d3612d528c4011f19babfa4856ea8e00','zstack.affinity.group.for.virtual.router','VROUTER',NULL,NULL,'ce88cd128c4011f19babfa4856ea8e00','2026-07-30 18:02:27','2026-07-30 18:02:27');

CREATE TABLE `VmSchedulingRuleRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmSchedulingRuleUuid` varchar(32) NOT NULL,
  `vmGroupUuid` varchar(32) NOT NULL,
  `hostGroupUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `ruleUuid_vmGroupUuid_hostGroupUuid` (`vmSchedulingRuleUuid`,`vmGroupUuid`,`hostGroupUuid`) USING BTREE,
  KEY `fkVmSchedulingRuleGroupVORefVO` (`vmGroupUuid`),
  KEY `fkHostSchedulingRuleGroupVORefVO` (`hostGroupUuid`),
  CONSTRAINT `fkVmSchedulingRuleVORefVO` FOREIGN KEY (`vmSchedulingRuleUuid`) REFERENCES `VmSchedulingRuleVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVmSchedulingRuleGroupVORefVO` FOREIGN KEY (`vmGroupUuid`) REFERENCES `VmSchedulingRuleGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHostSchedulingRuleGroupVORefVO` FOREIGN KEY (`hostGroupUuid`) REFERENCES `HostSchedulingRuleGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

INSERT INTO `VmSchedulingRuleRefVO` VALUE
(1,'ce88cd128c4011f19babfa4856ea8e00','d3612d528c4011f19babfa4856ea8e00',NULL,'2026-07-30 18:02:27','2026-07-30 18:02:27');

CREATE TABLE `VmSchedulingRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `rule` varchar(64) NOT NULL,
  `mode` varchar(64) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVmSchedulingRuleVOAffinityGroupVO` FOREIGN KEY (`uuid`) REFERENCES `AffinityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `VmSchedulingRuleVO` VALUE
('ce88cd128c4011f19babfa4856ea8e00','ANTIAFFINITY','SOFT');

CREATE TABLE `VmUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `state` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `cpuNum` int(10) unsigned NOT NULL,
  `memorySize` bigint(20) unsigned NOT NULL,
  `rootVolumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxVmUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxVmUuid` (`vmUuid`) USING BTREE,
  KEY `idxVmUsageVOvmUuid` (`accountUuid`,`dateInLong`,`vmUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `VmUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `state` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `cpuNum` int(10) unsigned NOT NULL,
  `memorySize` bigint(20) unsigned NOT NULL,
  `rootVolumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxVmUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxVmUuid` (`vmUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmVdpaNicVO` (
  `uuid` varchar(32) NOT NULL,
  `pciDeviceUuid` varchar(32) DEFAULT NULL,
  `lastPciDeviceUuid` varchar(32) DEFAULT NULL,
  `srcPath` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmVdpaNicVOPciDeviceVO` (`pciDeviceUuid`),
  CONSTRAINT `fkVmVdpaNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmVfNicVO` (
  `uuid` varchar(32) NOT NULL,
  `pciDeviceUuid` varchar(32) DEFAULT NULL,
  `haState` varchar(32) NOT NULL DEFAULT 'Disabled',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVmVfNicVOPciDeviceVO` (`pciDeviceUuid`),
  CONSTRAINT `fkVmVfNicVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VniRangeVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `startVni` int(11) NOT NULL COMMENT 'start vni',
  `endVni` int(11) NOT NULL COMMENT 'end vni',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVniRangeVOL2NetworkEO` (`l2NetworkUuid`),
  CONSTRAINT `fkVniRangeVOL2NetworkEO` FOREIGN KEY (`l2NetworkUuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeBackupHistoryVO` (
  `uuid` varchar(32) NOT NULL,
  `bitmap` varchar(32) NOT NULL,
  `lastBackupUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  KEY `fkVolumeBackupHistoryVOVolumeBackupVO` (`lastBackupUuid`),
  CONSTRAINT `fkVolumeBackupHistoryVOVolumeEO` FOREIGN KEY (`uuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVolumeBackupHistoryVOVolumeBackupVO` FOREIGN KEY (`lastBackupUuid`) REFERENCES `VolumeBackupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeBackupStorageRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `backupStorageUuid` varchar(32) NOT NULL,
  `volumeBackupUuid` varchar(32) NOT NULL,
  `status` varchar(64) NOT NULL,
  `installPath` varchar(2048) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVolumeBackupStorageRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkVolumeBackupStorageRefVOVolumeBackupVO` (`volumeBackupUuid`),
  CONSTRAINT `fkVolumeBackupStorageRefVOVolumeBackupVO` FOREIGN KEY (`volumeBackupUuid`) REFERENCES `VolumeBackupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVolumeBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeBackupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `status` varchar(64) NOT NULL,
  `metadata` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `groupUuid` varchar(32) DEFAULT NULL,
  `mode` varchar(32) DEFAULT 'incremental',
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `fkVolumeBackupVOVolumeEO` (`volumeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeCbtBackupRecordVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `taskUuid` varchar(32) NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `mode` varchar(255) NOT NULL,
  `target` varchar(2048) NOT NULL,
  `scratchNodeName` varchar(255) NOT NULL,
  `bitmapName` varchar(255) NOT NULL,
  `lastBitmapName` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeEO` (
  `uuid` varchar(32) NOT NULL,
  `primaryStorageUuid` varchar(32) DEFAULT NULL,
  `rootImageUuid` varchar(32) DEFAULT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `diskOfferingUuid` varchar(32) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `installPath` varchar(1024) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `format` varchar(64) DEFAULT NULL,
  `size` bigint(20) unsigned NOT NULL,
  `deviceId` int(10) unsigned DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `isAttached` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `actualSize` bigint(20) unsigned DEFAULT NULL,
  `isShareable` tinyint(1) NOT NULL DEFAULT '0',
  `volumeQos` varchar(128) DEFAULT NULL COMMENT 'volumeQos format like total=1048576',
  `lastVmInstanceUuid` varchar(32) DEFAULT NULL,
  `lastDetachDate` varchar(32) DEFAULT NULL,
  `lastAttachDate` varchar(32) DEFAULT NULL,
  `protocol` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVolumeEOImageEO` (`rootImageUuid`),
  KEY `fkVolumeEOPrimaryStorageEO` (`primaryStorageUuid`),
  KEY `fkVolumeEOVmInstanceEO` (`vmInstanceUuid`),
  KEY `idxVolumeEOname` (`name`),
  KEY `idxDeleted` (`deleted`),
  KEY `fkVolumeEODiskOfferingEO` (`diskOfferingUuid`),
  CONSTRAINT `fkVolumeEODiskOfferingEO` FOREIGN KEY (`diskOfferingUuid`) REFERENCES `DiskOfferingEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeHostRefVO` (
  `volumeUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `mountPath` varchar(512) NOT NULL,
  `device` varchar(512) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`volumeUuid`),
  UNIQUE KEY `volumeUuid` (`volumeUuid`),
  KEY `fkVolumeHostRefVOHostEO` (`hostUuid`),
  CONSTRAINT `fkVolumeHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotBackupStorageRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeSnapshotUuid` varchar(32) NOT NULL,
  `backupStorageUuid` varchar(32) NOT NULL,
  `installPath` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVolumeSnapshotBackupStorageRefVOBackupStorageEO` (`backupStorageUuid`),
  KEY `fkVolumeSnapshotBackupStorageRefVOVolumeSnapshotEO` (`volumeSnapshotUuid`),
  CONSTRAINT `fkVolumeSnapshotBackupStorageRefVOVolumeSnapshotEO` FOREIGN KEY (`volumeSnapshotUuid`) REFERENCES `VolumeSnapshotEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVolumeSnapshotBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotEO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `volumeUuid` varchar(32) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `format` varchar(64) NOT NULL,
  `treeUuid` varchar(32) NOT NULL,
  `parentUuid` varchar(32) DEFAULT NULL,
  `backupStorageUuid` varchar(32) DEFAULT NULL,
  `primaryStorageUuid` varchar(32) DEFAULT NULL,
  `primaryStorageInstallPath` varchar(1024) DEFAULT NULL,
  `backupStorageInstallPath` varchar(1024) DEFAULT NULL,
  `volumeType` varchar(32) NOT NULL,
  `state` varchar(64) NOT NULL,
  `status` varchar(64) NOT NULL,
  `distance` int(10) unsigned DEFAULT '0',
  `size` bigint(20) unsigned DEFAULT '0',
  `latest` tinyint(1) unsigned DEFAULT '0',
  `fullSnapshot` tinyint(1) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVolumeSnapshotEOPrimaryStorageEO` (`primaryStorageUuid`),
  KEY `fkVolumeSnapshotEOVolumeEO` (`volumeUuid`),
  KEY `fkVolumeSnapshotEOVolumeSnapshotEO` (`parentUuid`),
  KEY `fkVolumeSnapshotEOVolumeSnapshotTreeEO` (`treeUuid`),
  KEY `idxVolumeSnapshotEOname` (`name`),
  KEY `idxDeleted` (`deleted`),
  CONSTRAINT `fkVolumeSnapshotEOVolumeSnapshotTreeEO` FOREIGN KEY (`treeUuid`) REFERENCES `VolumeSnapshotTreeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVolumeSnapshotEOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVolumeSnapshotEOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE SET NULL,
  CONSTRAINT `fkVolumeSnapshotEOVolumeSnapshotEO` FOREIGN KEY (`parentUuid`) REFERENCES `VolumeSnapshotEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotGroupRefVO` (
  `volumeSnapshotUuid` varchar(32) NOT NULL,
  `volumeSnapshotGroupUuid` varchar(32) NOT NULL,
  `snapshotDeleted` tinyint(1) NOT NULL,
  `deviceId` int(10) unsigned NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeName` varchar(256) NOT NULL,
  `volumeType` varchar(32) NOT NULL,
  `volumeSnapshotName` varchar(256) DEFAULT NULL,
  `volumeSnapshotInstallPath` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `volumeLastAttachDate` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`volumeSnapshotUuid`),
  UNIQUE KEY `volumeSnapshotUuid` (`volumeSnapshotUuid`),
  KEY `fkVolumeSnapshotGroupRefVOVolumeSnapshotGroupVO` (`volumeSnapshotGroupUuid`),
  CONSTRAINT `fkVolumeSnapshotGroupRefVOVolumeSnapshotGroupVO` FOREIGN KEY (`volumeSnapshotGroupUuid`) REFERENCES `VolumeSnapshotGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `snapshotCount` int(10) unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotReferenceTreeVO` (
  `uuid` varchar(32) NOT NULL,
  `rootImageUuid` varchar(32) DEFAULT NULL,
  `rootVolumeUuid` varchar(32) DEFAULT NULL,
  `rootInstallUrl` varchar(1024) DEFAULT NULL,
  `rootVolumeSnapshotUuid` varchar(32) DEFAULT NULL,
  `rootVolumeSnapshotTreeUuid` varchar(32) DEFAULT NULL,
  `primaryStorageUuid` varchar(32) DEFAULT NULL,
  `hostUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotReferenceVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parentId` bigint(20) DEFAULT NULL,
  `volumeUuid` varchar(32) DEFAULT NULL,
  `volumeSnapshotUuid` varchar(32) DEFAULT NULL,
  `directSnapshotUuid` varchar(32) DEFAULT NULL,
  `volumeSnapshotInstallUrl` varchar(1024) DEFAULT NULL,
  `directSnapshotInstallUrl` varchar(1024) DEFAULT NULL,
  `treeUuid` varchar(32) DEFAULT NULL,
  `referenceUuid` varchar(32) DEFAULT NULL,
  `referenceType` varchar(32) DEFAULT NULL,
  `referenceInstallUrl` varchar(1024) DEFAULT NULL,
  `referenceVolumeUuid` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  KEY `idxVolumeSnapshotReferenceVOVolumeUuid` (`volumeUuid`),
  KEY `idxVolumeSnapshotReferenceVOVolumeSnapshotUuid` (`volumeSnapshotUuid`),
  KEY `idxVolumeSnapshotReferenceVOReferenceUuid` (`referenceUuid`),
  KEY `fkVolumeSnapshotReferenceReferenceVolumeUuid` (`referenceVolumeUuid`),
  KEY `fkVolumeSnapshotReferenceReferenceParentId` (`parentId`),
  KEY `fkVolumeSnapshotReferenceReferenceTreeUuid` (`treeUuid`),
  CONSTRAINT `fkVolumeSnapshotReferenceReferenceVolumeUuid` FOREIGN KEY (`referenceVolumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVolumeSnapshotReferenceReferenceParentId` FOREIGN KEY (`parentId`) REFERENCES `VolumeSnapshotReferenceVO` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fkVolumeSnapshotReferenceReferenceTreeUuid` FOREIGN KEY (`treeUuid`) REFERENCES `VolumeSnapshotReferenceTreeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VolumeSnapshotTreeEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `volumeUuid` varchar(32) DEFAULT NULL,
  `current` tinyint(1) unsigned DEFAULT '0',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'Completed',
  `rootImageUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVolumeSnapshotTreeEOVolumeEO` (`volumeUuid`),
  CONSTRAINT `fkVolumeSnapshotTreeEOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `VolumeEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallIpSetTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `sourceValue` varchar(2048) DEFAULT NULL,
  `destValue` varchar(2048) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleSetL3RefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `ruleSetUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `packetsForwardType` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOL3NetworkEO` (`l3NetworkUuid`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOVpcFirewallRuleSetVO` (`ruleSetUuid`) USING BTREE,
  KEY `fkVpcFirewallRuleSetL3RefVOVpcFirewallVO` (`vpcFirewallUuid`) USING BTREE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOVpcFirewallRuleSetVO` FOREIGN KEY (`ruleSetUuid`) REFERENCES `VpcFirewallRuleSetVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcFirewallRuleSetL3RefVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleSetVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `actionType` varchar(255) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `enableDefaultLog` tinyint(1) NOT NULL DEFAULT '0',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `isDefault` tinyint(1) NOT NULL DEFAULT '0',
  `isApplied` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleTemplateVO` (
  `uuid` varchar(32) NOT NULL,
  `action` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `sourcePort` varchar(255) DEFAULT NULL,
  `destPort` varchar(255) DEFAULT NULL,
  `sourceIp` varchar(2048) DEFAULT NULL,
  `destIp` varchar(2048) DEFAULT NULL,
  `ruleNumber` int(10) NOT NULL,
  `icmpTypeName` varchar(255) DEFAULT NULL,
  `allowStates` varchar(255) DEFAULT NULL,
  `tcpFlag` varchar(255) DEFAULT NULL,
  `enableLog` tinyint(1) NOT NULL DEFAULT '0',
  `state` varchar(32) NOT NULL DEFAULT '0',
  `isDefault` tinyint(1) NOT NULL DEFAULT '0',
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallRuleVO` (
  `uuid` varchar(32) NOT NULL,
  `ruleSetUuid` varchar(32) NOT NULL,
  `action` varchar(255) NOT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `sourcePort` varchar(255) DEFAULT NULL,
  `destPort` varchar(255) DEFAULT NULL,
  `sourceIp` varchar(2048) DEFAULT NULL,
  `destIp` varchar(2048) DEFAULT NULL,
  `ruleNumber` int(10) NOT NULL,
  `icmpTypeName` varchar(255) DEFAULT NULL,
  `allowStates` varchar(255) DEFAULT NULL,
  `tcpFlag` varchar(255) DEFAULT NULL,
  `enableLog` tinyint(1) NOT NULL DEFAULT '0',
  `state` varchar(32) NOT NULL DEFAULT '0',
  `isDefault` tinyint(1) NOT NULL DEFAULT '0',
  `description` varchar(2048) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `isApplied` tinyint(1) NOT NULL DEFAULT '1',
  `expired` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallVO` (
  `uuid` varchar(32) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcFirewallVRouterRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `vRouterUuid` varchar(32) NOT NULL,
  `vpcFirewallUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`) USING BTREE,
  KEY `VpcFirewallVRouterRefVOVpcFirewallVO` (`vpcFirewallUuid`),
  KEY `VpcFirewallVRouterRefVOVirtualRouteVmVO` (`vRouterUuid`),
  CONSTRAINT `VpcFirewallVRouterRefVOVpcFirewallVO` FOREIGN KEY (`vpcFirewallUuid`) REFERENCES `VpcFirewallVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `VpcFirewallVRouterRefVOVirtualRouteVmVO` FOREIGN KEY (`vRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupApplianceVmRefVO` (
  `uuid` varchar(32) NOT NULL,
  `vpcHaRouterUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVpcHaGroupApplianceVmRefVOVpcHaGroupVO` (`vpcHaRouterUuid`),
  CONSTRAINT `fkVpcHaGroupApplianceVmRefVOVpcHaGroupVO` FOREIGN KEY (`vpcHaRouterUuid`) REFERENCES `VpcHaGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcHaGroupApplianceVmRefVOApplianceVmVO` FOREIGN KEY (`uuid`) REFERENCES `ApplianceVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupMonitorIpVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vpcHaRouterUuid` varchar(32) NOT NULL,
  `monitorIp` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVpcHaGroupMonitorIpVOVpcHaGroupVO` (`vpcHaRouterUuid`),
  CONSTRAINT `fkVpcHaGroupMonitorIpVOVpcHaGroupVO` FOREIGN KEY (`vpcHaRouterUuid`) REFERENCES `VpcHaGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupNetworkServiceRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vpcHaRouterUuid` varchar(32) NOT NULL,
  `networkServiceName` varchar(128) NOT NULL,
  `networkServiceUuid` varchar(128) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVpcHaGroupNetworkServiceRefVOVpcHaGroupVO` (`vpcHaRouterUuid`),
  CONSTRAINT `fkVpcHaGroupNetworkServiceRefVOVpcHaGroupVO` FOREIGN KEY (`vpcHaRouterUuid`) REFERENCES `VpcHaGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcHaGroupVipRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vpcHaRouterUuid` varchar(32) NOT NULL,
  `vipUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `ip` varchar(32) NOT NULL,
  `netmask` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVpcHaGroupVipRefVOVpcHaGroupVO` (`vpcHaRouterUuid`),
  KEY `fkVpcHaGroupVipRefVOL3NetworkVO` (`l3NetworkUuid`),
  KEY `fkVpcHaGroupVipRefVOVipVO` (`vipUuid`),
  CONSTRAINT `fkVpcHaGroupVipRefVOVpcHaGroupVO` FOREIGN KEY (`vpcHaRouterUuid`) REFERENCES `VpcHaGroupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcHaGroupVipRefVOL3NetworkVO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `L3NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVpcHaGroupVipRefVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `VipVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcRouterDnsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vpcRouterUuid` varchar(32) NOT NULL,
  `dns` varchar(255) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVpcRouterDnsVOVirtualRouterVmVO` (`vpcRouterUuid`),
  CONSTRAINT `fkVpcRouterDnsVOVirtualRouterVmVO` FOREIGN KEY (`vpcRouterUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcRouterVmVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcSnatStateVO` (
  `uuid` varchar(32) NOT NULL,
  `vpcUuid` varchar(32) NOT NULL,
  `l3NetworkUuid` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  UNIQUE KEY `uqVpcL3NetworkRefVO` (`vpcUuid`,`l3NetworkUuid`),
  CONSTRAINT `fkVpcNetworkServiceRefVOVirtualRouterVmVO` FOREIGN KEY (`vpcUuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcUserVpnGatewayVO` (
  `uuid` varchar(32) NOT NULL,
  `accountName` varchar(128) NOT NULL,
  `gatewayId` varchar(32) NOT NULL,
  `dataCenterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `ip` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVpcUserVpnGatewayVO` (`dataCenterUuid`,`accountName`,`gatewayId`) USING BTREE,
  KEY `fkVpcUserVpnGatewayVOAccountVO` (`accountName`),
  CONSTRAINT `fkVpcUserVpnGatewayVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `DataCenterVO` (`uuid`),
  CONSTRAINT `fkVpcUserVpnGatewayVOAccountVO` FOREIGN KEY (`accountName`) REFERENCES `AccountVO` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVirtualRouteEntryVO` (
  `uuid` varchar(32) NOT NULL,
  `destinationCidrBlock` varchar(64) NOT NULL,
  `nextHopId` varchar(128) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `nextHopType` varchar(32) NOT NULL,
  `vRouterType` varchar(16) NOT NULL,
  `virtualRouterUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVirtualRouterVO` (
  `uuid` varchar(32) NOT NULL,
  `vrId` varchar(32) NOT NULL,
  `vpcUuid` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVpcVirtualRouterVOEcsVpcVO` (`vpcUuid`),
  CONSTRAINT `fkVpcVirtualRouterVOEcsVpcVO` FOREIGN KEY (`vpcUuid`) REFERENCES `EcsVpcVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnConnectionVO` (
  `uuid` varchar(32) NOT NULL,
  `accountName` varchar(128) NOT NULL,
  `connectionId` varchar(32) NOT NULL,
  `userGatewayUuid` varchar(32) NOT NULL,
  `vpnGatewayUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `localSubnet` varchar(64) NOT NULL,
  `remoteSubnet` varchar(64) NOT NULL,
  `ikeConfigUuid` varchar(32) NOT NULL,
  `ipsecConfigUuid` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `status` varchar(32) NOT NULL DEFAULT 'IPSEC_SUCCESS',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVpcVpnConnectionVO` (`connectionId`,`accountName`,`userGatewayUuid`) USING BTREE,
  KEY `fkVpcVpnConnectionVOVpcUserVpnGatewayVO` (`userGatewayUuid`),
  KEY `fkVpcVpnConnectionVOVpcVpnGatewayVO` (`vpnGatewayUuid`),
  KEY `fkVpcVpnConnectionVOVpcVpnIkeConfigVO` (`ikeConfigUuid`),
  KEY `fkVpcVpnConnectionVOVpcVpnIpSecConfigVO` (`ipsecConfigUuid`),
  KEY `fkVpcVpnConnectionVOAccountVO` (`accountName`),
  CONSTRAINT `fkVpcVpnConnectionVOAccountVO` FOREIGN KEY (`accountName`) REFERENCES `AccountVO` (`name`),
  CONSTRAINT `fkVpcVpnConnectionVOVpcUserVpnGatewayVO` FOREIGN KEY (`userGatewayUuid`) REFERENCES `VpcUserVpnGatewayVO` (`uuid`),
  CONSTRAINT `fkVpcVpnConnectionVOVpcVpnGatewayVO` FOREIGN KEY (`vpnGatewayUuid`) REFERENCES `VpcVpnGatewayVO` (`uuid`),
  CONSTRAINT `fkVpcVpnConnectionVOVpcVpnIkeConfigVO` FOREIGN KEY (`ikeConfigUuid`) REFERENCES `VpcVpnIkeConfigVO` (`uuid`),
  CONSTRAINT `fkVpcVpnConnectionVOVpcVpnIpSecConfigVO` FOREIGN KEY (`ipsecConfigUuid`) REFERENCES `VpcVpnIpSecConfigVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnGatewayVO` (
  `uuid` varchar(32) NOT NULL,
  `accountName` varchar(128) NOT NULL,
  `gatewayId` varchar(32) NOT NULL,
  `vSwitchUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `publicIp` varchar(32) NOT NULL,
  `spec` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `businessStatus` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `endDate` datetime NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVpcVpnGatewayVO` (`vSwitchUuid`,`accountName`,`gatewayId`) USING BTREE,
  KEY `fkVpcVpnGatewayVOAccountVO` (`accountName`),
  CONSTRAINT `fkVpcVpnGatewayVOAccountVO` FOREIGN KEY (`accountName`) REFERENCES `AccountVO` (`name`),
  CONSTRAINT `fkVpcVpnGatewayVOEcsVSwitchVO` FOREIGN KEY (`vSwitchUuid`) REFERENCES `EcsVSwitchVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnIkeConfigVO` (
  `uuid` varchar(32) NOT NULL,
  `accountName` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `psk` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `version` varchar(32) NOT NULL,
  `mode` varchar(32) NOT NULL,
  `encodeAlgorithm` varchar(32) NOT NULL,
  `authAlgorithm` varchar(32) NOT NULL,
  `pfs` varchar(32) NOT NULL,
  `lifetime` bigint(20) unsigned NOT NULL,
  `localIp` varchar(32) NOT NULL,
  `remoteIp` varchar(32) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVpcVpnIkeConfigVOAccountVO` (`accountName`),
  CONSTRAINT `fkVpcVpnIkeConfigVOAccountVO` FOREIGN KEY (`accountName`) REFERENCES `AccountVO` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnIpSecConfigVO` (
  `uuid` varchar(32) NOT NULL,
  `accountName` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `encodeAlgorithm` varchar(32) NOT NULL,
  `authAlgorithm` varchar(32) NOT NULL,
  `pfs` varchar(32) NOT NULL,
  `lifetime` bigint(20) unsigned NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `fkVpcVpnIpSecConfigVOAccountVO` (`accountName`),
  CONSTRAINT `fkVpcVpnIpSecConfigVOAccountVO` FOREIGN KEY (`accountName`) REFERENCES `AccountVO` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VtepVO` (
  `uuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vtepIp` varchar(32) NOT NULL,
  `port` int(11) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
  `physicalInterface` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVtepIpPoolUuid` (`vtepIp`,`poolUuid`) USING BTREE,
  KEY `fkVtepVOHostEO` (`hostUuid`),
  KEY `fkVtepVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkVtepVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVtepVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VxlanClusterMappingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vxlanUuid` varchar(32) NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `vlanId` int(11) DEFAULT NULL,
  `physicalInterface` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVxlanClusterMappingVOVxlanNetworkVO` (`vxlanUuid`),
  KEY `fkVxlanClusterMappingVOClusterEO` (`clusterUuid`),
  CONSTRAINT `fkVxlanClusterMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVxlanClusterMappingVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VxlanHostMappingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vxlanUuid` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vlanId` int(11) DEFAULT NULL,
  `physicalInterface` varchar(32) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkVxlanHostMappingVOVxlanNetworkVO` (`vxlanUuid`),
  KEY `fkVxlanHostMappingVOHostEO` (`hostUuid`),
  CONSTRAINT `fkVxlanHostMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVxlanHostMappingVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkVxlanNetworkPoolVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VxlanNetworkVO` (
  `uuid` varchar(32) NOT NULL,
  `vni` int(11) NOT NULL,
  `poolUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `ukVniPoolUuid` (`vni`,`poolUuid`) USING BTREE,
  KEY `fkVxlanNetworkVOVxlanNetworkPoolVO` (`poolUuid`),
  CONSTRAINT `fkVxlanNetworkVOL2NetworkEO` FOREIGN KEY (`uuid`) REFERENCES `L2NetworkEO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkVxlanNetworkVOVxlanNetworkPoolVO` FOREIGN KEY (`poolUuid`) REFERENCES `VxlanNetworkPoolVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `WebhookVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `url` varchar(2048) DEFAULT NULL,
  `type` varchar(255) NOT NULL,
  `opaque` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `WorkFlowChainVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'uuid made of name of workflow_flow',
  `name` varchar(255) NOT NULL,
  `owner` varchar(255) NOT NULL,
  `state` varchar(128) NOT NULL,
  `totalWorkFlows` int(11) NOT NULL,
  `currentPosition` int(11) NOT NULL,
  `OperationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reason` text,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `WorkFlowVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `chainUuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `state` varchar(128) NOT NULL,
  `reason` text,
  `position` int(11) NOT NULL,
  `OperationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `context` blob,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `XDragonHostVO` (
  `uuid` varchar(32) NOT NULL COMMENT 'host uuid',
  `cpuNum` int(10) unsigned NOT NULL DEFAULT '0',
  `cpuSockets` int(10) unsigned NOT NULL DEFAULT '1',
  `totalPhysicalMemory` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkXDragonHostVOHostEO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `XskyBlockVolumeVO` (
  `uuid` varchar(32) NOT NULL,
  `accessPathId` int(11) NOT NULL,
  `accessPathIqn` varchar(128) NOT NULL,
  `xskyStatus` varchar(32) DEFAULT NULL,
  `xskyBlockVolumeId` int(11) DEFAULT NULL,
  `burstTotalBw` bigint(20) DEFAULT NULL,
  `burstTotalIops` bigint(20) DEFAULT NULL,
  `maxTotalBw` bigint(20) DEFAULT NULL,
  `maxTotalIops` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkXskyBlockVolumeVOBlockVolumeVO` FOREIGN KEY (`uuid`) REFERENCES `BlockVolumeVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZBoxBackupVO` (
  `uuid` varchar(32) NOT NULL,
  `zBoxUuid` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`) USING BTREE,
  KEY `fkZBoxBackupVOZBoxVO` (`zBoxUuid`),
  CONSTRAINT `fkZBoxBackupVOExternalBackupVO` FOREIGN KEY (`uuid`) REFERENCES `ExternalBackupVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkZBoxBackupVOZBoxVO` FOREIGN KEY (`zBoxUuid`) REFERENCES `ZBoxVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZBoxLocationRefVO` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `zboxUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(256) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fkZBoxLocationRefVOZBoxVO` (`zboxUuid`),
  CONSTRAINT `fkZBoxLocationRefVOZBoxVO` FOREIGN KEY (`zboxUuid`) REFERENCES `ZBoxVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZBoxVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `state` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `mountPath` varchar(2048) DEFAULT NULL,
  `totalCapacity` bigint(20) unsigned DEFAULT NULL,
  `availableCapacity` bigint(20) unsigned DEFAULT NULL,
  `busNum` varchar(32) DEFAULT NULL,
  `devNum` varchar(32) DEFAULT NULL,
  `idVendor` varchar(32) DEFAULT NULL,
  `idProduct` varchar(32) DEFAULT NULL,
  `iManufacturer` varchar(1024) DEFAULT NULL,
  `iProduct` varchar(1024) DEFAULT NULL,
  `iSerial` varchar(1024) DEFAULT NULL,
  `usbVersion` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZStoneVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `managementIp` varchar(255) NOT NULL,
  `authorizationServer` varchar(32) NOT NULL,
  `logInPort` int(11) NOT NULL,
  `apiPort` int(11) NOT NULL,
  `logInUrl` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZceXThirdPartyPlatformAlertRefVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zceXUuid` char(32) NOT NULL,
  `thirdPartyPlatformUuid` char(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fkZceXThirdPartyPlatformAlertRefZceX` (`zceXUuid`),
  KEY `fkZceXThirdPartyPlatformAlertRefThirdPartyPlatform` (`thirdPartyPlatformUuid`),
  CONSTRAINT `fkZceXThirdPartyPlatformAlertRefZceX` FOREIGN KEY (`zceXUuid`) REFERENCES `ZceXVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkZceXThirdPartyPlatformAlertRefThirdPartyPlatform` FOREIGN KEY (`thirdPartyPlatformUuid`) REFERENCES `ThirdpartyPlatformVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZceXVO` (
  `uuid` char(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `managementIp` varchar(255) NOT NULL,
  `apiPort` int(11) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ZoneEO` (
  `uuid` varchar(32) NOT NULL COMMENT 'Zone uuid',
  `name` varchar(255) NOT NULL COMMENT 'Zone name',
  `type` varchar(255) NOT NULL COMMENT 'Zone type',
  `state` varchar(32) NOT NULL COMMENT 'Zone state',
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `deleted` varchar(255) DEFAULT NULL,
  `isDefault` tinyint(1) unsigned DEFAULT '0',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idxZoneEOname` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `check_point` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `uuid` varchar(40) NOT NULL,
  `state` varchar(128) NOT NULL,
  `context` blob,
  `op_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `check_point_entry` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `check_point_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `context` blob,
  `state` varchar(128) NOT NULL,
  `reason` varchar(1024) DEFAULT NULL,
  `op_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `person` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `age` int(10) unsigned DEFAULT NULL,
  `sex` varchar(40) NOT NULL DEFAULT 'male',
  `marriage` tinyint(1) unsigned NOT NULL,
  `title` varchar(12) NOT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `temp_array_table` (
  `idx` int(11) DEFAULT NULL,
  `value` varchar(128) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `temp_array_table` VALUE
(1,'AccountVO'),
(2,'AffinityGroupVO'),
(3,'AlarmVO'),
(4,'AlertVO'),
(5,'AliyunDiskVO'),
(6,'AliyunNasAccessGroupVO'),
(7,'AliyunNasAccessRuleVO'),
(8,'AliyunRouterInterfaceVO'),
(9,'AliyunSnapshotVO'),
(10,'BackupStorageVO'),
(11,'BaremetalChassisVO'),
(12,'BaremetalPxeServerVO'),
(13,'CephMonVO'),
(14,'CephPrimaryStoragePoolVO'),
(15,'CertificateVO'),
(16,'ClusterVO'),
(17,'ConnectionAccessPointVO'),
(18,'ConsoleProxyVO'),
(19,'DataCenterVO'),
(20,'DiskOfferingVO'),
(21,'EcsImageVO'),
(22,'EcsInstanceVO'),
(23,'EcsSecurityGroupRuleVO'),
(24,'EcsSecurityGroupVO'),
(25,'EcsVSwitchVO'),
(26,'EcsVpcVO'),
(27,'EipVO'),
(28,'EventSubscriptionVO'),
(29,'GarbageCollectorVO'),
(30,'HostVO'),
(31,'HybridAccountVO'),
(32,'HybridEipAddressVO'),
(33,'IPsecConnectionVO'),
(34,'IdentityZoneVO'),
(35,'ImageVO'),
(36,'InstanceOfferingVO'),
(37,'IpRangeVO'),
(38,'L2NetworkVO'),
(39,'L3NetworkVO'),
(40,'LdapServerVO'),
(41,'LoadBalancerListenerVO'),
(42,'LoadBalancerVO'),
(43,'LongJobVO'),
(44,'MediaVO'),
(45,'MonitorTriggerActionVO'),
(46,'MonitorTriggerVO'),
(47,'NasFileSystemVO'),
(48,'NasMountTargetVO'),
(49,'OssBucketVO'),
(50,'PciDeviceOfferingVO'),
(51,'PciDeviceVO'),
(53,'PortForwardingRuleVO'),
(54,'PrimaryStorageVO'),
(55,'QuotaVO'),
(56,'RoleVO'),
(57,'SNSApplicationEndpointVO'),
(58,'SNSApplicationPlatformVO'),
(59,'SNSDingTalkAtPersonVO'),
(60,'SNSTextTemplateVO'),
(61,'SNSTopicVO'),
(62,'SchedulerJobVO'),
(63,'SchedulerTriggerVO'),
(64,'SchedulerVO'),
(65,'SecurityGroupRuleVO'),
(66,'SecurityGroupVO'),
(67,'SharedBlockVO'),
(68,'TicketStatusHistoryVO'),
(69,'TicketVO'),
(70,'UsbDeviceVO'),
(73,'VCenterDatacenterVO'),
(74,'VCenterVO'),
(75,'VRouterRouteEntryVO'),
(76,'VRouterRouteTableVO'),
(77,'VipVO'),
(78,'VirtualBorderRouterVO'),
(79,'VmInstanceVO'),
(80,'VmNicVO'),
(81,'VniRangeVO'),
(82,'VolumeVO'),
(83,'VolumeSnapshotVO'),
(84,'VolumeSnapshotTreeVO'),
(85,'VpcUserVpnGatewayVO'),
(86,'VpcVirtualRouteEntryVO'),
(87,'VpcVirtualRouterVO'),
(88,'VpcVpnConnectionVO'),
(89,'VpcVpnGatewayVO'),
(90,'VpcVpnIkeConfigVO'),
(91,'VpcVpnIpSecConfigVO'),
(92,'VtepVO'),
(93,'ZoneVO');

CREATE VIEW `BackupStorageVO` AS select `BackupStorageEO`.`uuid` AS `uuid`,`BackupStorageEO`.`name` AS `name`,`BackupStorageEO`.`url` AS `url`,`BackupStorageEO`.`description` AS `description`,`BackupStorageEO`.`totalCapacity` AS `totalCapacity`,`BackupStorageEO`.`availableCapacity` AS `availableCapacity`,`BackupStorageEO`.`type` AS `type`,`BackupStorageEO`.`state` AS `state`,`BackupStorageEO`.`status` AS `status`,`BackupStorageEO`.`createDate` AS `createDate`,`BackupStorageEO`.`lastOpDate` AS `lastOpDate` from `BackupStorageEO` where isnull(`BackupStorageEO`.`deleted`);

CREATE VIEW `CdpPolicyVO` AS select `CdpPolicyEO`.`uuid` AS `uuid`,`CdpPolicyEO`.`name` AS `name`,`CdpPolicyEO`.`description` AS `description`,`CdpPolicyEO`.`retentionTimePerDay` AS `retentionTimePerDay`,`CdpPolicyEO`.`dailyRPSinceDay` AS `dailyRPSinceDay`,`CdpPolicyEO`.`expireTime` AS `expireTime`,`CdpPolicyEO`.`recoveryPointPerSecond` AS `recoveryPointPerSecond`,`CdpPolicyEO`.`fullBackupInterval` AS `fullBackupInterval`,`CdpPolicyEO`.`state` AS `state`,`CdpPolicyEO`.`lastOpDate` AS `lastOpDate`,`CdpPolicyEO`.`createDate` AS `createDate` from `CdpPolicyEO` where isnull(`CdpPolicyEO`.`deleted`);

CREATE VIEW `CephOsdGroupHistoricalUsageVO` AS select `PrimaryStorageHistoricalUsageBaseVO`.`id` AS `id`,`PrimaryStorageHistoricalUsageBaseVO`.`primaryStorageUuid` AS `primaryStorageUuid`,`PrimaryStorageHistoricalUsageBaseVO`.`resourceUuid` AS `osdGroupUuid`,`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` AS `resourceType`,`PrimaryStorageHistoricalUsageBaseVO`.`totalPhysicalCapacity` AS `totalPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`usedPhysicalCapacity` AS `usedPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`recordDate` AS `recordDate`,`PrimaryStorageHistoricalUsageBaseVO`.`createDate` AS `createDate`,`PrimaryStorageHistoricalUsageBaseVO`.`lastOpDate` AS `lastOpDate` from `PrimaryStorageHistoricalUsageBaseVO` where (`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` = 'CephOsdGroupVO');

CREATE VIEW `ClusterVO` AS select `ClusterEO`.`uuid` AS `uuid`,`ClusterEO`.`zoneUuid` AS `zoneUuid`,`ClusterEO`.`name` AS `name`,`ClusterEO`.`type` AS `type`,`ClusterEO`.`description` AS `description`,`ClusterEO`.`state` AS `state`,`ClusterEO`.`hypervisorType` AS `hypervisorType`,`ClusterEO`.`createDate` AS `createDate`,`ClusterEO`.`lastOpDate` AS `lastOpDate`,`ClusterEO`.`managementNodeId` AS `managementNodeId`,`ClusterEO`.`architecture` AS `architecture` from `ClusterEO` where isnull(`ClusterEO`.`deleted`);

CREATE VIEW `DiskOfferingVO` AS select `DiskOfferingEO`.`uuid` AS `uuid`,`DiskOfferingEO`.`name` AS `name`,`DiskOfferingEO`.`description` AS `description`,`DiskOfferingEO`.`diskSize` AS `diskSize`,`DiskOfferingEO`.`sortKey` AS `sortKey`,`DiskOfferingEO`.`type` AS `type`,`DiskOfferingEO`.`state` AS `state`,`DiskOfferingEO`.`createDate` AS `createDate`,`DiskOfferingEO`.`lastOpDate` AS `lastOpDate`,`DiskOfferingEO`.`allocatorStrategy` AS `allocatorStrategy` from `DiskOfferingEO` where isnull(`DiskOfferingEO`.`deleted`);

CREATE VIEW `HostVO` AS select `HostEO`.`uuid` AS `uuid`,`HostEO`.`zoneUuid` AS `zoneUuid`,`HostEO`.`clusterUuid` AS `clusterUuid`,`HostEO`.`name` AS `name`,`HostEO`.`description` AS `description`,`HostEO`.`managementIp` AS `managementIp`,`HostEO`.`hypervisorType` AS `hypervisorType`,`HostEO`.`state` AS `state`,`HostEO`.`status` AS `status`,`HostEO`.`architecture` AS `architecture`,`HostEO`.`nqn` AS `nqn`,`HostEO`.`hostname` AS `hostname`,`HostEO`.`createDate` AS `createDate`,`HostEO`.`lastOpDate` AS `lastOpDate` from `HostEO` where isnull(`HostEO`.`deleted`);

CREATE VIEW `ImageVO` AS select `ImageEO`.`uuid` AS `uuid`,`ImageEO`.`name` AS `name`,`ImageEO`.`description` AS `description`,`ImageEO`.`status` AS `status`,`ImageEO`.`state` AS `state`,`ImageEO`.`size` AS `size`,`ImageEO`.`actualSize` AS `actualSize`,`ImageEO`.`md5sum` AS `md5Sum`,`ImageEO`.`platform` AS `platform`,`ImageEO`.`type` AS `type`,`ImageEO`.`format` AS `format`,`ImageEO`.`url` AS `url`,`ImageEO`.`system` AS `system`,`ImageEO`.`mediaType` AS `mediaType`,`ImageEO`.`guestOsType` AS `guestOsType`,`ImageEO`.`architecture` AS `architecture`,`ImageEO`.`virtio` AS `virtio`,`ImageEO`.`createDate` AS `createDate`,`ImageEO`.`lastOpDate` AS `lastOpDate` from `ImageEO` where isnull(`ImageEO`.`deleted`);

CREATE VIEW `InstanceOfferingVO` AS select `InstanceOfferingEO`.`uuid` AS `uuid`,`InstanceOfferingEO`.`name` AS `name`,`InstanceOfferingEO`.`description` AS `description`,`InstanceOfferingEO`.`cpuNum` AS `cpuNum`,`InstanceOfferingEO`.`cpuSpeed` AS `cpuSpeed`,`InstanceOfferingEO`.`memorySize` AS `memorySize`,`InstanceOfferingEO`.`reservedMemorySize` AS `reservedMemorySize`,`InstanceOfferingEO`.`allocatorStrategy` AS `allocatorStrategy`,`InstanceOfferingEO`.`sortKey` AS `sortKey`,`InstanceOfferingEO`.`state` AS `state`,`InstanceOfferingEO`.`createDate` AS `createDate`,`InstanceOfferingEO`.`lastOpDate` AS `lastOpDate`,`InstanceOfferingEO`.`type` AS `type`,`InstanceOfferingEO`.`duration` AS `duration` from `InstanceOfferingEO` where isnull(`InstanceOfferingEO`.`deleted`);

CREATE VIEW `IpRangeVO` AS select `IpRangeEO`.`uuid` AS `uuid`,`IpRangeEO`.`l3NetworkUuid` AS `l3NetworkUuid`,`IpRangeEO`.`name` AS `name`,`IpRangeEO`.`description` AS `description`,`IpRangeEO`.`startIp` AS `startIp`,`IpRangeEO`.`endIp` AS `endIp`,`IpRangeEO`.`netmask` AS `netmask`,`IpRangeEO`.`gateway` AS `gateway`,`IpRangeEO`.`networkCidr` AS `networkCidr`,`IpRangeEO`.`createDate` AS `createDate`,`IpRangeEO`.`lastOpDate` AS `lastOpDate`,`IpRangeEO`.`ipVersion` AS `ipVersion`,`IpRangeEO`.`addressMode` AS `addressMode`,`IpRangeEO`.`prefixLen` AS `prefixLen` from `IpRangeEO` where isnull(`IpRangeEO`.`deleted`);

CREATE VIEW `L2NetworkVO` AS select `L2NetworkEO`.`uuid` AS `uuid`,`L2NetworkEO`.`name` AS `name`,`L2NetworkEO`.`description` AS `description`,`L2NetworkEO`.`type` AS `type`,`L2NetworkEO`.`vSwitchType` AS `vSwitchType`,`L2NetworkEO`.`virtualNetworkId` AS `virtualNetworkId`,`L2NetworkEO`.`zoneUuid` AS `zoneUuid`,`L2NetworkEO`.`physicalInterface` AS `physicalInterface`,`L2NetworkEO`.`createDate` AS `createDate`,`L2NetworkEO`.`lastOpDate` AS `lastOpDate` from `L2NetworkEO` where isnull(`L2NetworkEO`.`deleted`);

CREATE VIEW `L3NetworkVO` AS select `L3NetworkEO`.`uuid` AS `uuid`,`L3NetworkEO`.`name` AS `name`,`L3NetworkEO`.`description` AS `description`,`L3NetworkEO`.`state` AS `state`,`L3NetworkEO`.`type` AS `type`,`L3NetworkEO`.`zoneUuid` AS `zoneUuid`,`L3NetworkEO`.`l2NetworkUuid` AS `l2NetworkUuid`,`L3NetworkEO`.`system` AS `system`,`L3NetworkEO`.`dnsDomain` AS `dnsDomain`,`L3NetworkEO`.`createDate` AS `createDate`,`L3NetworkEO`.`lastOpDate` AS `lastOpDate`,`L3NetworkEO`.`category` AS `category`,`L3NetworkEO`.`ipVersion` AS `ipVersion`,`L3NetworkEO`.`enableIPAM` AS `enableIPAM` from `L3NetworkEO` where isnull(`L3NetworkEO`.`deleted`);

CREATE VIEW `LocalStorageHostHistoricalUsageVO` AS select `PrimaryStorageHistoricalUsageBaseVO`.`id` AS `id`,`PrimaryStorageHistoricalUsageBaseVO`.`primaryStorageUuid` AS `primaryStorageUuid`,`PrimaryStorageHistoricalUsageBaseVO`.`resourceUuid` AS `hostUuid`,`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` AS `resourceType`,`PrimaryStorageHistoricalUsageBaseVO`.`totalPhysicalCapacity` AS `totalPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`usedPhysicalCapacity` AS `usedPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`recordDate` AS `recordDate`,`PrimaryStorageHistoricalUsageBaseVO`.`createDate` AS `createDate`,`PrimaryStorageHistoricalUsageBaseVO`.`lastOpDate` AS `lastOpDate` from `PrimaryStorageHistoricalUsageBaseVO` where (`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` = 'LocalStorageHostRefVO');

CREATE VIEW `PrimaryStorageHistoricalUsageVO` AS select `PrimaryStorageHistoricalUsageBaseVO`.`id` AS `id`,`PrimaryStorageHistoricalUsageBaseVO`.`primaryStorageUuid` AS `primaryStorageUuid`,`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` AS `resourceType`,`PrimaryStorageHistoricalUsageBaseVO`.`totalPhysicalCapacity` AS `totalPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`usedPhysicalCapacity` AS `usedPhysicalCapacity`,`PrimaryStorageHistoricalUsageBaseVO`.`recordDate` AS `recordDate`,`PrimaryStorageHistoricalUsageBaseVO`.`createDate` AS `createDate`,`PrimaryStorageHistoricalUsageBaseVO`.`lastOpDate` AS `lastOpDate` from `PrimaryStorageHistoricalUsageBaseVO` where (`PrimaryStorageHistoricalUsageBaseVO`.`resourceType` = 'PrimaryStorageVO');

CREATE VIEW `PrimaryStorageVO` AS select `PrimaryStorageEO`.`uuid` AS `uuid`,`PrimaryStorageEO`.`zoneUuid` AS `zoneUuid`,`PrimaryStorageEO`.`name` AS `name`,`PrimaryStorageEO`.`url` AS `url`,`PrimaryStorageEO`.`description` AS `description`,`PrimaryStorageEO`.`type` AS `type`,`PrimaryStorageEO`.`mountPath` AS `mountPath`,`PrimaryStorageEO`.`state` AS `state`,`PrimaryStorageEO`.`status` AS `status`,`PrimaryStorageEO`.`createDate` AS `createDate`,`PrimaryStorageEO`.`lastOpDate` AS `lastOpDate` from `PrimaryStorageEO` where isnull(`PrimaryStorageEO`.`deleted`);

CREATE VIEW `ScsiLunVO` AS select `LunVO`.`uuid` AS `uuid`,`LunVO`.`name` AS `name`,`LunVO`.`wwid` AS `wwid`,`LunVO`.`vendor` AS `vendor`,`LunVO`.`model` AS `model`,`LunVO`.`wwn` AS `wwn`,`LunVO`.`serial` AS `serial`,`LunVO`.`type` AS `type`,`LunVO`.`hctl` AS `hctl`,`LunVO`.`path` AS `path`,`LunVO`.`size` AS `size`,`LunVO`.`state` AS `state`,`LunVO`.`source` AS `source`,`LunVO`.`multipathDeviceUuid` AS `multipathDeviceUuid`,`LunVO`.`createDate` AS `createDate`,`LunVO`.`lastOpDate` AS `lastOpDate` from `LunVO` where (`LunVO`.`source` in ('iSCSI','fiberChannel'));

CREATE VIEW `VmInstanceVO` AS select `VmInstanceEO`.`uuid` AS `uuid`,`VmInstanceEO`.`name` AS `name`,`VmInstanceEO`.`description` AS `description`,`VmInstanceEO`.`zoneUuid` AS `zoneUuid`,`VmInstanceEO`.`clusterUuid` AS `clusterUuid`,`VmInstanceEO`.`imageUuid` AS `imageUuid`,`VmInstanceEO`.`hostUuid` AS `hostUuid`,`VmInstanceEO`.`internalId` AS `internalId`,`VmInstanceEO`.`lastHostUuid` AS `lastHostUuid`,`VmInstanceEO`.`instanceOfferingUuid` AS `instanceOfferingUuid`,`VmInstanceEO`.`rootVolumeUuid` AS `rootVolumeUuid`,`VmInstanceEO`.`defaultL3NetworkUuid` AS `defaultL3NetworkUuid`,`VmInstanceEO`.`type` AS `type`,`VmInstanceEO`.`hypervisorType` AS `hypervisorType`,`VmInstanceEO`.`cpuNum` AS `cpuNum`,`VmInstanceEO`.`cpuSpeed` AS `cpuSpeed`,`VmInstanceEO`.`memorySize` AS `memorySize`,`VmInstanceEO`.`reservedMemorySize` AS `reservedMemorySize`,`VmInstanceEO`.`platform` AS `platform`,`VmInstanceEO`.`guestOsType` AS `guestOsType`,`VmInstanceEO`.`allocatorStrategy` AS `allocatorStrategy`,`VmInstanceEO`.`createDate` AS `createDate`,`VmInstanceEO`.`lastOpDate` AS `lastOpDate`,`VmInstanceEO`.`state` AS `state`,`VmInstanceEO`.`architecture` AS `architecture` from `VmInstanceEO` where isnull(`VmInstanceEO`.`deleted`);

CREATE VIEW `VolumeSnapshotTreeVO` AS select `VolumeSnapshotTreeEO`.`uuid` AS `uuid`,`VolumeSnapshotTreeEO`.`volumeUuid` AS `volumeUuid`,`VolumeSnapshotTreeEO`.`rootImageUuid` AS `rootImageUuid`,`VolumeSnapshotTreeEO`.`current` AS `current`,`VolumeSnapshotTreeEO`.`status` AS `status`,`VolumeSnapshotTreeEO`.`createDate` AS `createDate`,`VolumeSnapshotTreeEO`.`lastOpDate` AS `lastOpDate` from `VolumeSnapshotTreeEO` where isnull(`VolumeSnapshotTreeEO`.`deleted`);

CREATE VIEW `VolumeSnapshotVO` AS select `VolumeSnapshotEO`.`uuid` AS `uuid`,`VolumeSnapshotEO`.`name` AS `name`,`VolumeSnapshotEO`.`description` AS `description`,`VolumeSnapshotEO`.`type` AS `type`,`VolumeSnapshotEO`.`volumeUuid` AS `volumeUuid`,`VolumeSnapshotEO`.`format` AS `format`,`VolumeSnapshotEO`.`treeUuid` AS `treeUuid`,`VolumeSnapshotEO`.`parentUuid` AS `parentUuid`,`VolumeSnapshotEO`.`primaryStorageUuid` AS `primaryStorageUuid`,`VolumeSnapshotEO`.`primaryStorageInstallPath` AS `primaryStorageInstallPath`,`VolumeSnapshotEO`.`distance` AS `distance`,`VolumeSnapshotEO`.`size` AS `size`,`VolumeSnapshotEO`.`latest` AS `latest`,`VolumeSnapshotEO`.`fullSnapshot` AS `fullSnapshot`,`VolumeSnapshotEO`.`volumeType` AS `volumeType`,`VolumeSnapshotEO`.`state` AS `state`,`VolumeSnapshotEO`.`status` AS `status`,`VolumeSnapshotEO`.`createDate` AS `createDate`,`VolumeSnapshotEO`.`lastOpDate` AS `lastOpDate` from `VolumeSnapshotEO` where isnull(`VolumeSnapshotEO`.`deleted`);

CREATE VIEW `VolumeVO` AS select `VolumeEO`.`uuid` AS `uuid`,`VolumeEO`.`name` AS `name`,`VolumeEO`.`description` AS `description`,`VolumeEO`.`primaryStorageUuid` AS `primaryStorageUuid`,`VolumeEO`.`vmInstanceUuid` AS `vmInstanceUuid`,`VolumeEO`.`diskOfferingUuid` AS `diskOfferingUuid`,`VolumeEO`.`rootImageUuid` AS `rootImageUuid`,`VolumeEO`.`installPath` AS `installPath`,`VolumeEO`.`type` AS `type`,`VolumeEO`.`status` AS `status`,`VolumeEO`.`size` AS `size`,`VolumeEO`.`actualSize` AS `actualSize`,`VolumeEO`.`deviceId` AS `deviceId`,`VolumeEO`.`format` AS `format`,`VolumeEO`.`state` AS `state`,`VolumeEO`.`createDate` AS `createDate`,`VolumeEO`.`lastOpDate` AS `lastOpDate`,`VolumeEO`.`isShareable` AS `isShareable`,`VolumeEO`.`volumeQos` AS `volumeQos`,`VolumeEO`.`lastVmInstanceUuid` AS `lastVmInstanceUuid`,`VolumeEO`.`lastDetachDate` AS `lastDetachDate`,`VolumeEO`.`lastAttachDate` AS `lastAttachDate`,`VolumeEO`.`protocol` AS `protocol` from `VolumeEO` where isnull(`VolumeEO`.`deleted`);

CREATE VIEW `ZoneVO` AS select `ZoneEO`.`uuid` AS `uuid`,`ZoneEO`.`name` AS `name`,`ZoneEO`.`type` AS `type`,`ZoneEO`.`description` AS `description`,`ZoneEO`.`state` AS `state`,`ZoneEO`.`isDefault` AS `isDefault`,`ZoneEO`.`createDate` AS `createDate`,`ZoneEO`.`lastOpDate` AS `lastOpDate` from `ZoneEO` where isnull(`ZoneEO`.`deleted`);
