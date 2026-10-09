package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.core.ThreadFacadeForTest

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BooleanSupplier

class ThreadFacadeForTestCase extends SubCase {
    EnvSpec env
    ThreadFacadeForTest thdf
    List<Future> futures = []
    List<BooleanSupplier> blockCancels = []

    @Override
    void setup() {
        useSpring(SimpleTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create {
            thdf = threadFacadeForTest()
            assert thdf != null

            testUnblockedPeriodicTaskRuns()
            testExactNameIsSkippedUntilTriggered()
            testWildcardNameIsSkippedUntilTriggered()
            testTriggerReturnsErrorCodeWhenTaskFails()
        }
    }

    @Override
    void clean() {
        futures.each { it.cancel(true) }
        blockCancels.each { it.getAsBoolean() }
        env.delete()
    }

    void testUnblockedPeriodicTaskRuns() {
        logger.info("Test 001: submit a periodic task that is not in the block list, expect the scheduler to run it")

        AtomicInteger runs = new AtomicInteger(0)
        submitTask("ut-tf-open-task", { runs.incrementAndGet() })

        retryInSecs {
            assert runs.get() >= 1
        }
    }

    void testExactNameIsSkippedUntilTriggered() {
        logger.info("Test 101: block an exact task name, expect the scheduler not to run it")

        String taskName = "ut-tf-exact-task"
        BooleanSupplier cancel = thdf.addBlockedPeriodicTask(taskName)
        assert cancel != null
        blockCancels.add(cancel)

        AtomicInteger runs = new AtomicInteger(0)
        submitTask(taskName, { runs.incrementAndGet() })
        TimeUnit.MILLISECONDS.sleep(1000)
        assert runs.get() == 0

        logger.info("Test 102: trigger the exact blocked task, expect it to run on the current thread and return no error")
        ErrorCode errorCode = thdf.triggerBlockedPeriodicTask(taskName)
        assert errorCode == null
        assert runs.get() == 1

        logger.info("Test 199: cancel the exact-name block, expect the registration removed")
        assert cancel.getAsBoolean()
        assert !cancel.getAsBoolean()
    }

    void testWildcardNameIsSkippedUntilTriggered() {
        logger.info("Test 201: block a wildcard task name, expect the scheduler not to run a matching task")

        String pattern = "ut-tf-wild-*"
        String taskName = "ut-tf-wild-alpha"
        BooleanSupplier cancel = thdf.addBlockedPeriodicTask(pattern)
        assert cancel != null
        blockCancels.add(cancel)

        AtomicInteger runs = new AtomicInteger(0)
        submitTask(taskName, { runs.incrementAndGet() })
        TimeUnit.MILLISECONDS.sleep(1000)
        assert runs.get() == 0

        logger.info("Test 202: trigger the wildcard blocked task, expect the matching task to run and return no error")
        ErrorCode errorCode = thdf.triggerBlockedPeriodicTask(pattern)
        assert errorCode == null
        assert runs.get() == 1

        logger.info("Test 299: cancel the wildcard block, expect the registration removed")
        assert cancel.getAsBoolean()
        assert !cancel.getAsBoolean()
    }

    void testTriggerReturnsErrorCodeWhenTaskFails() {
        logger.info("Test 301: trigger a blocked task that throws OperationFailureException, expect its ErrorCode")

        String argumentFailureName = "ut-tf-arg-fail-task"
        BooleanSupplier argumentCancel = thdf.addBlockedPeriodicTask(argumentFailureName)
        blockCancels.add(argumentCancel)
        submitTask(argumentFailureName, {
            throw Platform.argerr("on purpose").toException()
        })

        ErrorCode argumentError = thdf.triggerBlockedPeriodicTask(argumentFailureName)
        assert argumentError != null
        assert argumentError.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert argumentError.details.contains("on purpose")

        logger.info("Test 302: trigger a blocked task that throws RuntimeException, expect an operation ErrorCode")

        String runtimeFailureName = "ut-tf-runtime-fail-task"
        BooleanSupplier runtimeCancel = thdf.addBlockedPeriodicTask(runtimeFailureName)
        blockCancels.add(runtimeCancel)
        submitTask(runtimeFailureName, {
            throw new RuntimeException("on purpose")
        })

        ErrorCode runtimeError = thdf.triggerBlockedPeriodicTask(runtimeFailureName)
        assert runtimeError != null
        assert runtimeError.code == SysErrors.OPERATION_ERROR.toString()
        assert runtimeError.details.contains("on purpose")

        logger.info("Test 399: cancel the failure-task blocks, expect both registrations removed")
        assert argumentCancel.getAsBoolean()
        assert runtimeCancel.getAsBoolean()
    }

    private void submitTask(String name, Closure body) {
        futures.add(thdf.submitPeriodicTask(new TestPeriodicTask(name, body)))
    }
}
