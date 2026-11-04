package org.zstack.test.integration.kvm.hostallocator


import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicInteger

class ConcurrentVmCreationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            testSyncSubmit()
        }
    }

    void testSyncSubmit() {
        ThreadFacadeImpl thdf = bean(ThreadFacadeImpl.class)
        AtomicInteger counter = new AtomicInteger(10)

        MySyncTask task = new MySyncTask(counter)

        1.upto(10) {
            thdf.syncSubmit(task)
        }

        retryInSecs(8, 1) {
            assert counter.get() == 0
        }
    }
}
