package org.zstack.test.integration.core

import org.zstack.core.workflow.SimpleFlowChain
import org.zstack.header.core.AsyncBackup
import org.zstack.header.core.workflow.Flow
import org.zstack.header.core.workflow.FlowRollback
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.errorcode.ErrorCode
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

import java.util.function.Consumer

class SimpleFlowChainCase extends SubCase {
    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testFailBeforeNoRollbackBefore()
        testFailOnTheFirstFlowAfterNoRollbackBefore()
        testFailOnALaterFlowAfterNoRollbackBefore()
        testNearestNoRollbackBeforeCutsEarlierRollbacks()
        testChainDoneAfterNoRollbackBefore()
    }

    @Override
    void clean() {
    }

    void testFailBeforeNoRollbackBefore() {
        logger.info("Test 001: fail step-3 before noRollbackBefore, expect rollback step-3, step-2, step-1, then error and Finally")

        List<String> runs = []
        List<String> rollbacks = []
        List<String> callbacks = []
        ErrorCode step3Error = new ErrorCode("SYS.1006", "operation error", "step-3 failed")
        ErrorCode handledError = null

        SimpleFlowChain chain = SimpleFlowChain.of("fail-before-no-rollback-before")
                .then(Flow.of("step-1")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-1")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-1")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-2")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-2")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-2")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-3")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-3")
                            trigger.fail(step3Error)
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-3")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-4")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-4")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-4")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-5")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-5")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-5")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .propagateExceptionTo(new AsyncBackup() {})
                .done({
                    callbacks.add("done")
                } as Runnable)
                .error({ ErrorCode err ->
                    callbacks.add("error")
                    handledError = err
                } as Consumer)
                .Finally({
                    callbacks.add("finally")
                } as Runnable)

        List<Flow> flows = chain.getFlows()
        assert flows.size() == 6
        assert flows[0].name() == "step-1"
        assert flows[1].name() == "step-2"
        assert flows[2].name() == "step-3"
        assert flows[3].name() == "no-rollback-before"
        assert flows[4].name() == "step-4"
        assert flows[5].name() == "step-5"

        chain.start()

        assert runs.size() == 3
        assert runs[0] == "step-1"
        assert runs[1] == "step-2"
        assert runs[2] == "step-3"
        assert !runs.contains("step-4")
        assert !runs.contains("step-5")

        assert rollbacks.size() == 3
        assert rollbacks[0] == "step-3"
        assert rollbacks[1] == "step-2"
        assert rollbacks[2] == "step-1"
        assert !rollbacks.contains("step-4")
        assert !rollbacks.contains("step-5")

        assert handledError.is(step3Error)
        assert handledError.code == "SYS.1006"
        assert handledError.description == "operation error"
        assert handledError.details == "step-3 failed"
        assert handledError.location != null
        assert handledError.location.contains("step-3")

        assert callbacks.size() == 2
        assert callbacks[0] == "error"
        assert callbacks[1] == "finally"
    }

    void testFailOnTheFirstFlowAfterNoRollbackBefore() {
        logger.info("Test 101: fail step-4 after noRollbackBefore, expect only step-4 rollback, then error and Finally")

        List<String> runs = []
        List<String> rollbacks = []
        List<String> callbacks = []
        ErrorCode step4Error = new ErrorCode("SYS.1006", "operation error", "step-4 failed")
        ErrorCode handledError = null

        SimpleFlowChain chain = SimpleFlowChain.of("fail-on-first-flow-after-no-rollback-before")
                .then(Flow.of("step-1")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-1")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-1")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-2")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-2")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-2")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-3")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-3")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-3")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-4")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-4")
                            trigger.fail(step4Error)
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-4")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-5")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-5")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-5")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .propagateExceptionTo(new AsyncBackup() {})
                .done({
                    callbacks.add("done")
                } as Runnable)
                .error({ ErrorCode err ->
                    callbacks.add("error")
                    handledError = err
                } as Consumer)
                .Finally({
                    callbacks.add("finally")
                } as Runnable)

        List<Flow> flows = chain.getFlows()
        assert flows.size() == 6
        assert flows[0].name() == "step-1"
        assert flows[1].name() == "step-2"
        assert flows[2].name() == "step-3"
        assert flows[3].name() == "no-rollback-before"
        assert flows[4].name() == "step-4"
        assert flows[5].name() == "step-5"

        chain.start()

        assert runs.size() == 4
        assert runs[0] == "step-1"
        assert runs[1] == "step-2"
        assert runs[2] == "step-3"
        assert runs[3] == "step-4"
        assert !runs.contains("step-5")

        assert rollbacks.size() == 1
        assert rollbacks[0] == "step-4"
        assert !rollbacks.contains("step-1")
        assert !rollbacks.contains("step-2")
        assert !rollbacks.contains("step-3")
        assert !rollbacks.contains("step-5")

        assert handledError.is(step4Error)
        assert handledError.code == "SYS.1006"
        assert handledError.description == "operation error"
        assert handledError.details == "step-4 failed"
        assert handledError.location != null
        assert handledError.location.contains("step-4")

        assert callbacks.size() == 2
        assert callbacks[0] == "error"
        assert callbacks[1] == "finally"
    }

    void testFailOnALaterFlowAfterNoRollbackBefore() {
        logger.info("Test 201: fail step-5 after noRollbackBefore, expect rollback step-5 then step-4, then error and Finally")

        List<String> runs = []
        List<String> rollbacks = []
        List<String> callbacks = []
        ErrorCode step5Error = new ErrorCode("SYS.1006", "operation error", "step-5 failed")
        ErrorCode handledError = null

        SimpleFlowChain chain = SimpleFlowChain.of("fail-on-later-flow-after-no-rollback-before")
                .then(Flow.of("step-1")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-1")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-1")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-2")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-2")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-2")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-3")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-3")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-3")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-4")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-4")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-4")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-5")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-5")
                            trigger.fail(step5Error)
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-5")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .propagateExceptionTo(new AsyncBackup() {})
                .done({
                    callbacks.add("done")
                } as Runnable)
                .error({ ErrorCode err ->
                    callbacks.add("error")
                    handledError = err
                } as Consumer)
                .Finally({
                    callbacks.add("finally")
                } as Runnable)

        List<Flow> flows = chain.getFlows()
        assert flows.size() == 6
        assert flows[0].name() == "step-1"
        assert flows[1].name() == "step-2"
        assert flows[2].name() == "step-3"
        assert flows[3].name() == "no-rollback-before"
        assert flows[4].name() == "step-4"
        assert flows[5].name() == "step-5"

        chain.start()

        assert runs.size() == 5
        assert runs[0] == "step-1"
        assert runs[1] == "step-2"
        assert runs[2] == "step-3"
        assert runs[3] == "step-4"
        assert runs[4] == "step-5"

        assert rollbacks.size() == 2
        assert rollbacks[0] == "step-5"
        assert rollbacks[1] == "step-4"
        assert !rollbacks.contains("step-1")
        assert !rollbacks.contains("step-2")
        assert !rollbacks.contains("step-3")

        assert handledError.is(step5Error)
        assert handledError.code == "SYS.1006"
        assert handledError.description == "operation error"
        assert handledError.details == "step-5 failed"
        assert handledError.location != null
        assert handledError.location.contains("step-5")

        assert callbacks.size() == 2
        assert callbacks[0] == "error"
        assert callbacks[1] == "finally"
    }

    void testNearestNoRollbackBeforeCutsEarlierRollbacks() {
        logger.info("Test 301: fail step-4 after the second noRollbackBefore, expect only step-4 rollback, then error and Finally")

        List<String> runs = []
        List<String> rollbacks = []
        List<String> callbacks = []
        ErrorCode step4Error = new ErrorCode("SYS.1006", "operation error", "step-4 failed")
        ErrorCode handledError = null

        SimpleFlowChain chain = SimpleFlowChain.of("nearest-no-rollback-before")
                .then(Flow.of("step-1")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-1")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-1")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-2")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-2")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-2")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-3")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-3")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-3")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-4")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-4")
                            trigger.fail(step4Error)
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-4")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .propagateExceptionTo(new AsyncBackup() {})
                .done({
                    callbacks.add("done")
                } as Runnable)
                .error({ ErrorCode err ->
                    callbacks.add("error")
                    handledError = err
                } as Consumer)
                .Finally({
                    callbacks.add("finally")
                } as Runnable)

        List<Flow> flows = chain.getFlows()
        assert flows.size() == 6
        assert flows[0].name() == "step-1"
        assert flows[1].name() == "step-2"
        assert flows[2].name() == "no-rollback-before"
        assert flows[3].name() == "step-3"
        assert flows[4].name() == "no-rollback-before"
        assert flows[5].name() == "step-4"

        chain.start()

        assert runs.size() == 4
        assert runs[0] == "step-1"
        assert runs[1] == "step-2"
        assert runs[2] == "step-3"
        assert runs[3] == "step-4"

        assert rollbacks.size() == 1
        assert rollbacks[0] == "step-4"
        assert !rollbacks.contains("step-1")
        assert !rollbacks.contains("step-2")
        assert !rollbacks.contains("step-3")

        assert handledError.is(step4Error)
        assert handledError.code == "SYS.1006"
        assert handledError.description == "operation error"
        assert handledError.details == "step-4 failed"
        assert handledError.location != null
        assert handledError.location.contains("step-4")

        assert callbacks.size() == 2
        assert callbacks[0] == "error"
        assert callbacks[1] == "finally"
    }

    void testChainDoneAfterNoRollbackBefore() {
        logger.info("Test 401: every flow succeeds after noRollbackBefore, expect done then Finally and no rollback")

        List<String> runs = []
        List<String> rollbacks = []
        List<String> callbacks = []

        SimpleFlowChain chain = SimpleFlowChain.of("done-after-no-rollback-before")
                .then(Flow.of("step-1")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-1")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-1")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-2")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-2")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-2")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-3")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-3")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-3")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .noRollbackBefore()
                .then(Flow.of("step-4")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-4")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-4")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .then(Flow.of("step-5")
                        .handle({ FlowTrigger trigger ->
                            runs.add("step-5")
                            trigger.next()
                        } as Consumer)
                        .rollback({ FlowRollback trigger ->
                            rollbacks.add("step-5")
                            trigger.rollback()
                        } as Consumer)
                        .build())
                .propagateExceptionTo(new AsyncBackup() {})
                .done({
                    callbacks.add("done")
                } as Runnable)
                .error({ ErrorCode err ->
                    callbacks.add("error")
                } as Consumer)
                .Finally({
                    callbacks.add("finally")
                } as Runnable)

        List<Flow> flows = chain.getFlows()
        assert flows.size() == 6
        assert flows[0].name() == "step-1"
        assert flows[1].name() == "step-2"
        assert flows[2].name() == "step-3"
        assert flows[3].name() == "no-rollback-before"
        assert flows[4].name() == "step-4"
        assert flows[5].name() == "step-5"

        chain.start()

        assert runs.size() == 5
        assert runs[0] == "step-1"
        assert runs[1] == "step-2"
        assert runs[2] == "step-3"
        assert runs[3] == "step-4"
        assert runs[4] == "step-5"

        assert rollbacks.isEmpty()
        assert !rollbacks.contains("step-1")
        assert !rollbacks.contains("step-2")
        assert !rollbacks.contains("step-3")
        assert !rollbacks.contains("step-4")
        assert !rollbacks.contains("step-5")

        assert callbacks.size() == 2
        assert callbacks[0] == "done"
        assert callbacks[1] == "finally"
    }
}
