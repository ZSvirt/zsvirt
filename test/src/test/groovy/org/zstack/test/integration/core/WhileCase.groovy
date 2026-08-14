package org.zstack.test.integration.core

import org.zstack.core.asyncbatch.While
import org.zstack.core.asyncbatch.WhileGlobalProperty
import org.zstack.core.thread.AsyncThread
import org.zstack.core.thread.ThreadGlobalProperty
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.WhileCompletion
import org.zstack.header.core.WhileDoneCompletion
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.test.TestSafeWhile
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.zstack.core.Platform.operr

/**
 * Created by MaJin on 2017-05-02.
 */
class WhileCase extends SubCase{
    private static final CLogger logger = Utils.getLogger(WhileCase.class)
    static TIME_OUT = TimeUnit.SECONDS.toMillis(15)

    @Override
    void clean() {

    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        testWhileExceptionHandle()
        testStepSmallerThanItems()
        testRunAllWhenItemsEmpty()
        testRunAllCompletionAllDone()
        testRunStepWhenItemsEmpty()
        testRunStepCompletionDone()
        testRunStepComletionAllDone()
        testConcurrentAdd()
        testRunAll()
        testRunAllWithConcurrencyCoefficientEqualZero()
    }

    static void testWhileExceptionHandle() {
        new TestSafeWhile().testSafeWhile()
    }

    static void testStepSmallerThanItems() {
        List<Integer> lst = [1, 2, 3]
        FutureCompletion fc = new FutureCompletion(null)

        int count = 0
        new While<>(lst).step(new While.Do() {
            @Override
            @AsyncThread
            void accept(Object item, WhileCompletion completion) {
                count ++
                completion.done()
            }
        } ,2).run(new WhileDoneCompletion(fc) {
            @Override
            void done(ErrorCodeList errorCodeList) {
                fc.success()
            }
        })

        fc.await(TIME_OUT)
        assert count == 3
    }

    static void testRunAllWhenItemsEmpty(){
        FutureCompletion fc2 = new FutureCompletion(null)

        new While<>(new ArrayList<String>()).all(new While.Do<String>() {
            @Override
            void accept(String item, WhileCompletion completion) {
                completion.done()
            }
        }).run(new WhileDoneCompletion(null){

            @Override
            void done(ErrorCodeList errs) {
                fc2.success()
            }
        })

        fc2.await(TIME_OUT)

        assert fc2.success
    }

    static void testRunAllCompletionAllDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger count = new AtomicInteger(0)

        new While<>(["1", "2"]).all({item, completion ->
            logger.debug(String.format("item %s allDone", item))
            completion.allDone()
        }).run(new WhileDoneCompletion(null){
            @Override
            void done(ErrorCodeList errs) {
                count.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert future.success
        assert count.get() == 1
    }

    static void testRunStepWhenItemsEmpty(){
        FutureCompletion future = new FutureCompletion(null)

        new While<>(new ArrayList<String>()).step({item, completion ->
            completion.done()
        }, 1).run(new WhileDoneCompletion(future){
            @Override
            void done(ErrorCodeList errorCodeList) {
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert future.success
    }

    static void testRunStepCompletionDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger whileDoneCount = new AtomicInteger(0)
        AtomicInteger itemDoneCount = new AtomicInteger(0)

        new While<>([1, 2, 3]).step({item, completion ->
            Thread.start {
                logger.debug("step " + item)
                sleep(item * 200 as long)
                itemDoneCount.addAndGet(1)
                completion.done()
            }
        }, 2).run(new WhileDoneCompletion(future){
            @Override
            void done(ErrorCodeList errorCodeList) {
                whileDoneCount.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert future.success
        assert whileDoneCount.get() == 1
        assert itemDoneCount.get() == 3
    }

    static void testRunStepComletionAllDone(){
        FutureCompletion future = new FutureCompletion(null)
        AtomicInteger count = new AtomicInteger(0)

        new While<>(["1", "2"]).step({item, completion ->
            logger.debug(String.format("step %s allDone", item))
            completion.allDone()
        }, 2).run(new WhileDoneCompletion(future){
            @Override
            void done(ErrorCodeList errorCodeList) {
                count.addAndGet(1)
                logger.debug("While is done")
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert future.success
        assert count.get() == 1
    }

    static void testConcurrentAdd() {
        List<Object> target = new ArrayList<>()
        List<Object> source = new ArrayList<>()
        for (int i = 0; i < 100; i++) {
            source.add(i + "")
        }

        Thread thread1 = new Thread() {
            @Override
            void run() {
                new While<>(source).all({ item, completion ->
                    synchronized (target) {
                        target.add(item)
                    }
                    completion.done()
                }).run(new WhileDoneCompletion(null) {
                    @Override
                    void done(ErrorCodeList errs) {
                    }
                })
            }
        }

        Thread thread2 = new Thread() {
            @Override
            void run() {
                new While<>(source).all({ item, completion ->
                    synchronized (target) {
                        target.add(item)
                    }
                    completion.done()
                }).run(new WhileDoneCompletion(null) {
                    @Override
                    void done(ErrorCodeList errs) {
                    }
                })
            }
        }

        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        assert target.size() == 2 * source.size()
        target.each {
            assert it != null
        }
    }

    void testRunAll(){
        WhileGlobalProperty.CONCURRENCY_LEVEL_OF_ALL_MODE = 10

        List<Integer> items = new ArrayList()
        int size = ThreadGlobalProperty.MAX_THREAD_NUM  * WhileGlobalProperty.CONCURRENCY_LEVEL_OF_ALL_MODE / 100
        size = size == 0 ? 1 : size
        for (int i = 0; i <= size + 10; i ++) {
            items.add(i)
        }

        AtomicInteger count = new AtomicInteger(0)
        AtomicInteger successCount = new AtomicInteger()

        new While<>(items).all({item, completion ->
            logger.debug(String.format("item %s allDone", item))
            count.addAndGet(1)
            completion.done()
        }).run(new WhileDoneCompletion(null){
            @Override
            void done(ErrorCodeList errs) {
                successCount.incrementAndGet()
                logger.debug("While is done")
            }
        })

        retryInSecs {
            assert count.get() == items.size()
            assert successCount.get() == 1
        }
    }

    void testRunAllWithConcurrencyCoefficientEqualZero(){
        WhileGlobalProperty.CONCURRENCY_LEVEL_OF_ALL_MODE = 0

        List<Integer> items = new ArrayList()
        for (int i = 0; i <= 10; i ++) {
            items.add(i)
        }

        AtomicInteger count = new AtomicInteger(0)
        AtomicInteger successCount = new AtomicInteger()

        new While<>(items).all({item, completion ->
            logger.debug(String.format("item %s allDone", item))
            count.addAndGet(1)
            completion.done()
        }).run(new WhileDoneCompletion(null){
            @Override
            void done(ErrorCodeList errs) {
                logger.debug("While is done")
                successCount.incrementAndGet()
            }
        })

        retryInSecs {
            assert count.get() == items.size()
            assert successCount.get() == 1
        }
    }
}
