package org.zstack.longjob;

import org.zstack.header.core.workflow.FlowContextHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.longjob.LongJobErrors;

public abstract class LongJobFlowContextHandler implements FlowContextHandler {
    private final String longJobUuid;

    public LongJobFlowContextHandler(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    @Override
    public String getJobUuid() {
        return longJobUuid;
    }

    @Override
    public boolean cancelled() {
        return LongJobUtils.jobCanceled(getJobUuid());
    }

    @Override
    public ErrorCode getCancelError() {
        return LongJobUtils.cancelErr(getJobUuid());
    }

    @Override
    public boolean skipRollback(ErrorCode errorCode) {
        return errorCode.isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR, LongJobErrors.INTERRUPTED);
    }
}
