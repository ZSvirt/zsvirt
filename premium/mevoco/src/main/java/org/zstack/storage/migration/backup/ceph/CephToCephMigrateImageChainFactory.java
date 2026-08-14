package org.zstack.storage.migration.backup.ceph;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.storage.migration.AbstractStorageMigrationChainFactory;
import org.zstack.storage.migration.StorageMigrationChainType;
import org.zstack.storage.migration.StorageMigrationConstant;

/**
 * Created by GuoYi on 8/31/17.
 */
public class CephToCephMigrateImageChainFactory extends AbstractStorageMigrationChainFactory {
    private static final StorageMigrationChainType type =
            new StorageMigrationChainType(StorageMigrationConstant.CEPH_TO_CEPH_MIGRATE_IMAGE_TYPE);

    @Override
    public FlowChain getStorageMigrationFlowChain() {
        return builder.build();
    }

    @Override
    public StorageMigrationChainType getStorageMigrationChainType() {
        return type;
    }
}
