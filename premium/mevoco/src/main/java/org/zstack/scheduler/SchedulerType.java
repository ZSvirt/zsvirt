package org.zstack.scheduler;

import org.zstack.header.vm.*;

/**
 * Created by AlanJager on 2017/6/10.
 */
public interface SchedulerType {
    String VM = VmInstanceVO.class.getSimpleName();
    String START_VM = "startVm";
    String STOP_VM = "stopVm";
    String REBOOT_VM = "rebootVm";

    String VOLUME_SNAPSHOT = "volumeSnapshot";
    String VOLUME_SNAPSHOT_GROUP = "volumeSnapshotGroup";

    String VOLUME_BACKUP = "volumeBackup";
    String ROOT_VOLUME_BACKUP = "rootVolumeBackup";
    String VM_BACKUP = "vmBackup";
    String DATABASE_BACKUP = "databaseBackup";

    String LOCAL_RAID_SELF_TEST = "localRaidSelfTest";
    String RUN_AUTO_SCALING_GROUP = "runAutoScalingGroup";
    String CANCEL_IAM2_PROJECT_LOGIN_EXPIRED = "cancelIAM2ProjectLoginExpired";
    String ADD_IAM2_PROJECT_LOGIN_EXPIRED = "addIAM2ProjectLoginExpired";

}
