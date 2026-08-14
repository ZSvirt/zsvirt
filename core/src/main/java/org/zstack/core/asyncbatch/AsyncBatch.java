package org.zstack.core.asyncbatch;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xing5 on 2016/6/26.
 */
public abstract class AsyncBatch extends AbstractCompletion {
    private static final CLogger logger = Utils.getLogger(AsyncBatch.class);

    private List<AsyncBatchRunner> runners = new ArrayList<>();
    protected List<ErrorCode> errors = Collections.synchronizedList(new ArrayList<ErrorCode>());

    public AsyncBatch(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    protected void batch(AsyncBatchRunner runner) {
        DebugUtils.Assert(runner != null, "runner cannot be null");
        runners.add(runner);
    }

    protected abstract void setup();

    protected abstract void done();

    public void start() {
        setup();

        if (runners.isEmpty()) {
            done();
            return;
        }

        AsyncBatch that = this;
        AsyncLatch latch = new AsyncLatch(runners.size(), new NoErrorCompletion(backups == null ? new AsyncBackup[]{} : backups.toArray(new AsyncBackup[backups.size()])) {
            @Override
            public void done() {
                that.done();
            }
        });

        for (AsyncBatchRunner runner : runners) {
            try {
                runner.run(new NoErrorCompletion(latch) {
                    @Override
                    public void done() {
                        latch.ack();
                    }
                });
            } catch (Throwable t) {
                logger.warn("unhandled exception happened", t);
                ErrorCode code = new ErrorCode();
                code.setCode(SysErrors.INTERNAL.toString());
                code.setDescription("an internal error happened");
                code.setDetails(t.getMessage());
                errors.add(code);

                latch.ack();
            }
        }
    }
}
