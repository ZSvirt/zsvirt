package org.zstack.ha.header;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaConstants {
    String ACTION_CATEGORY = "ha";

    String SERVICE_ID = "ha";

    String HOST_STORAGE_STATE = "hostStorageState";
    String HOST_BUSINESS_NIC = "hostBusinessNic";

    // kvm-agent fencer name
    String KVM_FENCER_HOST_BUSINESS_NIC = HOST_BUSINESS_NIC;
    String KVM_FENCER_SHARED_BLOCK = "shareblockFencer";
    String KVM_FENCER_FILE_SYSTEM = "fileSystemFencer";
    String KVM_FENCER_CEPH = "cephFencer";

    // agent call path
    String ADD_VM_FENCER_RULE_TO_HOST = "/add/vm/fencer/rule/to/host";
    String REMOVE_VM_FENCER_RULE_FROM_HOST = "/remove/vm/fencer/rule/from/host";
    String SETUP_SELF_FENCER_PATH = "/ha/selffencer/setup";
    String CANCEL_SELF_FENCER_PATH = "/ha/selffencer/cancel";
    String SYNC_SELF_FENCER_STATE_PATH = "/ha/selffencer/state";
    String KVM_REPORT_VM_FENCED_EVENT = "/ha/events/vm-fenced";
    String SCAN_HOST_PATH = "/ha/scanhost";
    String SYNC_HA_VM_LIST_PATH = "/ha/sync/vm/list";
}
