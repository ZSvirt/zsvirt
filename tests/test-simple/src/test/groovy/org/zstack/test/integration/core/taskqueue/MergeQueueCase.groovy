package org.zstack.test.integration.core.taskqueue

import org.zstack.core.thread.MergeQueue
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

/**
 * Created by mingjian.deng on 2020/4/29.*/
class MergeQueueCase extends SubCase {
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
        test1()
        test2()
        test3()
        test4()
    }

    /**
     * running twice
     */
    void test1() {
        def x = new AtomicInteger(0)
        def s = new Supplier<Void>() {
            @Override
            Void get() {
                sleep 20
                x.incrementAndGet()
                return null
            }
        }


        (1..10).each {
            new MergeQueue().addTask("test1", s).run()
        }

        sleep 300
        assert x.intValue() == 2
    }

    /**
     * running 10 times
     */
    void test2() {
        def x = new AtomicInteger(0)
        def s = new Supplier<Void>() {
            @Override
            Void get() {
                sleep 20
                x.incrementAndGet()
                return null
            }
        }

        (1..10).each {
            new MergeQueue().addTask("test1", s).run()
            sleep 50      // only difference between test1() and test2()
        }

        sleep 1000
        assert x.intValue() == 10
    }

    /**
     * running twice both queue "test1" and "test2", 4 times in total
     */
    void test3() {
        def x = new AtomicInteger(0)
        def s = new Supplier<Void>() {
            @Override
            Void get() {
                sleep 20
                x.incrementAndGet()
                return null
            }
        }

        (1..10).each {
            new MergeQueue().addTask("test1", s).run()
        }

        (1..10).each {
            new MergeQueue().addTask("test2", s).run()
        }

        sleep 300
        assert x.intValue() == 4
    }

    /**
     * running 4 times
     */
    void test4() {
        def x = new AtomicInteger(0)
        def s = new Supplier<Void>() {
            @Override
            Void get() {
                sleep 20
                x.incrementAndGet()
                return null
            }
        }


        (1..10).each {
            new MergeQueue().addTask("test1", s).setSyncLevel(3).run()  // setSyncLevel 3, so run 4 times
        }

        sleep 300
        assert x.intValue() == 4
    }
}
