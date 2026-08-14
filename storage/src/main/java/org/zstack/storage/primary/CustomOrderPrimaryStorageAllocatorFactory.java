package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.*;

import java.util.List;

public class CustomOrderPrimaryStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component,
        PrimaryStorageAllocatorFlowNameSetter, PrimaryStorageAllocatorStrategyExtensionPoint {
    private static final PrimaryStorageAllocatorStrategyType type = new PrimaryStorageAllocatorStrategyType(
            PrimaryStorageConstant.CUSTOM_ORDER_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
    private CustomOrderPrimaryStorageAllocatorStrategy strategy;
    private List<String> allocatorFlowNames;
    private List<String> sortFlowNames;
    private FlowChainBuilder allocateBuilder = new FlowChainBuilder();
    private FlowChainBuilder sortBuilder = new FlowChainBuilder();

    @Override
    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    public List<String> getSortFlowNames() {
        return sortFlowNames;
    }

    public void setSortFlowNames(List<String> sortFlowNames) {
        this.sortFlowNames = sortFlowNames;
    }

    @Override
    public PrimaryStorageAllocatorStrategyType getPrimaryStorageAllocatorStrategyType() {
        return type;
    }

    @Override
    public PrimaryStorageAllocatorStrategy getPrimaryStorageAllocatorStrategy() {
        return strategy;
    }

    @Override
    public boolean start() {
        allocateBuilder.setFlowClassNames(allocatorFlowNames).construct();
        sortBuilder.setFlowClassNames(sortFlowNames).construct();
        strategy = new CustomOrderPrimaryStorageAllocatorStrategy(allocateBuilder, sortBuilder);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getPrimaryStorageAllocatorStrategyName(AllocatePrimaryStorageMsg msg) {
        if (PrimaryStorageConstant.CUSTOM_ORDER_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE.equals(msg.getAllocationStrategy())) {
            return PrimaryStorageConstant.CUSTOM_ORDER_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE;
        }
        return null;
    }

    @Override
    public String getAllocatorStrategy(HostInventory host) {
        if (host != null && !"KVM".equals(host.getHypervisorType())) {
            return null;
        }
        return PrimaryStorageConstant.CUSTOM_ORDER_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE;
    }
}
