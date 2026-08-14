package org.zstack.storage.migration.primary.ceph;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.storage.migration.AbstractStorageMigrationChainFactory;
import org.zstack.storage.migration.StorageMigrationChainType;
import org.zstack.storage.migration.StorageMigrationConstant;

/**
 * Created by GuoYi on 8/31/17.
 */
public class CephToCephMigrateVolumeChainFactory extends AbstractStorageMigrationChainFactory {
    private static final StorageMigrationChainType type =
            new StorageMigrationChainType(StorageMigrationConstant.CEPH_TO_CEPH_MIGRATE_VOLUME_TYPE);

    @Override
    public FlowChain getStorageMigrationFlowChain() {
        return builder.build();
    }

    @Override
    public FlowChain getStorageMigrationRollbackFlowChain() {
        return rollbackBuilder.build();
    }

    @Override
    public StorageMigrationChainType getStorageMigrationChainType() {
        return type;
    }
}
