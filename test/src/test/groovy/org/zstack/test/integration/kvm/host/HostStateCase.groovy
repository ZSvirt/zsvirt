package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.cloudbus.EventFacadeImpl
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.*
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

/**
 * Created by lining on 02/03/2017.
 */
class HostStateCase extends SubCase {

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
            testChangeHostStateByRealizeHost()
            testPingMaximumFailure()
            testAddMultipleHostConcurrently()
            testChangeHostStateNotConnected()
        }
    }

    void testChangeHostStateByRealizeHost() {
        HostSpec spec = env.specByName("kvm")

        HostInventory inventory = changeHostState {
            sessionId = Test.currentEnvSpec.session.uuid
            uuid = spec.inventory.uuid
            stateEvent = HostStateEvent.enable
        }

        assert inventory.uuid == spec.inventory.uuid
        assert inventory.status == HostStatus.Connected.name()
    }

    void testPingMaximumFailure() {
        HostInventory hinv = env.inventoryByName("kvm")
        def maxFailure = HostGlobalConfig.MAXIMUM_PING_FAILURE.value(Integer.class)
        def count = 1

        env.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.PingCmd.class)
            def rsp = new KVMAgentCommands.PingResponse()
            if (hinv.uuid == cmd.hostUuid && count < maxFailure) {
                count += 1
                throw new Exception("on purpose")
            }
            rsp.hostUuid = cmd.hostUuid
            return rsp
        }

        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)

        1.upto(5) {
            TimeUnit.SECONDS.sleep(1)
            def hvo = dbFindByUuid(hinv.uuid, HostVO.class)
            assert hvo.status == HostStatus.Connected
        }

        def caughtDisconnected = false
        EventFacade evtf = bean(EventFacadeImpl.class)
        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data
                if (d.hostUuid == hinv.uuid && d.newStatus == HostStatus.Disconnected.toString()) {
                    caughtDisconnected = true
                }
            }
        })

        maxFailure = 10
        retryInSecs(10) {
            assert caughtDisconnected
        }
    }

    void testAddMultipleHostConcurrently() {
        def ipAddr = "127.0.0.11"
        ClusterInventory clusterInv = env.inventoryByName("cluster") as ClusterInventory
        def threads = []
        1.upto(8, { it ->
            def vmName = "test-${it}"
            def thread = Thread.start {
                try {
                    addKVMHost {
                        name = vmName
                        managementIp = ipAddr
                        username = "root"
                        password = "password"
                        clusterUuid = clusterInv.uuid
                    }
                } catch (Throwable ignored) {
                }
            }

            threads.add(thread)
        })

        threads.each { it.join() }

        assert Q.New(HostVO.class).eq(HostVO_.managementIp, ipAddr).count() == 1L
    }

    void testChangeHostStateNotConnected() {
        HostSpec spec = env.specByName("kvm")

        // set host to disconnected
        SQL.New(HostVO.class)
                .eq(HostVO_.uuid, spec.inventory.uuid)
                .set(HostVO_.status, HostStatus.Disconnected)
                .update()

        def action = new ChangeHostStateAction()
        action.sessionId = Test.currentEnvSpec.session.uuid
        action.uuid = spec.inventory.uuid
        action.stateEvent = HostStateEvent.maintain
        def res = action.call()

        assert res.error != null
        assert res.error.code == SysErrors.OPERATION_ERROR.toString()
    }

    @Override
    void clean() {
        env.delete()
    }

}
