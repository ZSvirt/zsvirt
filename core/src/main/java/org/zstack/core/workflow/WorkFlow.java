package org.zstack.core.workflow;

public interface WorkFlow {
    void process(WorkFlowContext ctx) throws WorkFlowException;
    
    void rollback(WorkFlowContext ctx);
    
    String getName();
}
