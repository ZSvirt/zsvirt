package org.zstack.core.progress;

import org.zstack.header.core.workflow.FlowChainMutable;
import org.zstack.header.core.workflow.FlowChainProcessor;

public class ProgressFlowChainProcessor implements FlowChainProcessor {
    TaskProgressReporter reporter;

    @Override
    public void beforeChainStart(FlowChainMutable chain) {
        reporter = ActionProgressService.taskProgress()
                .withContent(progressContent(chain, 0))
                .withCurrentStep(0L)
                .withTotalStep(Math.max(chain.getFlows().size(), 1L))
                .report();
    }

    @Override
    public void afterOneFlowDone(FlowChainMutable chain, int currentFlowIndex) {
        reporter.withContent(progressContent(chain, currentFlowIndex))
                .withCurrentStep(currentFlowIndex)
                .report();
    }

    @Override
    public void beforeChainRollback(FlowChainMutable chain, int currentFlowIndex) {
        reporter.withContent(progressRollbackContent(chain, currentFlowIndex))
                .withCurrentStep(currentFlowIndex)
                .report();
    }

    private String progressContent(FlowChainMutable chain, int currentFlowIndex) {
        if (currentFlowIndex >= chain.getFlows().size()) {
            return String.format("%s: done", chain.getChainName());
        }

        return String.format("%s: %s", chain.getChainName(), chain.getFlows().get(currentFlowIndex).name());
    }

    private String progressRollbackContent(FlowChainMutable chain, int currentFlowIndex) {
        return String.format("%s: rollback", chain.getChainName());
    }
}
