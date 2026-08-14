package org.zstack.test.integration.core.taskqueue

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.debug.DebugSignal
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChainTaskCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    void testChainTaskIncreaseAsyncLevel() {
        ThreadFacade thdf = bean(ThreadFacade.class)
        String signature = Platform.uuid

        boolean task1 = false
        boolean task2 = false

        CountDownLatch latch1 = new CountDownLatch(1)
        CountDownLatch latch2 = new CountDownLatch(1)

        thdf.chainSubmit(new ChainTask(null) {
            // sync level = 1
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                task1 = true
                latch1.await()
            }

            @Override
            String getName() {
                return "task 1"
            }
        })

        thdf.chainSubmit(new ChainTask(null) {
            // sync level = 2, this will increase queue's async level to 2
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                task2 = true
                latch2.await()
            }

            @Override
            String getName() {
                return "task 2"
            }

            @Override
            protected int getSyncLevel() {
                return 2
            }
        })

        retryInSecs {
            assert task1
            assert task2
        }

        latch1.countDown()
        latch2.countDown()
    }

    void testDumpQueueSignal() {
        def inv = loginAsAdmin()
        debugSignal {
            signals = [DebugSignal.DumpTaskQueue.toString()]
            sessionId = inv.uuid
        }
    }

    void testDumpRestStats() {
        def inv = loginAsAdmin()
        def orig = CoreGlobalProperty.PROFILER_HTTP_CALL

        debugSignal {
            signals = ["DumpRestStats"]
            sessionId = inv.uuid
        }

        CoreGlobalProperty.PROFILER_HTTP_CALL = !orig
        debugSignal {
            signals = ["DumpRestStats"]
            sessionId = inv.uuid
        }

        CoreGlobalProperty.PROFILER_HTTP_CALL = orig
    }

    void testChainTaskAsyncLevel() {
        ThreadFacade thdf = bean(ThreadFacade.class)
        String signature = Platform.uuid

        boolean task1 = false
        boolean task2 = false

        CountDownLatch latch1 = new CountDownLatch(1)
        CountDownLatch latch2 = new CountDownLatch(1)

        thdf.chainSubmit(new ChainTask(null) {
            // sync level = 1
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                task1 = true
                latch1.await()
            }

            @Override
            String getName() {
                return "task 1"
            }
        })

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                // this task won't get executed
                task2 = true
                latch2.await()
            }

            @Override
            String getName() {
                return "task 2"
            }
        })

        TimeUnit.SECONDS.sleep(2L)

        assert task1
        assert !task2

        latch1.countDown()
        latch2.countDown()
    }

    @Override
    void test() {
        testChainTaskAsyncLevel()
        testChainTaskIncreaseAsyncLevel()
        testDumpQueueSignal()
        testDumpRestStats()
    }
}
