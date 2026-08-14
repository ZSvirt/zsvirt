package org.zstack.header.core.progress;

import org.zstack.header.core.workflow.FlowChainProcessor;

public interface ProgressFlowChainProcessorFactory {
    FlowChainProcessor create();
}
