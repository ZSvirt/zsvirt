package org.zstack.test.integration.core.taskqueue

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.testlib.SubCase

class ChainTaskPendingQueueCase extends SubCase {
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
        def threads = []
        for (int i = 0; i < pendingTaskSize; i++) {
            def thread = Thread.start {
                submitTask(signature, pendingTaskName, {})
            }
            threads << thread
        }

        threads.each { it.join() }

        assert thdf.getChainTaskInfo(signature).getPendingTask().size() == pendingTaskSize
        assert thdf.dpq.chainTasks.get(signature).getCurrentPendingQueueThreshold() == CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD * 5

        // confirm all task finished
        hangFirstTask = false
        retryInSecs {
            assert thdf.getChainTaskInfo(signature).getRunningTask().size() == 0
            assert thdf.getChainTaskInfo(signature).getPendingTask().size() == 0
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
            assert thdf.getChainTaskInfo(signature).getRunningTask().size() == 1
            assert thdf.getChainTaskInfo(signature).getPendingTask().size() == 0
        }

        // check pending queue threshold is reset
        assert thdf.dpq.chainTasks.get(signature).getCurrentPendingQueueThreshold() == CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD

        // confirm all task finished
        hangFirstTask = false
        retryInSecs {
            assert thdf.getChainTaskInfo(signature).getRunningTask().size() == 0
            assert thdf.getChainTaskInfo(signature).getPendingTask().size() == 0
        }
    }

    private void submitTask(String syncSignature, String taskName, Closure taskExecution) {
        thdf.chainSubmit(new ChainTask(null) {
            // sync level = 1
            @Override
            String getSyncSignature() {
                return syncSignature
            }

            @Override
            void run(SyncTaskChain chain) {
                taskExecution()
                chain.next()
            }

            @Override
            String getName() {
                return taskName
            }
        })
    }

    @Override
    void test() {
        testChainTaskPending()
    }
}