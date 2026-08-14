package org.zstack.storage.volume;

import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.vm.VmInstanceState;

import java.util.List;

/**
 * Create by weiwang at 2018/6/8
 */
public class VolumeMevocoConstants {
    public static final List<VmInstanceState> ALLOW_TAKE_SNAPSHOTS_VM_STATES =
            VolumeSnapshotConstant.ALLOW_TAKE_SNAPSHOTS_VM_STATES;

    public static int VOLUME_MULTI_QUEUES_MIN_NUM = 0;
}
