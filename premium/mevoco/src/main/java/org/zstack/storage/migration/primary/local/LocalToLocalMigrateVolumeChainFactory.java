package org.zstack.storage.migration.primary.local;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.storage.migration.AbstractStorageMigrationChainFactory;
import org.zstack.storage.migration.StorageMigrationChainType;
import org.zstack.storage.migration.StorageMigrationConstant;

public class LocalToLocalMigrateVolumeChainFactory extends AbstractStorageMigrationChainFactory {
    private static final StorageMigrationChainType type =
            new StorageMigrationChainType(StorageMigrationConstant.LOCAL_TO_LOCAL_MIGRATE_VOLUME_TYPE);

    @Override
    public FlowChain getStorageMigrationFlowChain() {
        return builder.build();
    }

    @Override
    public FlowChain getStorageMigrationRollbackFlowChain() {
        return super.getStorageMigrationRollbackFlowChain();
    }

    @Override
    public StorageMigrationChainType getStorageMigrationChainType() {
        return type;
    }
}
