package org.zstack.header.storageDevice;

import org.zstack.header.vm.VmInstanceState;

import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/8/3
 */
public class StorageDeviceConstants {
    public static final String SERVICE_ID = "storageDevice";

    public static final String FIBER_CHANNEL = "fiberChannel";
    public static final String NVME = "NVMe";
    public static final String ISCSI = "iSCSI";

    public static final List<VmInstanceState> allowedVmOperationStates = Arrays.asList(
            VmInstanceState.Running, VmInstanceState.Paused, VmInstanceState.Stopped);

    public static final String MULTIPATH = "mpath";
    public static final String DISK = "disk";

    public static final String STORAGE_DEVICE_ALLOCATOR_STRATEGY = "StorageDeviceStrategy";

    public static final String LOCAL_RAID_SELF_TEST_JOB_NAME = "Local raid self test job";
    public static final String LOCAL_RAID_SELF_TEST_JOB_UUID = "f757f49c1624f552611eabf46995a508";

    public static final String LOCAL_RAID_SELF_TEST_TRIGGER_NAME = "Local raid self test trigger";
    public static final String LOCAL_RAID_SELF_TEST_TRIGGER_UUID = "112a238ebe24f57cab33dd3585d65933";
}
