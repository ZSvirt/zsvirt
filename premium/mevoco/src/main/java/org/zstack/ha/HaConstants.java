package org.zstack.ha;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaConstants {
    String ACTION_CATEGORY = "ha";

    String SERVICE_ID = "ha";

    ErrorCode PRIMARY_STORAGE_HOST_DISCONNECTED_ERROR = new ErrorCode(HaErrors.PRIMARY_STORAGE_ERROR.toString(), "Primary storage error");

    String HOST_STORAGE_STATE = "hostStorageState";
    String HOST_BUSINESS_NIC = "hostBusinessNic";

    // kvm-agent fencer name
    String KVM_FENCER_HOST_BUSINESS_NIC = HOST_BUSINESS_NIC;
    String KVM_FENCER_SHARED_BLOCK = "shareblockFencer";
    String KVM_FENCER_FILE_SYSTEM = "fileSystemFencer";
    String KVM_FENCER_CEPH = "cephFencer";
}
