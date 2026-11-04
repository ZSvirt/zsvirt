-- in version zsv_4.1.6

ALTER TABLE AuditsVO ADD COLUMN `resourceName` varchar(255) DEFAULT NULL;

ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `deviceName` VARCHAR(64) DEFAULT NULL AFTER `deviceId`;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `vendorName` VARCHAR(64) DEFAULT NULL AFTER `deviceName`;
ALTER TABLE `zstack`.`HostNetworkInterfaceVO` ADD COLUMN `subvendorName` VARCHAR(64) DEFAULT NULL AFTER `subdeviceId`;

-- from issue: (feature) ZSV-4408
ALTER TABLE `zstack`.`VmSchedHistoryVO` ADD COLUMN `schedReason` text DEFAULT NULL;
ALTER TABLE `zstack`.`VmSchedHistoryVO` ADD COLUMN `failReason` text DEFAULT NULL;
