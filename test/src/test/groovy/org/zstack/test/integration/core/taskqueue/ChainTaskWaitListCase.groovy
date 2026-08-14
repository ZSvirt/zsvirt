package org.zstack.test.integration.core.taskqueue

import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
/**
 * Created by mingjian.deng on 2019/10/15.*/
class ChainTaskWaitListCase extends SubCase {
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
        testDefaultTasks()
        testMaxTasks()
        testTwoDedupTasks()
    }

    class LatchTask {
        String name
        def task = false
        def abandon = false
        def latch = new CountDownLatch(1)

        void execute() {
            task = true
            logger.debug("start execute task: ${name}")
            sleep 80
            logger.debug("finish execute task: ${name}")
        }

        void lock() {
            task = true
            latch.await()
        }

        void release() {
            latch.countDown()
        }

        void throwed() {
            abandon = true
        }

        LatchTask(name) {
            this.name = name
        }
    }

    // test default running is not changed
    void testDefaultTasks() {
        logger.debug("start test default tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)
//        def latches = [new LatchTask("task1"), new LatchTask("task2"), new LatchTask("task3")]
        def latches = []
        latches.add(new LatchTask("task1"))
        latches.add(new LatchTask("task2"))
        latches.add(new LatchTask("task3"))

        latches.each {
            def lat = it
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    lat.lock()
                    chain.next()
                }

                @Override
                String getName() {
                    return lat.name
                }
            })
            sleep 4
        }

        sleep 500

        latches.each {
            if (it.name == "task1") {
                assert it.task
            } else {
                assert !it.task
            }
        }

        latches.each {
            it.release()
        }
        sleep 100
    }

    // test max waiting list
    void testMaxTasks() {
        logger.debug("start test max tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)
        def latches = []
        latches.add(new LatchTask("task1"))
        latches.add(new LatchTask("task2"))
        latches.add(new LatchTask("task3"))
        latches.add(new LatchTask("task4"))
        /**
         * use add instead of the bellow code, cause some failed cases indicate there maybe some bugs
         */
        //def latches = [new LatchTask("task1"), new LatchTask("task2"), new LatchTask("task3"), new LatchTask("task4")]
        boolean hangLatch = true
        latches.each {
            long start = System.currentTimeMillis();
            def lat = it
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    while (hangLatch) {
                        sleep(50)
                    }

                    lat.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return lat.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 2
                }

                @Override
                void exceedMaxPendingCallback() {
                    lat.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "max tasks"
                }
            })
            sleep 4
            long end = System.currentTimeMillis();
            logger.info(String.format("thdf.chainSubmit cost %s", end - start))
        }

        retryInSecs {
            latches.each {
                if (it.name == "task4") {
                    assert !it.task
                    assert it.abandon
                }
            }
        }

        hangLatch = false
        sleep 500

        latches.each {
            if (it.name == "task4") {
                assert !it.task
                assert it.abandon
            } else {
                assert it.task
                assert !it.abandon
            }
        }

        // test chain.next still works
        def task = new LatchTask("task5")
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() {
                return "task"
            }

            @Override
            void run(SyncTaskChain chain) {
                task.execute()
                chain.next()
            }

            @Override
            String getName() {
                return task.name
            }

            @Override
            int getMaxPendingTasks() {
                return 2
            }

            @Override
            void exceedMaxPendingCallback() {
                task.throwed()
            }

            @Override
            String getDeduplicateString() {
                return "max tasks"
            }
        })
        sleep 500
        assert task.task
        assert !task.abandon
    }

    void testTwoDedupTasks() {
        logger.debug("start test two dedup tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)

        def latches1 = []
        latches1.add(new LatchTask("task1-1"))
        latches1.add(new LatchTask("task1-2"))
        latches1.add(new LatchTask("task1-3"))

        def latches2 = []
        latches2.add(new LatchTask("task2-1"))
        latches2.add(new LatchTask("task2-2"))
        latches2.add(new LatchTask("task2-3"))
//        def latches1 = [new LatchTask("task1-1"), new LatchTask("task1-2"), new LatchTask("task1-3")]
//        def latches2 = [new LatchTask("task2-1"), new LatchTask("task2-2"), new LatchTask("task2-3")]

        def latchesTaskHang = true
        latches1.each {
            def lat = it
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    while (latchesTaskHang) {
                        sleep(50)
                    }

                    lat.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return lat.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 0
                }

                @Override
                void exceedMaxPendingCallback() {
                    lat.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "latches1 tasks"
                }
            })
            sleep 4
        }

        latches2.each {
            def lat = it
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    lat.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return lat.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 2
                }

                @Override
                void exceedMaxPendingCallback() {
                    lat.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "latches2 tasks"
                }
            })
            sleep 4
        }

        retryInSecs {
            latches2.each {
                if (it.name == "task2-3") {
                    assert !it.task
                    assert it.abandon
                }
            }
        }

        latchesTaskHang = false
/**
 * now
 * running:   task1-1
 * pending:   task2-1, task2-2
 * throwed:   task1-2, task1-3, task2-3
 */
        sleep 500

        latches1.each {
            if (it.name == "task1-1") {
                assert it.task
                assert !it.abandon
            } else {
                assert !it.task
                assert it.abandon
            }
        }

        latches2.each {
            if (it.name == "task2-3") {
                assert !it.task
                assert it.abandon
            } else {
                assert it.task
                assert !it.abandon
            }
        }
    }
}
