package org.zstack.test.integration.kvm.status

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.UpdateHostAction
import org.zstack.sdk.UpdateKVMHostAction
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-03-03.
 */
class OperationToConnectingHostCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testMaintainHostWhichIsConnecting()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testMaintainHostWhichIsConnecting() {
        HostSpec hostSpec = env.specByName("kvm") as HostSpec
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        String hostUuid = hostSpec.inventory.uuid
        assert hostUuid

        HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class)
        hvo.status = HostStatus.Connecting

        dbf.updateAndRefresh(hvo)

        assert dbf.findByUuid(hostUuid, HostVO.class).status == HostStatus.Connecting

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.stateEvent = HostStateEvent.maintain
        action.uuid = hostUuid
        action.sessionId = currentEnvSpec.session.uuid

        ChangeHostStateAction.Result res = action.call()
        assert res.error != null


        UpdateHostAction a1 = new UpdateHostAction()
        a1.description = "hahaha"
        a1.uuid = hostUuid
        a1.sessionId = currentEnvSpec.session.uuid

        assert a1.call().error == null

        hvo = dbf.findByUuid(hostUuid, HostVO.class)
        hvo.status = HostStatus.Connected

        dbf.updateAndRefresh(hvo)

        UpdateKVMHostAction a2 = new UpdateKVMHostAction()
        a2.username = "hehehe"
        a2.uuid = hostUuid
        a2.sessionId = currentEnvSpec.session.uuid

        assert a2.call().error == null
    }
}