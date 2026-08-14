package org.zstack.test.integration.core.async


import org.zstack.core.thread.SingleFlightTask
import org.zstack.core.thread.SingleFlightTaskResult
import org.zstack.core.thread.ThreadFacade
import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.testlib.SubCase

import java.util.concurrent.ConcurrentLinkedQueue

class BatchSubmitSingleFlightTaskCase extends SubCase {

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
        testBatchSubmitSingleFlightTasks()
    }

    void testBatchSubmitSingleFlightTasks() {
        List<String> objectForTest = ["test1", "test2"]

        ConcurrentLinkedQueue results = new ConcurrentLinkedQueue<>()
        ThreadFacade threadFacade = bean(ThreadFacadeImpl.class)
        int num = 100

        def threads = []
        boolean hangOnTask = true
        for (int i = 0; i < num; i++) {
            def thread = Thread.start {
                threadFacade.singleFlightSubmit(new SingleFlightTask(null)
                        .setSyncSignature("test")
                        .run({ completion ->
                            completion.success(objectForTest)
                        }).done({ done ->
                    results.add(done)
                }))
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs {
            assert results.size() == num
            results.each { SingleFlightTaskResult result ->
                assert result.getResult() == objectForTest}
        }

        for (int i = 0; i < num; i++) {
            def thread = Thread.start {
                threadFacade.singleFlightSubmit(new SingleFlightTask(null)
                        .setSyncSignature("test")
                        .run({ completion ->
                            completion.success(objectForTest)
                        }).done({ done ->
                    results.add(done)
                }))
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs {
            assert results.size() == num * 2
            results.each { SingleFlightTaskResult result ->
                assert result.getResult() == objectForTest}
        }
    }
}
