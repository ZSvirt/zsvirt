DROP PROCEDURE IF EXISTS AddFkPciDeviceVOVmInstanceEO;
DELIMITER $$
CREATE PROCEDURE AddFkPciDeviceVOVmInstanceEO()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_TYPE = 'FOREIGN KEY' AND TABLE_NAME = 'PciDeviceVO' AND CONSTRAINT_NAME = 'fkPciDeviceVOVmInstanceEO') = 0 THEN
ALTER TABLE PciDeviceVO
    ADD CONSTRAINT fkPciDeviceVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO(uuid) ON DELETE SET NULL;
END IF;
END $$
DELIMITER ;
CALL AddFkPciDeviceVOVmInstanceEO();

update AlarmVO set name = 'Monitor IP Unreachable' where uuid='065f9609dce141bb952c80f729f58af4';
update EventSubscriptionVO set name = 'Management Node Connected' where uuid='10d9c4e69fc2456bb8c6c6d456bb5038';
update EventSubscriptionVO set name = 'SMS Message Sending Failed' where uuid='14a991d4d7d54a66b14e398ffc510bd6';
update EventSubscriptionVO set name = 'Host Connected' where uuid='1a7a3eb433904df89f5c42a1fa4e0716';
update EventSubscriptionVO set name = 'VPC vRouter Disk Space is Occupied by Abnormal Files' where uuid='33de14ed204948daa850f9b9a3a02e89';
update AlarmVO set name = 'VPC vRouter CPU Utilization Average' where uuid='369eef54655548eab2a4d2d7ef061c79';
update EventSubscriptionVO set name = 'VPC vRouter Connected' where uuid='39d2b6689efa4e4a96c239716cb6f3ea';
update AlarmVO set name = 'Backup Storage Available Capacity' where uuid='44e6f054a59a451fb1b535accff64fc2';
update EventSubscriptionVO set name = 'CDP Task Status Abnormally Changed' where uuid='47388fa83669736d6ed8776d7ed384g4';
update EventSubscriptionVO set name = 'NIC IP Configured in VM Instance Has Been Occupied on Cloud (GuestTools Required)' where uuid='4a3494bcdbac4eaab9e9e56e27d74a2a';
update EventSubscriptionVO set name = 'Host Disconnected' where uuid='4a3cb114b10d41e19545ab693222c134';
update EventSubscriptionVO set name = 'Backup Storage Connected' where uuid='55365763fed244c39b4642bef6c5daf9';
update EventSubscriptionVO set name = 'QEMU Version of Hosts in Cluster Needs to be Updated' where uuid='559ca06aa8bba6990d10c255e4c9ab5b';
update AlarmVO set name = 'VPC vRouter Memory Percent Used' where uuid='582ea79cb57d45a8bfd4d2030244c1c4';
update AlarmVO set name = 'Host Root Volume Utilization' where uuid='5d3bb9d271a349b283893317f531f723';
update EventSubscriptionVO set name = 'Primary Storage Disconnected' where uuid='5e75230bd2ea4f47abf6ff92fa816a20';
update AlarmVO set name = 'Average CPU Utilization of Hosts' where uuid='5z6gsgkc5kccpylj9ocgbd647p2700b7';
update AlarmVO set name = 'License Expiration' where uuid='65e8f1a4892231b692cc7a881581f3da';
update AlarmVO set name = 'CDP Task RPO Latency' where uuid='66898fa836694f7665d49b74dedf7631';
update AlarmVO set name = 'Primary Storage Available Capacity' where uuid='66dfdee6fd314aac96ca3779774ad977';
update EventSubscriptionVO set name = 'VM HA Started On Host' where uuid='6nz3vn2e0rdwu5hzmuetzv37ak0nj248';
update AlarmVO set name = 'Dual Management Node Database Needs Synchronization' where uuid='712c3dec6aa94ed2b3bcd32192c22f69';
update AlarmVO set name = 'Capacity Used by CDP Task' where uuid='78898fa836694f769ed89b74ded006f1';
update EventSubscriptionVO set name = 'LB Instance Disk Space is Occupied by Abnormal Files' where uuid='79b0dad6607a429cb235ad2f701718a0';
update EventSubscriptionVO set name = 'LB Instance Disconnected' where uuid='842e20d7d9844ee3a3c2a4224235a7df';
update EventSubscriptionVO set name = 'Management Node Disconnected' where uuid='8eca1096feb34419913087d2b281ecec';
update EventSubscriptionVO set name = 'Failed to Detect Connection Between Primary Storage and Host' where uuid='8tlwqj65mus1gdolu3w61yy35pvwinhz';
update EventSubscriptionVO set name = 'VM NIC IP Changed (GuestTools Required)' where uuid='98536fa94e3f4481a38331a989132b7c';
update EventSubscriptionVO set name = 'Backup Storage Disconnected' where uuid='98f9c802604e4852bd84716f66cf4f73';
update EventSubscriptionVO set name = 'CDP Task Failed' where uuid='98h262f95c1987fg2ba1be4a3562765f';
update EventSubscriptionVO set name = 'Host NIC Disconnected' where uuid='9a593ad138bf44138b72e0f0dd989f27';
update EventSubscriptionVO set name = 'VM Crashed' where uuid='a391bb01fd954ed3b6c0569ecc7b5764';
update EventSubscriptionVO set name = 'VPC vRouter State Changed to Paused' where uuid='a3d9fd893fbb4468867a7880b6b91ba6';
update AlarmVO set name = 'System Data Directory Disk Capacity' where uuid='b632652cc16044cdb6b4f516ed93a118';
update EventSubscriptionVO set name = 'VPC vRouter Failover' where uuid='bd0163e7028644a5b482534c2711d2d9';
update AlarmVO set name = 'Host Memory Utilization' where uuid='d0b35ac37c58e358cb74e664532f1044';
update EventSubscriptionVO set name = 'Host NIC Connected' where uuid='d1d122f95c194c958ba1be4a3568ebd0';
update EventSubscriptionVO set name = 'VPC vRouter Disconnected' where uuid='d59397479d2548d7abfe4ad31a575390';
update AlarmVO set name = 'Primary Storage Available Physical Capacity' where uuid='ded02f9786444c6296e9bc3efb8eb484';
update EventSubscriptionVO set name = 'VM Host Abnormally Changed' where uuid='eccfc93109cd4c71b56a2612d84a2773';
update EventSubscriptionVO set name = 'LB Instance Connected' where uuid='eef29da3aff8486093d6afabb05cddbf';
update AlarmVO set name = 'VPC vRouter Disk Capacity Percent Used Sum' where uuid='f3389a28b7d64e35875992d254ff4f96';
update EventSubscriptionVO set name = 'Primary Storage Connected' where uuid='f56795b8c34b452f84bcf25cb89bded2';
update AlarmVO set name = 'VM Memory Utilization' where uuid='fuz2p4fa71urf4fd7cknoxsalvj60ynk';
update EventSubscriptionVO set name = 'Host Mount Path Faulted' where uuid='g0eviogong06nubt1kj54z63pcka81sw';
update EventSubscriptionVO set name = 'VM Migration Failed as Host in Maintenance Mode' where uuid='krdu1hs2314kt18ttgqndaynxchs2ufc';
update EventSubscriptionVO set name = 'VM in Shutdown State for a Long Time' where uuid='ppfazo1y3tjvup4jfetxz36y3su98ngc';
update EventSubscriptionVO set name = 'Unknown VM Detected On Host' where uuid='rlwalvvqyoujj3ign3o309p2zulwbhwm';
update AlarmVO set name = 'Host Memory Used Capacity Per Host alarm' where uuid='ue0x30t7wfyuba87nwk6ywu3ub5svtwk';
update AlarmVO set name = 'Average CPU Utilization of VM Instances' where uuid='uhgfoh0soh6e1qai005elfa9c6h2s2y0';
update EventSubscriptionVO set name = 'HSM Exception Notification' where uuid='d3ba391bb01fd954eecc7b576c056964';
update EventSubscriptionVO set name = '3rd-Party Cryptographic Service Error' where uuid='eecc7b576c05391bb01fd956964d3ba4';

update AlarmVO set name = 'CPU Temperature' where uuid = '10908b6b7b8741139a5efc2b1d461a16';
update AlarmVO set name = 'SSD Remaining Life Expectancy' where uuid = 'a3905b37d5e8477dbe0fb75a30b8f21c';
update AlarmVO set name = 'SSD Temperature' where uuid = '33198a88f22e4d19b5ff8ebaebb6ujm7';
update EventSubscriptionVO set name = 'Host Physical Memory Ecc Error Triggered' where uuid = '258ac24eba3443dea73cbcb7758e6759';
update EventSubscriptionVO set name = 'Host Physical Memory Status Abnormal' where uuid = '03c7c6a8ab4f492bbe06c8b03ba25f27';
update EventSubscriptionVO set name = 'Host Physical Disk Remove Triggered' where uuid = '226da68a97b94b7f9f918f8ac7138873';
update EventSubscriptionVO set name = 'Host Physical Disk Insert Triggered' where uuid = '4c230269620e4e739ebc5ab2a6b0f0a4';
update EventSubscriptionVO set name = 'Host Physical Disk Status Abnormal' where uuid = '4b04f06e4ba24231ad67bd6f06093ba2';
update EventSubscriptionVO set name = 'Host Physical Cpu Status Abnormal' where uuid = '8186fbbeab1d449b93e7be78d6045c7f';
update EventSubscriptionVO set name = 'Host Physical Fan Status Abnormal' where uuid = '37e5bdfa2eaf49538931113ddaecf927';

CALL ADD_COLUMN('VmNicVO', 'state', 'varchar(255)', 0, 'enable');

CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `initiatorName` varchar(256) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkBlockPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `zstack`.`PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL DROP_COLUMN('BlockPrimaryStorageVO', 'encryptGatewayIp');
CALL DROP_COLUMN('BlockPrimaryStorageVO', 'encryptGatewayPort');
CALL DROP_COLUMN('BlockPrimaryStorageVO', 'encryptGatewayUsername');
CALL DROP_COLUMN('BlockPrimaryStorageVO', 'encryptGatewayPassword');
CALL DROP_COLUMN('BlockScsiLunVO', 'type');

DROP PROCEDURE IF EXISTS `AlterBlockScsiLunTable`;
DELIMITER $$
CREATE PROCEDURE AlterBlockScsiLunTable()
    BEGIN
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'id'
                            AND column_type like 'smallint%') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `id` int unsigned default 0;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'lunType'
                            AND is_nullable = 'NO') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN lunType varchar(256) DEFAULT NULL;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'lunMapId'
                            AND column_type like 'smallint%') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `lunMapId` int unsigned default 0;
        END IF;
    END $$
DELIMITER ;
call AlterBlockScsiLunTable;
DROP PROCEDURE IF EXISTS `AlterBlockScsiLunTable`;

DELIMITER $$
CREATE PROCEDURE checkAllBlockHostInPrimaryHostRef()
    BEGIN
        DECLARE hostUuid VARCHAR(32);
        DECLARE primaryStorageUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT hiref.hostUuid, hiref.primaryStorageUuid FROM HostInitiatorRefVO hiref;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO hostUuid, primaryStorageUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from PrimaryStorageHostRefVO pshref where pshref.hostUuid = hostUuid and pshref.primaryStorageUuid = primaryStorageUuid) = 0 THEN
                BEGIN
                    INSERT INTO zstack.PrimaryStorageHostRefVO (`primaryStorageUuid`, `hostUuid`, `status`, `lastOpDate`, `createDate`) values (primaryStorageUuid, hostUuid, 'Disconnected', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE migrateBlockPrimaryHostRef()
    BEGIN
        DECLARE initiatorName VARCHAR(256);
        DECLARE psId BIGINT(20);
        DECLARE metadata text;
        DECLARE psUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT  hostInitiatorRef.initiatorName, hostInitiatorRef.metadata, primaryStorageHostRef.id
             FROM zstack.HostInitiatorRefVO hostInitiatorRef, zstack.PrimaryStorageHostRefVO primaryStorageHostRef
             where hostInitiatorRef.hostUuid = primaryStorageHostRef.hostUuid and hostInitiatorRef.primaryStorageUuid = primaryStorageHostRef.primaryStorageUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done =TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO initiatorName, metadata, psId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from BlockPrimaryStorageHostRefVO bpshref where id = psId) = 0 THEN
                BEGIN
                    INSERT INTO zstack.BlockPrimaryStorageHostRefVO(id, initiatorName, metadata) values(psId, initiatorName, metadata);
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE checkHostInitiatorRefVO()
    BEGIN
        IF (SELECT count(*) FROM information_schema.columns WHERE table_name = 'HostInitiatorRefVO' AND column_name = 'primaryStorageUuid') != 0 THEN
            call checkAllBlockHostInPrimaryHostRef();
            call migrateBlockPrimaryHostRef();
        END IF;
    END $$
DELIMITER ;
call checkHostInitiatorRefVO();
DROP PROCEDURE IF EXISTS migrateBlockPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkAllBlockHostInPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkHostInitiatorRefVO;
DROP TABLE IF EXISTS HostInitiatorRefVO;

UPDATE ResourceConfigVO SET createDate = CURRENT_TIMESTAMP where name='iam2.force.enable.securityGroup' and createDate='0000-00-00 00:00:00';

update AlarmLabelVO set value='/var/lib/zstack/' where alarmUuid='b632652cc16044cdb6b4f516ed93a118' and value='/var/lib/zstack';