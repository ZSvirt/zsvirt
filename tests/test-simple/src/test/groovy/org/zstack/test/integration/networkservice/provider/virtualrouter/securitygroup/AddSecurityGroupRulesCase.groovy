package org.zstack.test.integration.networkservice.provider.virtualrouter.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by Qi Le on 2020/5/22
 */
class AddSecurityGroupRulesCase extends SubCase{
    EnvSpec env

    L3NetworkInventory l3Net, l3Net2
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg
    SecurityGroupInventory sg2

    void testCreateSecurityGroup() {
        sg = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = l3Net.uuid
        }

        vm1 = startVmInstance {
            uuid = vm1.uuid
        } as VmInstanceInventory

        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid, vm4.vmNics[0].uuid]
        }

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 4
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net2.uuid
        }
    }

    void testAttachVmNicWithSecurityGroup() {
        vm1 = detachL3NetworkFromVm {
            vmNicUuid = vm1.vmNics[0].uuid
        } as VmInstanceInventory
        def refs = zqlQuery("query vmNicSecurityGroupRef where vmInstanceUuid='${vm1.uuid}' and securityGroupUuid='${sg.uuid}'") as List<LinkedHashMap>
        assert refs.isEmpty()

        vm1 = attachL3NetworkToVm {
            vmInstanceUuid = vm1.uuid
            l3NetworkUuid = l3Net.uuid
            systemTags = [VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.instantiateTag(map(
                    e(VmSystemTags.L3_UUID_TOKEN, l3Net.uuid),
                    e(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN, sg.uuid)))]
        } as VmInstanceInventory
        refs = zqlQuery("query vmNicSecurityGroupRef where vmInstanceUuid='${vm1.uuid}' and securityGroupUuid='${sg.uuid}'") as List<LinkedHashMap>
        assert refs.size() == 1
        def tags = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(vm1.uuid) as List<String>
        assert tags.isEmpty()
    }

    void testChangeVmNicWithSecurityGroup() {
        changeVmNicNetwork {
            vmNicUuid = (vm1.vmNics as List<VmNicInventory>).get(0).uuid
            destL3NetworkUuid = l3Net2.uuid
            systemTags = [VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.instantiateTag(map(
                    e(VmSystemTags.L3_UUID_TOKEN, l3Net2.uuid),
                    e(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN, sg2.uuid)))]
        }

        def refs = zqlQuery("query vmNicSecurityGroupRef where vmInstanceUuid='${vm1.uuid}' and securityGroupUuid='${sg.uuid}'") as List<LinkedHashMap>
        assert refs.isEmpty()
        refs = zqlQuery("query vmNicSecurityGroupRef where vmInstanceUuid='${vm1.uuid}' and securityGroupUuid='${sg2.uuid}'") as List<LinkedHashMap>
        assert refs.size() == 1
        def tags = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTags(vm1.uuid) as List<String>
        assert tags.isEmpty()
    }

    void testAddMultiRulesToSecurityGroup(int num) {
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        def ruleList = []
        for (int i = 0; i < num; i++) {
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO(
                    type: i % 2 == 0 ? "Ingress" : "Egress",
                    ipVersion: 4,
                    startPort: 10000 + i / 2,
                    endPort: 10000 + i / 2,
                    allowedCidr: "192.168.100.0/24",
                    protocol: "TCP"
            )
            ruleList.add(rule)
        }

        sg = addSecurityGroupRule {
            securityGroupUuid = sg.uuid
            rules = ruleList
        } as SecurityGroupInventory

        /* 2 ipv4 base rule + 2 ipv6 base rule */
        assert sg.rules.size() == num + 4

        retryInSecs {
            assert cmd != null
        }
    }

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
        env = VirtualRouterNetworkServiceEnv.fourVmThreeHostNoEipForSecurityGroupEnv()
    }

    @Override
    void test() {
        env.create {
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            l3Net2 = env.inventoryByName("l3-2") as L3NetworkInventory
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory // vm4 in host3
            testCreateSecurityGroup()
            testAttachVmNicWithSecurityGroup()
            testChangeVmNicWithSecurityGroup()
            testAddMultiRulesToSecurityGroup(100)
        }
    }
}
