package org.zstack.test.integration.longjob

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.sdk.SubmitLongJobAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by GuoYi on 12/6/17.
 */
class SubmitLongJobCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        spring {
            include("LongJobManager.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testSubmitLongJobCase()
            testExecuteTime()
        }
    }

    void testSubmitLongJobCase() {
        // check jobName
        SubmitLongJobAction action = new SubmitLongJobAction()
        action.sessionId = adminSession()
        action.jobName = "NoSuchApiMessage"
        action.jobData = "{}"
        SubmitLongJobAction.Result res = action.call()
        assert res.error != null
    }

    void testExecuteTime() {
        def job = mockJobVO()
        assert job.executeTime == null

        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, job.uuid).set(LongJobVO_.state, LongJobState.Canceled).update()

        job = dbf.reload(job)
        assert job.executeTime == null

        job.setState(LongJobState.Succeeded)
        dbf.update(job)
        job = dbf.reload(job)
        long exec2 = job.executeTime
        assert exec2 >= 0

        // try again
        job.setState(LongJobState.Canceled)
        dbf.update(job)
        job = dbf.reload(job)
        assert exec2 == job.executeTime
        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, job.uuid).delete()
    }

    LongJobVO mockJobVO() {
        def vo = new LongJobVO()
        vo.setUuid(Platform.getUuid())
        vo.setName("aaaa")
        vo.setDescription("aaaa")
        vo.setApiId(Platform.getUuid())
        vo.setJobName("aaaa")
        vo.setJobData("bbbbb")
        vo.setState(LongJobState.Waiting)
        vo.setTargetResourceUuid(Platform.getUuid())
        vo.setManagementNodeUuid(Platform.getManagementServerId())
        vo.setAccountUuid("36c27e8ff05c4780bf6d2fa65700f22e")
        return dbf.persistAndRefresh(vo)
    }
}
