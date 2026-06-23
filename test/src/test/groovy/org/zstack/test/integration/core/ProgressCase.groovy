package org.zstack.test.integration.core

import org.apache.logging.log4j.ThreadContext
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.progress.ActionProgressService
import org.zstack.core.progress.ProgressGlobalConfig
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.header.core.progress.TaskProgressInventory
import org.zstack.header.core.progress.TaskProgressVO_
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.core.workflow.FlowDoneHandler
import org.zstack.header.core.workflow.FlowErrorHandler
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.core.workflow.NoRollbackFlow
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

import static org.zstack.header.Constants.THREAD_CONTEXT_API

class ProgressCase extends SubCase {
    @Override
    void clean() {
        SQL.New(TaskProgressVO.class).delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("Progress.xml")
        }
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        testReportUntil()
        testFlowChainProgress()
        testProgressTtlUsesSeconds()
    }

    void testReportUntil() {
        logger.info("Test-001: test action progress service reporter")

        def apiId = Platform.getUuid()
        ThreadContext.put(THREAD_CONTEXT_API, apiId)

        def reporter = ActionProgressService.taskProgress()
                .withContent("testTaskName")
                .withTotalStep(30L)
                .withCurrentStep(0L)
                .report()
        def progressList1A = ActionProgressService.findProgressesByApiId(apiId)
        assert progressList1A.size() == 1
        assert progressList1A[0].content == "testTaskName"
        assert progressList1A[0].totalStep == 30L
        assert progressList1A[0].currentStep == 0L

        retryInSecs { // first save in cache, and 1 ~ 2 seconds later save to db
            assert Q.New(TaskProgressVO.class)
                    .eq(TaskProgressVO_.apiId, apiId)
                    .isExists()
        }

        def progressList1 = Q.New(TaskProgressVO.class)
                .eq(TaskProgressVO_.apiId, apiId)
                .list() as List<TaskProgressVO>
        assert progressList1.size() == 1
        assert progressList1[0].content == "testTaskName"
        assert progressList1[0].totalStep == 30L
        assert progressList1[0].currentStep == 0L
        long id = progressList1[0].id

        reporter.withCurrentStep(22L).report()

        def progressList2A = ActionProgressService.findProgressesByApiId(apiId)
        assert progressList2A.size() == 1
        assert progressList2A[0].content == "testTaskName"
        assert progressList2A[0].totalStep == 30L
        assert progressList2A[0].currentStep == 22L

        retryInSecs { // first save in cache, and 1 ~ 2 seconds later save to db
            assert Q.New(TaskProgressVO.class)
                    .eq(TaskProgressVO_.id, id)
                    .eq(TaskProgressVO_.currentStep, 22L)
                    .isExists()
        }

        def progressList2 = Q.New(TaskProgressVO.class)
                .eq(TaskProgressVO_.apiId, apiId)
                .list() as List<TaskProgressVO>
        assert progressList2.size() == 1
        assert progressList2[0].id == id
        assert progressList2[0].content == "testTaskName"
        assert progressList2[0].totalStep == 30L
    }

    void testFlowChainProgress() {
        logger.info("Test-011: test action progress service reporter")

        def apiId = Platform.getUuid()
        ThreadContext.put(THREAD_CONTEXT_API, apiId)

        List<TaskProgressInventory> flow1ProgressList = []
        List<TaskProgressInventory> flow2ProgressList = []
        List<TaskProgressInventory> chainDoneProgressList = []
        def completed = new AtomicBoolean(false)
        def success = new AtomicBoolean(false)

        def chain = FlowChainBuilder.newSimpleFlowChain()
        chain.setName("Test-011")
        chain.enableProgressReport()
        chain.then(new NoRollbackFlow() {
            String __name__ = "flow-1"
            @Override
            void run(FlowTrigger trigger, Map data) {
                flow1ProgressList.addAll(ActionProgressService.findProgressesByApiId(apiId))
                trigger.next()
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "flow-2"
            @Override
            void run(FlowTrigger trigger, Map data) {
                flow2ProgressList.addAll(ActionProgressService.findProgressesByApiId(apiId))
                trigger.next()
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            void handle(Map data) {
                chainDoneProgressList.addAll(ActionProgressService.findProgressesByApiId(apiId))
                success.set(true)
                completed.set(true)
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            void handle(ErrorCode errCode, Map data) {
                completed.set(true)
            }
        }).start()

        retryInSecs {
            assert completed.get()
        }
        assert success.get()

        assert flow1ProgressList.size() == 1
        assert flow1ProgressList[0].content == "Test-011: flow-1"
        assert flow1ProgressList[0].currentStep == 0
        assert flow1ProgressList[0].totalStep == 2

        assert flow2ProgressList.size() == 1
        assert flow2ProgressList[0].content == "Test-011: flow-2"
        assert flow2ProgressList[0].currentStep == 1
        assert flow2ProgressList[0].totalStep == 2

        assert chainDoneProgressList.size() == 1
        assert chainDoneProgressList[0].content == "Test-011: done"
        assert chainDoneProgressList[0].currentStep == 2
        assert chainDoneProgressList[0].totalStep == 2
    }

    void testProgressTtlUsesSeconds() {
        def oldTtl = ProgressGlobalConfig.PROGRESS_TTL_SECONDS.value()
        def oldInterval = ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.value()
        def oldNow = ActionProgressService.nowInMillis
        def oldLastCleanTime = getStaticField(ActionProgressService.class, "lastCleanTime")

        ProgressGlobalConfig.PROGRESS_TTL_SECONDS.updateValue(86400)
        ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.updateValue(2)

        long now = System.currentTimeMillis()
        ActionProgressService.nowInMillis = now
        setStaticField(ActionProgressService.class, "lastCleanTime", 0L)

        def recentApiId = Platform.getUuid()
        def expiredApiId = Platform.getUuid()
        persistProgress(recentApiId, now - 120_000L)
        persistProgress(expiredApiId, now - TimeUnit.HOURS.toMillis(25))

        def method = ActionProgressService.class.getDeclaredMethod("cleanExpiredProgressIfNeeded")
        method.setAccessible(true)
        method.invoke(null)

        assert Q.New(TaskProgressVO.class)
                .eq(TaskProgressVO_.apiId, recentApiId)
                .isExists()
        assert !Q.New(TaskProgressVO.class)
                .eq(TaskProgressVO_.apiId, expiredApiId)
                .isExists()

        ProgressGlobalConfig.PROGRESS_TTL_SECONDS.updateValue(oldTtl)
        ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.updateValue(oldInterval)
        ActionProgressService.nowInMillis = oldNow
        setStaticField(ActionProgressService.class, "lastCleanTime", oldLastCleanTime)
    }

    void persistProgress(String apiId, long lastOpTime) {
        def vo = new TaskProgressVO()
        vo.apiId = apiId
        vo.content = "ttl-test"
        vo.opaque = "{}"
        vo.createTime = lastOpTime
        vo.lastOpTime = lastOpTime
        vo.currentStep = 1L
        vo.totalStep = 1L
        bean(DatabaseFacade.class).persist(vo)
    }

    void setStaticField(Class clz, String name, Object value) {
        def field = clz.getDeclaredField(name)
        field.setAccessible(true)
        field.set(null, value)
    }

    Object getStaticField(Class clz, String name) {
        def field = clz.getDeclaredField(name)
        field.setAccessible(true)
        return field.get(null)
    }
}

