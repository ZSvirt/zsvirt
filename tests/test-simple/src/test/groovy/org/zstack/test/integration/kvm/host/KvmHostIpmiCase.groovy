package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.header.host.HostIpmiVO
import org.zstack.header.host.HostIpmiVO_
import org.zstack.header.host.HostPowerStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KvmHostIpmiPowerExecutor
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @Author : jingwang
 * @create 2023/5/5 3:42 PM
 */
class KvmHostIpmiCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster1
    KvmHostIpmiPowerExecutor executor

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            cluster1 = env.inventoryByName("cluster") as ClusterInventory
            executor = bean(KvmHostIpmiPowerExecutor.class)
            testAddKvmHostWithIpmi()
            testUpdateKvmHostIpmi()
            testDeleteKvmHostWithIpmi()
            testAddHostsWithSameIpmiAddr()
            testAddHostWithNoneIpmiAddr()
        }
    }

    void testAddKvmHostWithIpmi() {
        def ipmiAddress = "172.25.10.159"
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = ipmiAddress
            return rsp
        }

        def host = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.10"
            clusterUuid = cluster1.uuid
        } as KVMHostInventory

        def tags = querySystemTag {
            delegate.conditions = [
                    "resourceUuid=${host.uuid}",
                    "tag=ipmiAddress::${ipmiAddress}"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1
        assert Q.New(HostIpmiVO.class).eq(HostIpmiVO_.ipmiAddress, "172.25.10.159").isExists()
    }

    void testDeleteKvmHostWithIpmi() {
        String hostUuid = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .findValue()

        deleteHost {
            uuid = hostUuid
            deleteMode = "Enforcing"
        }
        assert !Q.New(HostIpmiVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .isExists()
    }

    void testUpdateKvmHostIpmi() {
        String hostUuid = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .findValue()

        executor.mockedPowerStatus = HostPowerStatus.POWER_ON
        updateHostIpmi {
            uuid = hostUuid
            ipmiAddress = "127.0.0.11"
            ipmiUsername = "admin"
            ipmiPassword = "password"
        }

        HostIpmiVO ipmi = Q.New(HostIpmiVO.class)
            .eq(HostIpmiVO_.uuid, hostUuid)
            .find()
        assert ipmi.ipmiAddress == "127.0.0.11"
        assert ipmi.ipmiUsername == "admin"
        assert ipmi.ipmiPassword == "password"
    }

    void testAddHostsWithSameIpmiAddr() {
        def ipmiAddress = "0.0.0.0"
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = ipmiAddress
            return rsp
        }

        addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.12"
            clusterUuid = cluster1.uuid
        } as KVMHostInventory
        addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.13"
            clusterUuid = cluster1.uuid
        } as KVMHostInventory

        assert Q.New(HostIpmiVO.class).eq(HostIpmiVO_.ipmiAddress, "0.0.0.0").count() == 2
    }

    void testAddHostWithNoneIpmiAddr() {
        def ipmiAddress = "None"
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.ipmiAddress = ipmiAddress
            return rsp
        }

        def host = addKVMHost {
            name = "host"
            username = "root"
            password = "password"
            managementIp = "127.0.0.14"
            clusterUuid = cluster1.uuid
        } as KVMHostInventory

        def ipmi = Q.New(HostIpmiVO.class).eq(HostIpmiVO_.uuid, host.uuid)
                .find() as HostIpmiVO
        assert ipmi.ipmiAddress == null
    }
}
