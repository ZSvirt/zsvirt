package org.zstack.header.core.workflow;

import org.zstack.header.errorcode.ErrorCode;

public interface FlowContextHandler {
    String getJobUuid();
    void saveContext(Flow toRun);
    boolean cancelled();
    ErrorCode getCancelError();
    boolean skipRollback(ErrorCode errorCode);
    default boolean skipFlow(Flow toRun) {
        return false;
    }
}
