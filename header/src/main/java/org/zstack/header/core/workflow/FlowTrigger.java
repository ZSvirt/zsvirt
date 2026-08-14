package org.zstack.header.core.workflow;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface FlowTrigger extends AsyncBackup {
    void fail(ErrorCode errorCode);

    void next();

    void setError(ErrorCode error);

    default void next(ErrorCode error) {
        if (error == null) {
            next();
        } else {
            fail(error);
        }
    }

    default Completion toCompletion() {
        return new Completion(this) {
            @Override
            public void success() {
                next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                FlowTrigger.this.fail(errorCode);
            }
        };
    }
}
