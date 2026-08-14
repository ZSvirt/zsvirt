package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

public abstract class Completion extends AbstractCompletion {
    public Completion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }
    public abstract void success();

    public abstract void fail(ErrorCode errorCode);
}
