package org.zstack.test.integration.networkservice.provider

import org.zstack.sdk.CreatePortForwardingRuleAction
import org.zstack.sdk.GetPortForwardingAttachableVmNicsAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 3/31/17.
 */
class VirtualRouterPortForwardingCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        // This environment contains vr-offering but no VM.
        env = VirtualRouterNetworkServiceEnv.twoVmOneHostThreePortForwardingVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            testPortForwardingRuleOperation()
        }
    }

    void testPortForwardingRuleOperation() {
        PortForwardingRuleInventory r1 = env.inventoryByName("pfRule1") as PortForwardingRuleInventory
        PortForwardingRuleInventory r2 = env.inventoryByName("pfRule2") as PortForwardingRuleInventory
        PortForwardingRuleInventory r3 = env.inventoryByName("pfRule3") as PortForwardingRuleInventory
        L3NetworkInventory l3 = env.inventoryByName("GuestNetwork") as L3NetworkInventory
        L3NetworkInventory P_l3 = env.inventoryByName("PublicNetwork") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        VmInstanceInventory vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        VmNicInventory vm1Nic1 = vm.vmNics.get(0)
        VmNicInventory vm2Nic1 = vm2.vmNics.get(0)

        GetPortForwardingAttachableVmNicsAction getPortForwardingAttachableVmNicsAction = new GetPortForwardingAttachableVmNicsAction()
        getPortForwardingAttachableVmNicsAction.ruleUuid = r3.uuid
        getPortForwardingAttachableVmNicsAction.sessionId = adminSession()
        GetPortForwardingAttachableVmNicsAction.Result res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.get(0).uuid != res.value.inventories.get(1).uuid
        assert res.value.inventories.size() == 2

        getPortForwardingAttachableVmNicsAction.ruleUuid = r2.uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.value.inventories.get(0).uuid != res.value.inventories.get(1).uuid
        assert res.error == null
        assert res.value.inventories.size() == 2

        getPortForwardingAttachableVmNicsAction.ruleUuid = r1.uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.value.inventories.get(0).uuid != res.value.inventories.get(1).uuid
        assert res.error == null
        assert res.value.inventories.size() == 2

        attachPortForwardingRule {
            ruleUuid = r1.uuid
            vmNicUuid = vm2Nic1.uuid
        }


        getPortForwardingAttachableVmNicsAction.ruleUuid = r3.uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.get(0).uuid == vm1Nic1.uuid
        assert res.value.inventories.size() == 1

        getPortForwardingAttachableVmNicsAction.ruleUuid = r2.uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.get(0).uuid == vm1Nic1.uuid
        assert res.value.inventories.size() == 1

        getPortForwardingAttachableVmNicsAction.ruleUuid = r1.uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.size() == 0

        // rule4 share the same vip with rule1, so it's attachable to the vm1
        // and only attachable to the vm1 because the vm2 is on another private L3
        CreatePortForwardingRuleAction createPortForwardingRuleAction = new CreatePortForwardingRuleAction()
        createPortForwardingRuleAction.name = "rule4"
        createPortForwardingRuleAction.vipUuid = r1.getVipUuid()
        createPortForwardingRuleAction.vipPortStart = 200
        createPortForwardingRuleAction.vipPortEnd = 220
        createPortForwardingRuleAction.privatePortStart = 200
        createPortForwardingRuleAction.privatePortEnd = 220
        createPortForwardingRuleAction.protocolType = "TCP"
        createPortForwardingRuleAction.vmNicUuid = vm2Nic1.uuid
        createPortForwardingRuleAction.sessionId = adminSession()
        CreatePortForwardingRuleAction.Result res2 = createPortForwardingRuleAction.call()
        assert res2.error == null
        String r4Uuid = res2.value.inventory.uuid

        createPortForwardingRuleAction.name = "rule5"
        createPortForwardingRuleAction.vipUuid = res2.value.inventory.vipUuid
        createPortForwardingRuleAction.vipPortStart = 2000
        createPortForwardingRuleAction.vipPortEnd = 2200
        createPortForwardingRuleAction.privatePortStart = 2000
        createPortForwardingRuleAction.privatePortEnd = 2200
        createPortForwardingRuleAction.vmNicUuid = null
        res2 = createPortForwardingRuleAction.call()
        assert res2.error == null
        String r5Uuid = res2.value.inventory.uuid

        getPortForwardingAttachableVmNicsAction.ruleUuid = r4Uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.size() == 0
        detachPortForwardingRule {
            uuid = r4Uuid
        }
        stopVmInstance {
            uuid = vm2.uuid
        }
        attachPortForwardingRule {
            ruleUuid = r5Uuid
            vmNicUuid = vm2Nic1.uuid
        }
        getPortForwardingAttachableVmNicsAction.ruleUuid = r4Uuid
        res = getPortForwardingAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories.size() == 1
        assert res.value.inventories.get(0).getUuid() == vm2Nic1.uuid
        //Freeing resources
        deletePortForwardingRule {
           uuid = r1.uuid
        }
        deletePortForwardingRule {
            uuid = r2.uuid
        }
        deletePortForwardingRule {
            uuid = r3.uuid
        }
        deletePortForwardingRule {
            uuid = r4Uuid
        }
        deletePortForwardingRule {
            uuid = r5Uuid
        }
    }
}
