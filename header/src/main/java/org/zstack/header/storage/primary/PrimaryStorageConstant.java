package org.zstack.header.storage.primary;

import org.zstack.header.configuration.PythonClass;

import java.util.Arrays;
import java.util.List;

@PythonClass
public interface PrimaryStorageConstant {
    String SERVICE_ID = "storage.primary";
    @PythonClass
    String DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "DefaultPrimaryStorageAllocationStrategy";
    String LEAST_VOLUME_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "LeastVolumePrimaryStorageAllocationStrategy";
    String MAXIMUM_AVAILABLE_CAPACITY_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "MaximumAvailableCapacityAllocationStrategy";
    String CUSTOM_ORDER_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE = "CustomOrderPrimaryStorageAllocationStrategy";
    String VM_FOLDER = "vm";
    String PRIMARY_STORAGE_DETACH_CODE = "primaryStorage.detach";

    String MIGRATE_VOLUME_BACKING_FILE_GET_MD5_STAGE = "0-5";
    String MIGRATE_VOLUME_BACKING_FILE_COPY_STAGE = "5-35";
    String MIGRATE_VOLUME_BACKING_FILE_CHECK_MD5_STAGE = "35-40";
    String MIGRATE_VOLUME_AFTER_BACKING_FILE_GET_MD5_STAGE = "40-45";
    String MIGRATE_VOLUME_AFTER_BACKING_FILE_COPY_STAGE = "45-95";
    String MIGRATE_VOLUME_AFTER_BACKING_FILE_CHECK_MD5_STAGE = "95-100";

    String MIGRATE_VOLUME_GET_MD5_STAGE = "0-10";
    String MIGRATE_VOLUME_COPY_STAGE = "10-90";
    String MIGRATE_VOLUME_CHECK_MD5_STAGE = "90-100";
    String EXTERNAL_PRIMARY_STORAGE_TYPE = "Addon";

    enum AllocatorParams {
        SPEC,
        CANDIDATES,
    }

    interface StatusConfig {
        List<PrimaryStorageStatus> AVAILABLE_STATUSES = Arrays.asList(PrimaryStorageStatus.Connected);
    }

    interface StateConfig {
        List<PrimaryStorageState> AVAILABLE_STATES = Arrays.asList(PrimaryStorageState.Enabled);
    }
}
