package org.zstack.test.integration.kvm.host

import org.zstack.header.host.HostVO
import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil


class GetCpuInfoAndPowerSupplyInfoCase extends SubCase {

    EnvSpec env

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
            testCpuInfoByReConnectHostOrAddHost()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCpuInfoByReConnectHostOrAddHost() {

        HostInventory host = env.inventoryByName("kvm")

        KVMAgentCommands.ConnectCmd connectCmd = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            connectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            rsp.success = true
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }

        assert Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, host.uuid).eq(SystemTagVO_.resourceType, HostVO.getSimpleName()).like(SystemTagVO_.tag, "cpuProcessorNum::%").isExists()

    }

}