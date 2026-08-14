package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public class NopeReturnValueCompletion extends ReturnValueCompletion {
    public NopeReturnValueCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public NopeReturnValueCompletion(AsyncBackup... others) {
        super(null, others);
    }

    @Override
    public void success(Object returnValue) {
    }

    @Override
    public void fail(ErrorCode errorCode) {
    }
}
