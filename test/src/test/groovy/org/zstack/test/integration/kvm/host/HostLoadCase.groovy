package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostManagerImpl
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

class HostLoadCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    void waitHostDisconnected(String hostUuid) {
        retryInSecs {
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, hostUuid).findValue() == HostStatus.Disconnected
        }
    }

    void waitHostConnected(String hostUuid) {
        retryInSecs {
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, hostUuid).findValue() == HostStatus.Connected
        }
    }

    void testConnectedHostNotReconnectWhenNodeLeft() {
        HostInventory kvm1 = env.inventoryByName("kvm1")
        HostInventory kvm2 = env.inventoryByName("kvm2")

        boolean pingFailure = true
        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.PingCmd.class)

            if (cmd.hostUuid == kvm1.uuid && pingFailure) {
                rsp.success = false
                rsp.error = "on purpose"
            } else {
                rsp.hostUuid = cmd.hostUuid
            }

            return rsp
        }

        waitHostDisconnected(kvm1.uuid)

        HostManagerImpl mgr = bean(HostManagerImpl.class)

        boolean kvm1Connected = false
        boolean kvm2Connected = false
        def cleanup = notifyWhenReceivedMessage(ConnectHostMsg.class) { ConnectHostMsg msg ->
            if (msg.getHostUuid() == kvm1.uuid) {
                kvm1Connected = true
            }

            if (msg.getHostUuid() == kvm2.uuid) {
                kvm2Connected = true
            }
        }

        pingFailure = false

        def inv = new ManagementNodeInventory()
        inv.setUuid(Platform.getUuid())
        inv.setHostName("localhost")
        mgr.nodeLeft(inv)

        waitHostConnected(kvm1.uuid)

        assert kvm1Connected
        // kvm2 is in status of Connected, which should not be reconnected
        assert !kvm2Connected

        cleanup()
    }

    void testConnectedHostNotReconnectWhenNodeReady() {
        HostInventory kvm1 = env.inventoryByName("kvm1")
        HostInventory kvm2 = env.inventoryByName("kvm2")

        boolean pingFailure = true
        env.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.PingCmd.class)

            def rsp = new KVMAgentCommands.PingResponse()
            if (cmd.hostUuid == kvm1.uuid && pingFailure) {
                rsp.success = false
                rsp.error = "on purpose"
            } else {
                rsp.hostUuid = cmd.hostUuid
            }

            return rsp
        }

        waitHostDisconnected(kvm1.uuid)

        HostManagerImpl mgr = bean(HostManagerImpl.class)

        boolean kvm1Connected = false
        boolean kvm2Connected = false
        def cleanup = notifyWhenReceivedMessage(ConnectHostMsg.class) { ConnectHostMsg msg ->
            if (msg.getHostUuid() == kvm1.uuid) {
                kvm1Connected = true
            }

            if (msg.getHostUuid() == kvm2.uuid) {
                kvm2Connected = true
            }
        }

        mgr.managementNodeReady()

        pingFailure = false
        waitHostConnected(kvm1.uuid)

        assert kvm1Connected
        // kvm2 is in status of Connected, which should not be reconnected
        assert !kvm2Connected

        cleanup()
    }

    @Override
    void test() {
        env.create {

            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            HostGlobalConfig.MAXIMUM_PING_FAILURE.updateValue(1)
            HostGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.updateValue(0)
            HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false)

            testConnectedHostNotReconnectWhenNodeLeft()
            testConnectedHostNotReconnectWhenNodeReady()
        }
    }
}
