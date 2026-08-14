package org.zstack.core.workflow;

import org.zstack.header.exception.CloudRuntimeException;

public class MockAsyncWorkFlowChain extends AsyncWorkFlowChain {
    private int exitPosition = -1;
    private int rollbackExitPosition = -1;
    
    public MockAsyncWorkFlowChain(String canonicalName) {
        super(canonicalName);
    }
    
    public void setExitPositionForProcessDone(int pos) {
        exitPosition = pos;
    }

    public void setRollbackExitPosition(int rollbackExitPosition) {
        this.rollbackExitPosition = rollbackExitPosition;
    }
    
    @Override
    protected void processFlow(AsyncWorkFlow flow, WorkFlowContext ctx, WorkFlowVO vo, int position) {
        if (position == exitPosition) {
            throw new CloudRuntimeException("Stop processing flow on purpose");
        } else {
            super.processFlow(flow, ctx, vo, position);
        }
    }
    
    @Override
    protected void rollbackFlow(WorkFlowVO vo) {
        super.rollbackFlow(vo);
        if (vo.getPosition() == rollbackExitPosition) {
            throw new CloudRuntimeException("Stop rolling back flow on purpose");
        }
    }
}
