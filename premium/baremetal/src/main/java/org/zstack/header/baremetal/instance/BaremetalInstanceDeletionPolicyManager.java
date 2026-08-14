package org.zstack.header.baremetal.instance;

/**
 * Created by GuoYi on 7/8/18.
 */
public interface BaremetalInstanceDeletionPolicyManager {
    enum BaremetalInstanceDeletionPolicy {
        Direct,
        Delay
    }

    BaremetalInstanceDeletionPolicy getDeletionPolicy(String vmUuid);
}
