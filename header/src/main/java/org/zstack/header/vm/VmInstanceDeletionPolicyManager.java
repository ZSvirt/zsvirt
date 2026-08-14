package org.zstack.header.vm;

/**
 * Created by frank on 11/12/2015.
 */
public interface VmInstanceDeletionPolicyManager {
    enum VmInstanceDeletionPolicy {
        Direct,
        Delay,
        DBOnly,
        Never,
        KeepVolume
    }

    VmInstanceDeletionPolicy getDeletionPolicy(String vmUuid);
}
