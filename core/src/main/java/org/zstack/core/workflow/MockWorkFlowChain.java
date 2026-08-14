package org.zstack.core.workflow;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;

public class MockWorkFlowChain extends WorkFlowChain {
    private int exitPosition = -1;
    private int rollbackExitPosition = -1;

    public MockWorkFlowChain(String canonicalName) {
        super(canonicalName);
    }

    protected ErrorCode processFlow(WorkFlow flow, WorkFlowVO vo, int position) {
        if (exitPosition == position) {
            super.processFlow(flow, vo, position);
            throw new CloudRuntimeException("Exit on purpose");
        } else {
            return super.processFlow(flow, vo, position);
        }
    }

    protected void rollbackFlow(WorkFlowVO vo) {
        super.rollbackFlow(vo);
        if (vo.getPosition() == rollbackExitPosition) {
            throw new CloudRuntimeException("Exit on purpose");
        }
    }

    public void setExitPositionForProcessDone(int pos) {
        exitPosition = pos;
    }

    public void setRollbackExitPosition(int rollbackExitPosition) {
        this.rollbackExitPosition = rollbackExitPosition;
    }
}
