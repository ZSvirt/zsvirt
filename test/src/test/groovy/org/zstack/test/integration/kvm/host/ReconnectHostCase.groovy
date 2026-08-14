package org.zstack.test.integration.kvm.host

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.*
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMHostVO
import org.zstack.kvm.KVMHostVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by MaJin on 2017-05-01.
 */
class ReconnectHostCase extends SubCase {
    EnvSpec env
    HostInventory host
    static RECONNECT_TIME = 10
    int originVmCount

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testReconnectHostVmState()
            testReconnectFailureHostVmState()
            testUpdateHostDuringConnecting()
            testChangeHostConnectionState()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void prepare() {
        host = env.inventoryByName("kvm") as HostInventory

        originVmCount = (queryVmInstance {
            conditions = ["state=${VmInstanceState.Running}"]
        } as List<VmInstanceInventory>).size()
        logger.info("originVmCount = ${originVmCount}".toString())
    }

    void testUpdateHostDuringConnecting() {
        boolean called
        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            updateKVMHost {
                uuid = host.uuid
                sshPort = 23
            }

            if (!called) {
                rsp.setError("on purpose")
            }

            return rsp
        }
        expect(AssertionError.class) {
            reconnectHost {
                uuid = host.uuid
            }
        }

        called = true
        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 23

        updateKVMHost {
            uuid = host.uuid
            sshPort = 22
        }
        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 22
        reconnectHost {
            uuid = host.uuid
        }

        assert Q.New(KVMHostVO.class).select(KVMHostVO_.port).findValue() == 23

        updateKVMHost {
            uuid = host.uuid
            sshPort = 22
        }
        env.cleanSimulatorHandlers()
    }

    void testReconnectFailureHostVmState() {
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            throw new Exception("on purpose")
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = vmInv.hostUuid
            }
        }

        retryInSecs {
            List<VmInstanceInventory> vmInvs = queryVmInstance {
                conditions = ["state=${VmInstanceState.Unknown}"]
            } as List<VmInstanceInventory>

            assert vmInvs.size() == originVmCount
        }
    }

    // Reconnect host will not change VM's state, this test confirm VMs are always Running after host reconnecting
    void testReconnectHostVmState() {
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        for (int i = 0; i < RECONNECT_TIME; i++) {
            reconnectHost {
                uuid = vmInv.hostUuid
                sessionId = currentEnvSpec.session.uuid
            }

            retryInSecs() {
                List<VmInstanceInventory> vmInvs = queryVmInstance {
                    conditions = ["state=${VmInstanceState.Running}"]
                } as List<VmInstanceInventory>

                assert vmInvs.size() == originVmCount
            }
        }
    }

    void testChangeHostConnectionState() {
        // set host to disconnected
        SQL.New(HostVO.class)
                .eq(HostVO_.uuid, host.uuid)
                .set(HostVO_.status, HostStatus.Disconnected)
                .update()

        // extpEmitter.connectionReestablished()
        CloudBus bus = bean(CloudBus.class)
        ChangeHostConnectionStateMsg msg = new ChangeHostConnectionStateMsg()
        msg.hostUuid = host.uuid
        msg.connectionStateEvent = HostStatusEvent.connected.toString()
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.uuid)
        assert bus.call(msg).success
        assert Q.New(HostVO.class)
                .eq(HostVO_.uuid, host.uuid)
                .select(HostVO_.status)
                .findValue() == HostStatus.Connected
    }
}
