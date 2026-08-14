package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.host.HostIpmiVO
import org.zstack.header.host.HostIpmiVO_
import org.zstack.header.host.HostPowerStatus
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KvmHostIpmiPowerExecutor
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PowerResetHostAction
import org.zstack.sdk.ShutdownHostAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * @Author : jingwang
 * @create 2023/5/9 11:21 AM
 *
 */
class KvmHostPowerCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    KVMHostInventory host1
    KVMHostInventory host2
    KVMHostInventory host3
    KVMHostInventory host4
    KVMHostInventory host5
    KvmHostIpmiPowerExecutor executor


    @Override
    void clean() {
        env.delete()
        executor.mockedPowerStatus = null
        executor.mockFail = false
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            executor = bean(KvmHostIpmiPowerExecutor.class)
            cluster = env.inventoryByName("cluster") as ClusterInventory
            addKvmHostWithIpmi()
            testKvmHostShutdownByAgent()
            testKvmHostShutdownByIpmi()
            testKvmHostRebootByAgent()
            testKvmHostRebootByIpmi()
            testKvmHostPowerOn()
            testKvmHostPowerStatus()
            testKvmHostPowerStatusIntervalRefresh()
            testShutdownReturnEarly()
            testRebootReturnEarly()
        }
    }

    void addKvmHostWithIpmi() {
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = "172.25.10.1"
            return rsp
        }
        host1 = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.10"
            clusterUuid = cluster.uuid
        } as KVMHostInventory
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = "172.25.10.2"
            return rsp
        }
        host2 = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.11"
            clusterUuid = cluster.uuid
        } as KVMHostInventory
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = "172.25.10.3"
            return rsp
        }
        host3 = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.12"
            clusterUuid = cluster.uuid
        } as KVMHostInventory
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = "172.25.10.4"
            return rsp
        }
        host4 = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.13"
            clusterUuid = cluster.uuid
        } as KVMHostInventory
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = "172.25.10.5"
            return rsp
        }
        host5 = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.14"
            clusterUuid = cluster.uuid
        } as KVMHostInventory
    }

    void testKvmHostShutdownByAgent() {
        env.simulator(KVMConstant.HOST_SHUTDOWN) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.ShutdownHostCmd()
            return rsp
        }

        shutdownHost {
            uuid = host1.uuid
        }

        assert Q.New(HostVO)
                .eq(HostVO_.uuid, host1.uuid)
                .eq(HostVO_.status, HostStatus.Disconnected)
                .isExists()
    }

    void testKvmHostShutdownByIpmi() {
        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        updateHostIpmi {
            uuid = host2.uuid
            ipmiAddress = "172.25.10.2"
            ipmiUsername = "admin"
            ipmiPassword = "password"
        }
        KVMHostInventory ret
        def t1 = new Thread(new Runnable() {
            @Override
            void run() {
                ret = shutdownHost {
                    uuid = host2.uuid
                } as KVMHostInventory
            }
        })
        t1.start()
        //wait ipmiPowerStatus change
        TimeUnit.SECONDS.sleep(3)
        assert Q.New(HostIpmiVO)
                .eq(HostIpmiVO_.uuid, host2.uuid)
                .eq(HostIpmiVO_.ipmiPowerStatus, HostPowerStatus.POWER_SHUTDOWN)
                .isExists()
        executor.mockedPowerStatus = HostPowerStatus.POWER_OFF
        t1.join()
        assert ret != null
        assert ret.getIpmiPowerStatus() == HostPowerStatus.POWER_OFF.toString()
    }

    void testKvmHostRebootByAgent() {
        env.simulator(KVMConstant.HOST_REBOOT) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.RebootHostCmd()
            return rsp
        }
        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        updateHostIpmi {
            uuid = host3.uuid
            ipmiAddress = "172.25.10.3"
            ipmiUsername = "admin"
            ipmiPassword = "password"
        }
        powerResetHost {
            uuid = host3.uuid
        }

        assert Q.New(HostVO)
                .eq(HostVO_.uuid, host3.uuid)
                .eq(HostVO_.status, HostStatus.Disconnected)
                .isExists()
    }

    void testKvmHostRebootByIpmi() {
        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        updateHostIpmi {
            uuid = host4.uuid
            ipmiAddress = "172.25.10.4"
            ipmiUsername = "admin"
            ipmiPassword = "password"
        }
        powerResetHost {
            uuid = host4.uuid
        }

        assert Q.New(HostVO)
                .eq(HostVO_.uuid, host4.uuid)
                .eq(HostVO_.status, HostStatus.Disconnected)
                .isExists()
    }

    void testKvmHostPowerOn() {
        executor.mockedPowerStatus = HostPowerStatus.POWER_OFF
        updateHostIpmi {
            uuid = host5.uuid
            ipmiAddress = "172.25.10.5"
            ipmiUsername = "admin"
            ipmiPassword = "password"
        }

        KVMHostInventory ret
        def t1 = new Thread(new Runnable() {
            @Override
            void run() {
                ret = powerOnHost {
                    uuid = host5.uuid
                } as KVMHostInventory
            }
        })
        t1.start()
        //wait ipiPowerStatus change
        TimeUnit.SECONDS.sleep(3)
        assert Q.New(HostIpmiVO)
                .eq(HostIpmiVO_.uuid, host5.uuid)
                .eq(HostIpmiVO_.ipmiPowerStatus, HostPowerStatus.POWER_BOOTING)
                .isExists()
        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        t1.join()
        assert ret != null
        assert ret.getIpmiPowerStatus() == HostPowerStatus.POWER_ON.toString()
    }

    void testKvmHostPowerStatus() {
        executor.mockedPowerStatus = HostPowerStatus.POWER_OFF
        assert Q.New(HostIpmiVO)
                .eq(HostIpmiVO_.uuid, host5.uuid)
                .eq(HostIpmiVO_.ipmiPowerStatus, HostPowerStatus.POWER_ON)
                .isExists()
        getHostPowerStatus {
            uuid = host5.uuid
        }
        assert Q.New(HostIpmiVO)
                .eq(HostIpmiVO_.uuid, host5.uuid)
                .eq(HostIpmiVO_.ipmiPowerStatus, HostPowerStatus.POWER_OFF)
                .isExists()
    }

    void testKvmHostPowerStatusIntervalRefresh() {
        int interval = HostGlobalConfig.HOST_POWER_REFRESH_INTERVAL.value(Integer.class)
        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        // wait ipiPowerStatus change by period task
        TimeUnit.SECONDS.sleep(interval+1)
        List<HostIpmiVO> ipmis = Q.New(HostIpmiVO)
                .eq(HostIpmiVO_.uuid, host5.uuid)
                .eq(HostIpmiVO_.ipmiPowerStatus, HostPowerStatus.POWER_OFF)
                .list()
        assert ipmis.find {
            it.ipmiPowerStatus != HostPowerStatus.POWER_ON
        } == null
    }

    void testShutdownReturnEarly() {
        executor.mockFail = true
        def action = new ShutdownHostAction()
        action.uuid = host5.uuid
        action.sessionId = adminSession()
        action.returnEarly = true
        ShutdownHostAction.Result res = action.call()
        assert res.error == null
    }

    void testRebootReturnEarly() {
        executor.mockFail = true
        def action = new PowerResetHostAction()
        action.uuid = host5.uuid
        action.sessionId = adminSession()
        action.returnEarly = true
        PowerResetHostAction.Result res = action.call()
        assert res.error == null
    }
}
