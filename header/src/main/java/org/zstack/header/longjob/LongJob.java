package org.zstack.header.longjob;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APIEvent;

/**
 * Created by GuoYi on 11/24/17.
 */
public interface LongJob {
    void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion);
    default void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {}
    default void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {}
    default void clean(LongJobVO job, NoErrorCompletion completion) {}
    default Class getAuditType() {
        return null;
    }
    default String getAuditResourceUuid() {
        return null;
    }
}
