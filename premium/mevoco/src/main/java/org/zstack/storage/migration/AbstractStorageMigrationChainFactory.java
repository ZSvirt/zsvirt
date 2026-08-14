package org.zstack.storage.migration;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.FlowChain;

import java.util.List;

/**
 * Created by GuoYi on 9/4/17.
 */
public abstract class AbstractStorageMigrationChainFactory implements StorageMigrationChainFactory, Component {
    protected FlowChainBuilder builder;
    protected FlowChainBuilder rollbackBuilder;
    protected FlowChainBuilder discardBuilder;
    protected FlowChainBuilder canceldBuilder;

    private List<String> flowClassNames;
    private List<String> rollbackFlowClassNames;
    private List<String> discardClassNames;
    private List<String> cancelFlowClassNames;

    public List<String> getRollbackFlowClassNames() {
        return rollbackFlowClassNames;
    }

    public void setRollbackFlowClassNames(List<String> rollbackFlowClassNames) {
        this.rollbackFlowClassNames = rollbackFlowClassNames;
    }

    public List<String> getFlowClassNames() {
        return flowClassNames;
    }

    public void setFlowClassNames(List<String> flowClassNames) {
        this.flowClassNames = flowClassNames;
    }

    public List<String> getDiscardClassNames() {
        return discardClassNames;
    }

    public void setDiscardClassNames(List<String> discardClassNames) {
        this.discardClassNames = discardClassNames;
    }

    public List<String> getCancelFlowClassNames() {
        return cancelFlowClassNames;
    }

    public void setCancelFlowClassNames(List<String> cancelFlowClassNames) {
        this.cancelFlowClassNames = cancelFlowClassNames;
    }

    @Override
    public abstract StorageMigrationChainType getStorageMigrationChainType();

    @Override
    public FlowChain getStorageMigrationRollbackFlowChain() {
        return rollbackBuilder.build();
    }

    @Override
    public FlowChain getStorageMigrationDiscardFlowChain() {
        return discardBuilder.build();
    }

    @Override
    public FlowChain getCancelStorageMigrationFlowChain() {
        return canceldBuilder.getFlows() == null ? null : canceldBuilder.build();
    }

    @Override
    public boolean start() {
        builder = FlowChainBuilder.newBuilder().setFlowClassNames(flowClassNames).construct();
        rollbackBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(rollbackFlowClassNames).construct();
        discardBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(discardClassNames).construct();
        canceldBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(cancelFlowClassNames).construct();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
