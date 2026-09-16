package org.zstack.test.integration.core

import org.zstack.core.thread.AsyncTimer
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class AsyncTimerCase extends SubCase {
    @Override
    void clean() {

    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    void testRun() {
        int count = 0
        int total = 3

        def timer = new AsyncTimer(TimeUnit.MILLISECONDS, 100L) {
            @Override
            protected void execute() {
                count ++
                if (count < total) {
                    continueToRunThisTimer()
                }
            }
        }

        timer.start()

        TimeUnit.MILLISECONDS.sleep(total * 100)
        retryInSecs {
            assert total == count
        }

        TimeUnit.MILLISECONDS.sleep(2L * 100)

        assert total == count
    }

    void testStartRightNow() {
        int count = 0
        int total = 3

        def timer = new AsyncTimer(TimeUnit.MILLISECONDS, 100L) {
            @Override
            protected void execute() {
                count ++
                if (count < total) {
                    continueToRunThisTimer()
                }
            }
        }

        timer.startRightNow()

        TimeUnit.MILLISECONDS.sleep((total - 1) * 100)
        retryInSecs {
            assert total == count
        }

        TimeUnit.MILLISECONDS.sleep(2L * 100)
        assert total == count
    }

    void testCancel() {
        int count = 0

        def timer = new AsyncTimer(TimeUnit.MILLISECONDS, 1L * 100) {
            @Override
            protected void execute() {
                count ++
                continueToRunThisTimer()
            }
        }

        timer.start()
        TimeUnit.MILLISECONDS.sleep(2L * 100)
        timer.cancel()
        TimeUnit.MILLISECONDS.sleep(2L * 100)
        int now = count
        TimeUnit.MILLISECONDS.sleep(2L * 100)
        assert now == count
    }

    @Override
    void test() {
        testRun()
        testStartRightNow()
        testCancel()
    }
}
