package org.zstack.test.integration.core.taskqueue

import org.zstack.core.Platform
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.host.APIReconnectHostMsg
import org.zstack.testlib.SubCase

class ChainTaskTimeoutCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testTimeOutTask()
    }

    void testTimeOutTask() {
        ThreadFacade thdf = bean(ThreadFacade.class)
        String signature = Platform.uuid

        boolean task1 = false
        boolean task2 = false

        def msg = new APIReconnectHostMsg()
        msg.setTimeout(2)

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                sleep(2000)
                task1 = true
                chain.next()
            }

            @Override
            String getName() {
                return "task1"
            }
        })

        // task 2 will pending 2 secs, longer than timeout(2ms), not execute
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            String getSyncSignature() {
                return signature
            }

            @Override
            void run(SyncTaskChain chain) {
                task2 = true
                chain.next()
            }

            @Override
            String getName() {
                return "task2"
            }
        })

        sleep(3000)
        assert task1
        assert !task2
    }
}
