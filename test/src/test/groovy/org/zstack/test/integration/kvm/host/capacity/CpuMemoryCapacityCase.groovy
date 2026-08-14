package org.zstack.test.integration.kvm.host.capacity

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.GetCpuMemoryCapacityAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ReconnectHostAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.hostallocator.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by camile on 2017/4/10.
 */
class CpuMemoryCapacityCase extends SubCase {
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
        env = Env.noVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1G")
            KVMGlobalConfig.KVM_IGNORE_MSRS.updateValue(true)
            HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false)
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(5)
            setHostDisconnecedAndGetCorrectlyCpuMemoryCapacity()
        }
    }

    void setHostDisconnecedAndGetCorrectlyCpuMemoryCapacity() {
        GetCpuMemoryCapacityAction action = new GetCpuMemoryCapacityAction()
        action.all = true
        action.sessionId = adminSession()
        GetCpuMemoryCapacityAction.Result res = action.call()
        assert res.error == null
        long result = res.value.availableMemory
        assert result == SizeUtils.sizeStringToBytes("27G")

        HostInventory kvm1Inv = (env.specByName("kvm1") as HostSpec).inventory
        HostInventory kvm2Inv = (env.specByName("kvm2") as HostSpec).inventory
        HostInventory kvm3Inv = (env.specByName("kvm3") as HostSpec).inventory

        KVMAgentCommands.ReconnectMeCmd reconnectMeCmd = null
        KVMAgentCommands.ConnectCmd connectCmd = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            connectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            rsp.success = false
            if (connectCmd.hostUuid == kvm3Inv.uuid) {
                rsp.success = true
            }
            return rsp
        }
        KVMAgentCommands.ReconnectMeCmd reConnectCmd = null
        env.afterSimulator(KVMConstant.KVM_RECONNECT_ME) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            reConnectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ReconnectMeCmd.class)
            rsp.success = false
            if ((reconnectMeCmd.hostUuid == kvm3Inv.uuid)) {
                rsp.success = true
            }
            return rsp
        }


        ReconnectHostAction reconnectHostAction = new ReconnectHostAction()
        reconnectHostAction.uuid = kvm1Inv.uuid
        reconnectHostAction.sessionId = adminSession()
        ReconnectHostAction.Result reconnectRes = reconnectHostAction.call()
        reconnectRes.error !=null
        reconnectHostAction.uuid = kvm2Inv.uuid
        reconnectRes = reconnectHostAction.call()
        reconnectRes.error !=null

        assert connectCmd.ignoreMsrs

        retryInSecs{
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, kvm1Inv.uuid).findValue().toString() == HostStatus.Disconnected.toString()
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, kvm2Inv.uuid).findValue().toString() == HostStatus.Disconnected.toString()
        }
        if (Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, kvm3Inv.uuid).findValue().toString() != HostStatus.Connected.toString()) {
            reconnectHost {
                uuid = kvm3Inv.uuid
            }
        }
        retryInSecs{
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, kvm3Inv.uuid).findValue().toString() == HostStatus.Connected.toString()
        }

        res = action.call()
        assert res.error == null
        result = res.value.availableMemory
        assert result == SizeUtils.sizeStringToBytes("9G")
    }


}
