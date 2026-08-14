package org.zstack.core.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.rest.AbstractAsyncResponseFetcher;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;

public class AsyncResponseFetcher<T> extends AbstractAsyncResponseFetcher<T> {
    private static final CLogger logger = Utils.getLogger(AsyncResponseFetcher.class);
    private Future<Void> cancelHandler;

    protected long runUtil;

    @Autowired
    protected ThreadFacade threadFacade;
    @Autowired
    protected TimeHelper timeHelper;

    protected AsyncResponseFetcher(Class<T> returnType) {
        super(returnType);
    }

    @Override
    public void fetch(ReturnValueCompletion<T> completion) {
        runUtil = timeHelper.getCurrentTimeMillis() + allAttemptsTimeoutInMillis;

        ReturnValueCompletion<T> completionOnlyIfFinished = new ReturnValueCompletion<T>(completion) {
            @Override
            public void success(T returnValue) {
                onFinished();
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                onFinished();
                completion.fail(errorCode);
            }
        };

        cancelHandler = threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return eachAttemptIntervalInMillis;
            }

            @Override
            public String getName() {
                return "wait-for-api-response";
            }

            @Override
            public void run() {
                if (cancelHandler == null || cancelHandler.isCancelled()) {
                    return;
                }

                boolean finished = AsyncResponseFetcher.this.run(completionOnlyIfFinished);
                if (finished) {
                    return;
                }

                if (timeHelper.getCurrentTimeMillis() > runUtil) {
                    completionOnlyIfFinished.fail(timeoutError());
                }
            }
        });
    }

    private void onFinished() {
        try {
            cancelHandler.cancel(true);
            cancelHandler = null;
        } catch (Exception e) {
            logger.trace("cancel periodic task failed", e);
        }
    }

    private ErrorCode timeoutError() {
        return err(SysErrors.HTTP_ERROR, "http timeout")
                .withOpaque("timeout.millis", allAttemptsTimeoutInMillis)
                .withOpaque("expect.response.type", returnType.getSimpleName());
    }

    /**
     * invoke completionOnlyIfFinished.success/fail ONLY when task finished !!!
     * @return finished
     */
    protected boolean run(ReturnValueCompletion<T> completionOnlyIfFinished) {
        ErrorableValue<T> value = eachAttempt();

        if (value.isSuccess()) {
            completionOnlyIfFinished.success(value.result);
            return true;
        }

        final ErrorCode error = value.error;
        if (error == NOT_FINISHED_YET) {
            return false;
        }

        completionOnlyIfFinished.fail(error);
        return true;
    }
}
