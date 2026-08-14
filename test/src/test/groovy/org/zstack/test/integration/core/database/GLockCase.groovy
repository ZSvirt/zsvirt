package org.zstack.test.integration.core.database


import org.zstack.core.db.GLock
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/12/28.*/
class GLockCase extends SubCase {
    EnvSpec env
    String lockName = "TestDBLock.lock"
    def lock1
    def lock2

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            prepare()
            testGLockTimeout()
            testGlock()
        }

    }

    void prepare() {
    }

    void testGLockTimeout() {
        // test 3 times
        lock1 = new GLock(lockName, 10)
        lock1.lock()
        sleep 100
        try {
            lock1.unlock()
        } catch (CloudRuntimeException e) {
            assert false
        }

        lock1.lock(1)
        sleep 1500
        // glock has been released by wait_timeout, see ZSTAC-25214
        try {
            lock1.unlock()
            assert false
        } catch (CloudRuntimeException e) {
            logger.info(e.message)
        }

        lock1.lock()
        sleep 1500
        try {
            lock1.unlock()
        } catch (CloudRuntimeException e) {
            assert false
        }
    }

    void testGlock() {
        lock1 = new GLock(lockName, 1)
        lock2 = new GLock(lockName, 1)

        long t1 = 0
        long t2 = 0

        Thread.start {
            lock1.lock()
            sleep 100
            lock1.unlock()
            t1 = System.currentTimeMillis()
        }.join()

        Thread.start {
            lock2.lock()
            t2 = System.currentTimeMillis()
            sleep 100
            lock2.unlock()
        }.join()
        logger.debug("locker.debug: t1=${t1}, t2=${t2}")
        assert t1 <= t2
    }
}
