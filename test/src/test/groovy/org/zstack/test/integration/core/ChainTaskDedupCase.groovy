package org.zstack.test.integration.core

import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SkipTestSuite
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Deprecated
@SkipTestSuite
class ChainTaskDedupCase extends SubCase {
    ThreadFacade thdf;

    @Override
    void clean() {

    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        thdf = bean(ThreadFacade.class)
    }

    void testDedup() {
        String sig = "dedup"
        SyncTaskChain nextChain = null

        CountDownLatch latch = new CountDownLatch(1)
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() {
                return sig
            }

            @Override
            void run(SyncTaskChain chain) {
                // the task
                nextChain = chain
                latch.countDown()
            }

            @Override
            String getName() {
                return getSyncSignature()
            }
        })

        latch.await(5L, TimeUnit.SECONDS)
        assert thdf.isChainTaskRunning(sig)
        nextChain.next()
    }

    void testNoDedup() {
        String sig = "dedup"

        CountDownLatch latch = new CountDownLatch(1)

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() {
                return sig
            }

            @Override
            void run(SyncTaskChain chain) {
                chain.next()
                // this must be below chain.next()
                latch.countDown()
            }

            @Override
            String getName() {
                return getSyncSignature()
            }
        })

        latch.await(5L, TimeUnit.SECONDS)
        assert !thdf.isChainTaskRunning(sig)
    }

    @Override
    void test() {
        testDedup()
        testNoDedup()
    }
}
