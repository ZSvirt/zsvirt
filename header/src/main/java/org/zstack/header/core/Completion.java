package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;

public abstract class Completion extends AbstractCompletion {
    public Completion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }
    public abstract void success();

    public abstract void fail(ErrorCode errorCode);

    public final void next(ErrorCode error) {
        if (error == null) {
            success();
        } else {
            fail(error);
        }
    }

    public final void next(ErrorCodeList errorCodeList) {
        next(errorCodeList.wrapOrNull());
    }
}
