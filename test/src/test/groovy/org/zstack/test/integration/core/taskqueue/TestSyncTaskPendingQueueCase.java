package org.zstack.test.integration.core.taskqueue;

import groovy.lang.Closure;
import org.apache.logging.log4j.ThreadContext;
import org.zstack.core.Platform;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.testlib.SubCase;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class TestSyncTaskPendingQueueCase extends SubCase {
    ThreadFacadeImpl thdf;

    @Override
    public void clean() {
    }

    @Override
    public void setup() {
    }

    @Override
    public void environment() {
        thdf = bean(ThreadFacadeImpl.class);
    }

    void testChainTaskPending() {
        String signature = Platform.getUuid();

        AtomicBoolean firstTaskHung = new AtomicBoolean(false);
        final AtomicBoolean hangFirstTask = new AtomicBoolean(true);

        String firstTaskName = "task 1";
        String pendingTaskName = "task pending";
        submitTask(signature, firstTaskName, () -> {
            while (hangFirstTask.get()) {
                firstTaskHung.set(true);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        retryInSecs(new Closure(null) {
            public Object doCall() {
                assert firstTaskHung.get();
                return null;
            }
        });

        Closure empty = new Closure(null) {
            public Object doCall() {
                return null;
            }
        };
        int pendingTaskSize = 60;
        for (int i = 0; i < pendingTaskSize; i++) {
            submitTask(signature, pendingTaskName, empty);
        }

        assert thdf.getSyncTaskStatistics().get(signature).getCurrentRunningThreadNum() == 1;
        assert thdf.getSyncTaskStatistics().get(signature).getPendingTaskNum() == pendingTaskSize;

        // confirm all task finished
        hangFirstTask.set(false);
        retryInSecs(new Closure(null) {
            public Object doCall() {
                assert thdf.getSyncTaskStatistics().get(signature) == null;
                return null;
            }
        });

        // submit hang task again and test abnormalTaskPendingThreshold is reset
        hangFirstTask.set(true);
        submitTask(signature, firstTaskName, () -> {
            while (hangFirstTask.get()) {
                firstTaskHung.set(true);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        retryInSecs(new Closure(null) {
            public Object doCall() {
                assert thdf.getSyncTaskStatistics().get(signature).getCurrentRunningThreadNum() == 1;
                assert thdf.getSyncTaskStatistics().get(signature).getPendingTaskNum() == 0;
                return null;
            }
        });

        // confirm all task finished
        hangFirstTask.set(false);
        retryInSecs(new Closure(null) {
            public Object doCall() {
                assert thdf.getSyncTaskStatistics().get(signature) == null;
                return null;
            }
        });
    }

    private void submitTask(String syncSignature, String taskName, Runnable taskExecution) {
        TestSyncTask task = new TestSyncTask(syncSignature, taskName, 1, taskExecution);
        thdf.syncSubmit(task);
    }

    @Override
    public void test() {
        testChainTaskPending();
    }
}
