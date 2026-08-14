package org.zstack.test.integration.core.taskqueue

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.thread.SingleFlightTask
import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.testlib.SubCase

class SingleFlightPendingQueueCase extends SubCase {
    ThreadFacadeImpl thdf

    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        thdf = bean(ThreadFacadeImpl.class)
    }

    void testChainTaskPending() {
        String signature = Platform.uuid

        boolean firstTaskHung = false
        boolean hangFirstTask = true

        String firstTaskName = "task 1"
        String pendingTaskName = "task pending"
        submitTask(signature, firstTaskName, {
            while (hangFirstTask) {
                firstTaskHung = true
                sleep(100)
            }
        })

        retryInSecs {
            assert firstTaskHung
        }

        int pendingTaskSize = 60
        for (int i = 0; i < pendingTaskSize; i++) {
            submitTask(signature, pendingTaskName, {})
        }

        assert thdf.getSingleFlightChainTaskInfo(signature).getPendingTask().size() == pendingTaskSize
        assert thdf.dpq.singleFlightTasks.get(signature).getCurrentPendingQueueThreshold() == CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD * 5

        // confirm all task finished
        hangFirstTask = false
        retryInSecs {
            assert thdf.getSingleFlightChainTaskInfo(signature) == null
        }

        // submit hang task again and test abnormalTaskPendingThreshold is reset
        hangFirstTask = true
        submitTask(signature, firstTaskName, {
            while (hangFirstTask) {
                firstTaskHung = true
                sleep(100)
            }
        })

        retryInSecs {
            assert thdf.getSingleFlightChainTaskInfo(signature).getRunningTask().size() == 1
            assert thdf.getSingleFlightChainTaskInfo(signature).getPendingTask().size() == 0
        }

        // check pending queue threshold is reset
        assert thdf.dpq.singleFlightTasks.get(signature).getCurrentPendingQueueThreshold() == CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD

        // confirm all task finished
        hangFirstTask = false
        retryInSecs {
            assert thdf.getSingleFlightChainTaskInfo(signature) == null
        }
    }

    private void submitTask(String syncSignature, String taskName, Closure taskExecution) {
        thdf.singleFlightSubmit(new SingleFlightTask(null)
                .setSyncSignature(syncSignature)
                .run({ completion ->
                    taskExecution()
                    completion.success(null)
                }).done({ done ->
            // do nothing
        }))
    }

    @Override
    void test() {
        testChainTaskPending()
    }
}