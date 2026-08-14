package org.zstack.header.storage.snapshot;

import org.zstack.header.vm.VmInstanceState;

import java.util.Arrays;
import java.util.List;

/**
 */
public interface VolumeSnapshotConstant {
    String SERVICE_ID = "snapshot.volume";

    String ACTION_CATEGORY = "volumeSnapshot";

    VolumeSnapshotType HYPERVISOR_SNAPSHOT_TYPE = new VolumeSnapshotType("Hypervisor");
    VolumeSnapshotType STORAGE_SNAPSHOT_TYPE = new VolumeSnapshotType("Storage");

    String SNAPSHOT_MESSAGE_ROUTED = "SNAPSHOT_MESSAGE_ROUTED";
    String SNAPSHOT_UUID = "SNAPSHOT_UUID";

    String VOLUME_SNAPSHOT_STRUCT = "VolumeSnapshotStruct";
    String NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR = "needTakeSnapshotOnHypervisor";
    String NEED_BLOCK_STREAM_ON_HYPERVISOR = "needBlockStreamOnHypervisor";

    public static final List<VmInstanceState> ALLOW_TAKE_SNAPSHOTS_VM_STATES = Arrays.asList(
            VmInstanceState.Running, VmInstanceState.Stopped, VmInstanceState.Paused
    );
    public static final List<VmInstanceState> ALLOW_TAKE_MEMORY_SNAPSHOTS_VM_STATES = Arrays.asList(
            VmInstanceState.Stopped
    );
}
