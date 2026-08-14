package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by MaJin on 2017-05-12.
 */
public abstract class WhileCompletion extends NoErrorCompletion {
    private final AtomicBoolean addErrorCalled = new AtomicBoolean(false);

    public WhileCompletion(AsyncBackup... completion) {
        super(completion);
    }

    /**
     * The allDone method will IMMEDIATELY stop the executing loop and return,
     * without waiting for other handler finished
     * 
     * The executing handler will keep running;
     * 
     * The handler that has not yet been executed will be aborted.
     */
    public abstract void allDone();

    public abstract void addError(ErrorCode error);

    public final AtomicBoolean getAddErrorCalled() {
        return addErrorCalled;
    }
}
