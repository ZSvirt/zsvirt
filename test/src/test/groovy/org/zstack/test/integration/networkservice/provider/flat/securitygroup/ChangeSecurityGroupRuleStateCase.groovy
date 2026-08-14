package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.RuleTO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ChangeSecurityGroupRuleStateCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testChangeMultipleRules() {
        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 10; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.srcIpRange = "172.16." + i + ".0/24"
            r.protocol = "TCP"
            ingressRules.add(r)
        }

        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = ingressRules
        }

        SecurityGroupRuleInventory rule1 = sg1.rules.find {it.priority == 1 && it.type == "Ingress"}
        SecurityGroupRuleInventory rule3 = sg1.rules.find {it.priority == 3 && it.type == "Ingress"}
        SecurityGroupRuleInventory rule5 = sg1.rules.find {it.priority == 5 && it.type == "Ingress"}

        assert rule1.state == "Enabled"
        assert rule3.state == "Enabled"
        assert rule5.state == "Enabled"

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        List<RuleTO> ip4Rules = new ArrayList<>()
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1.vmNics[0].uuid
            ip4Rules = cmd.ruleTOs.get(sg1.uuid)
            assert ip4Rules.size() == 12

            assert ip4Rules.find {it.priority == 1 && it.type == "Ingress" && it.state == "Enabled"}
            assert ip4Rules.find {it.priority == 3 && it.type == "Ingress" && it.state == "Enabled"}
            assert ip4Rules.find {it.priority == 5 && it.type == "Ingress" && it.state == "Enabled"}
        }

        cmd = null
        sg1 = changeSecurityGroupRuleState {
            securityGroupUuid = sg1.uuid
            ruleUuids = [rule1.uuid, rule3.uuid, rule5.uuid]
            state = "Disabled"
        }

        rule1 = sg1.rules.find {it.priority == 1 && it.type == "Ingress"}
        rule3 = sg1.rules.find {it.priority == 3 && it.type == "Ingress"}
        rule5 = sg1.rules.find {it.priority == 5 && it.type == "Ingress"}

        assert rule1.state == "Disabled"
        assert rule3.state == "Disabled"
        assert rule5.state == "Disabled"

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1.vmNics[0].uuid
            ip4Rules = cmd.ruleTOs.get(sg1.uuid)
            assert ip4Rules.size() == 9
            assert cmd.ip6RuleTOs.isEmpty()
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
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory // vm4 in host3
            sg1 = createSecurityGroup {
                name = "sg-1"
                ipVersion = 4
            } as SecurityGroupInventory

            sg2 = createSecurityGroup {
                name = "sg-2"
                ipVersion = 6
            } as SecurityGroupInventory

            sg3 = createSecurityGroup {
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testChangeMultipleRules()
    }
}
