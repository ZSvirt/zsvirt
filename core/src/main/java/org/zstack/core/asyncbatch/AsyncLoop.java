package org.zstack.core.asyncbatch;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by xing5 on 2016/9/11.
 */
public abstract class AsyncLoop<T>  {
    private AsyncBackup[] backups;
    protected List<ErrorCode> errors = new ArrayList<>();

    public AsyncLoop(AsyncBackup... backups) {
        this.backups = backups;
    }

    public AsyncLoop() {
        backups = new AsyncBackup[]{};
    }

    protected boolean continueOnError() {
        return false;
    }

    protected abstract Collection<T> collectionForLoop();

    protected abstract void run(T item, Completion completion);

    protected abstract void done();

    protected abstract void error(ErrorCode errorCode);

    public void start() {
        Collection<T> items = collectionForLoop();
        DebugUtils.Assert(items != null, "collectionForLoop cannot return null");

        runItem(items.iterator(), new Completion(null, backups) {
            @Override
            public void success() {
                done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                error(errorCode);
            }
        });
    }

    private void runItem(Iterator<T> it, Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        T i = it.next();
        run(i, new Completion(completion) {
            @Override
            public void success() {
                runItem(it, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!continueOnError()) {
                    completion.fail(errorCode);
                } else {
                    errors.add(errorCode);
                    runItem(it, completion);
                }
            }
        });
    }
}
