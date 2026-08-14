package org.zstack.core.workflow;

import org.zstack.header.errorcode.ErrorCode;

public interface WorkFlowCallback {
    void succeed(WorkFlowContext ctx);
    
    void fail(WorkFlowContext ctx, ErrorCode error);
}
