package org.zstack.header.core.workflow;

/**
 * Created by xing5 on 2016/4/4.
 */
public interface FlowChainProcessor {
    default void beforeChainStart(FlowChainMutable chain) {}

    default void afterOneFlowDone(FlowChainMutable chain, int currentFlowIndex) {}

    default void beforeChainRollback(FlowChainMutable chain, int currentFlowIndex) {}
}
