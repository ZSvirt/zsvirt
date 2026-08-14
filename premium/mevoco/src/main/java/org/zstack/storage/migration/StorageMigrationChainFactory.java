package org.zstack.storage.migration;

import org.zstack.header.core.workflow.FlowChain;

/**
 * Created by GuoYi on 8/31/17.
 */
public interface StorageMigrationChainFactory {
    StorageMigrationChainType getStorageMigrationChainType();

    FlowChain getStorageMigrationFlowChain();

    FlowChain getCancelStorageMigrationFlowChain();

    FlowChain getStorageMigrationRollbackFlowChain();

    FlowChain getStorageMigrationDiscardFlowChain();
}
