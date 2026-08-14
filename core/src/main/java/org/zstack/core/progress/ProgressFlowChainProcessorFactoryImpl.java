package org.zstack.core.progress;

import org.zstack.header.core.progress.ProgressFlowChainProcessorFactory;
import org.zstack.header.core.workflow.FlowChainProcessor;

public class ProgressFlowChainProcessorFactoryImpl implements ProgressFlowChainProcessorFactory {
    @Override
    public FlowChainProcessor create() {
        return new ProgressFlowChainProcessor();
    }
}
