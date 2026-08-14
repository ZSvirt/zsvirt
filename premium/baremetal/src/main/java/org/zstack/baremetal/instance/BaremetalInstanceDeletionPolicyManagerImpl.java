package org.zstack.baremetal.instance;

import org.zstack.header.baremetal.instance.BaremetalInstanceDeletionPolicyManager;

/**
 * Created by GuoYi on 7/8/18.
 */
public class BaremetalInstanceDeletionPolicyManagerImpl implements BaremetalInstanceDeletionPolicyManager {
    @Override
    public BaremetalInstanceDeletionPolicy getDeletionPolicy(String vmUuid) {
        return BaremetalInstanceDeletionPolicy.valueOf(BaremetalInstanceGlobalConfig.BM_DELETION_POLICY.value());
    }
}
