package org.zstack.storage.primary.sharedblock.migration;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.storage.migration.AbstractStorageMigrationChainFactory;
import org.zstack.storage.migration.StorageMigrationChainType;
import org.zstack.storage.migration.StorageMigrationConstant;

public class SblkToSblkMigrateVolumeChainFactory extends AbstractStorageMigrationChainFactory {
    private static final StorageMigrationChainType type =
            new StorageMigrationChainType(StorageMigrationConstant.SHAREDBLOCK_TO_SHAREDBLOCK_MIGRATE_VOLUME_TYPE);

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
