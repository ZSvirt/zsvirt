package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.BackupStorageAllocatorStrategy;
import org.zstack.header.storage.backup.BackupStorageAllocatorStrategyFactory;
import org.zstack.header.storage.backup.BackupStorageAllocatorStrategyType;
import org.zstack.header.storage.backup.BackupStorageConstant;

import java.util.List;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DefaultBackupStorageAllocatorStrategyFactory implements BackupStorageAllocatorStrategyFactory, Component {
    private List<String> allocatorFlowNames;
    private FlowChainBuilder builder = new FlowChainBuilder();

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    @Override
    public BackupStorageAllocatorStrategyType getType() {
        return BackupStorageConstant.DEFAULT_ALLOCATOR_STRATEGY_TYPE;
    }

    @Override
    public BackupStorageAllocatorStrategy getAllocatorStrategy() {
        return new DefaultBackupStorageAllocatorStrategy(builder.build());
    }

    @Override
    public boolean start() {
        builder.setFlowClassNames(allocatorFlowNames).construct();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
