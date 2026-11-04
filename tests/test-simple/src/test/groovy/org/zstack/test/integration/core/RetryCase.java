package org.zstack.test.integration.core;

import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.testlib.SubCase;

import java.io.IOException;

public class RetryCase extends SubCase {
    private int num = 0;

    @Override
    public void clean() {

    }

    @Override
    public void setup() {

    }

    @Override
    public void environment() {
    }

    void testRetry1() {
        num = 3;

        new Retry() {
            @Override
            @RetryCondition(onExceptions = {RuntimeException.class})
            protected Object call() {
                num --;

                if (num > 0) {
                    throw new RuntimeException("on purpose");
                }

                return null;
            }
        }.run();

        assert num == 0 : num;
    }

    void testRetry2() {
        boolean success = false;
        num = 3;

        // not retried because we specify only retry on IOException
        try {
            new Retry() {
                @Override
                @RetryCondition(times = 1, onExceptions = {IOException.class})
                protected Object call() {
                    if (num > 0) {
                        throw new RuntimeException("on purpose");
                    }

                    return null;
                }
            }.run();
        } catch (RuntimeException e) {
            success = true;
        }

        assert success;
        assert num == 3 : num;
    }

    void testRetry3() {
        new Retry() {
            @Override
            @RetryCondition(times = 1, onExceptions = {RuntimeException.class})
            protected Object call() {
                throw new RuntimeException();
            }

            protected boolean onFailure(Throwable t) {
                return false;
            }
        }.run();

        // even retry fails no exception will be thrown because onFailure returns false
        assert true;
    }

    @Override
    public void test() {
        testRetry1();
        testRetry2();
        testRetry3();
    }
}
