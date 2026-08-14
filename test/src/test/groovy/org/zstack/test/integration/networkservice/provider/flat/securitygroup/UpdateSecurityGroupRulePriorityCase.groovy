package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.APIUpdateSecurityGroupRulePriorityMsg.SecurityGroupRulePriorityAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.sdk.ApiException
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class UpdateSecurityGroupRulePriorityCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testUpdateRulesPriorityError() {
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            sg1 = addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
                priority = -1
            }
        }

        List<SecurityGroupRuleInventory> ruleInvs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleInventory rule = sg1.rules.find {it.priority == i && it.type == "Ingress"}
            assert rule != null
            ruleInvs.add(rule)
        }
        
        SecurityGroupRulePriorityAO ao = new SecurityGroupRulePriorityAO();
        ao.ruleUuid = ruleInvs.get(0).uuid
        ao.priority = 1
        expect(AssertionError) {
            updateSecurityGroupRulePriority {
                securityGroupUuid = sg1.uuid
                type = "Ingress"
                rules = [ao]
            }
        }

        List<SecurityGroupRulePriorityAO> aos = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            SecurityGroupRulePriorityAO ao1 = new SecurityGroupRulePriorityAO();
            ao1.ruleUuid = ruleInvs.get(i).uuid
            ao1.priority = 1
            aos.add(ao1)
        }

        expect(AssertionError) {
            updateSecurityGroupRulePriority {
                securityGroupUuid = sg1.uuid
                type = "Ingress"
                rules = aos
            }
        }

        List<SecurityGroupRulePriorityAO> aos_2 = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            SecurityGroupRulePriorityAO ao2 = new SecurityGroupRulePriorityAO();
            ao2.ruleUuid = ruleInvs.get(1).uuid
            ao2.priority = 5 - i
            aos_2.add(ao2)
        }

         expect(AssertionError) {
            updateSecurityGroupRulePriority {
                securityGroupUuid = sg1.uuid
                type = "Ingress"
                rules = aos_2
            }
        }

    }

    void testUpdateSignalRulePriority() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Ingress"
        r.ipVersion = 4
        r.protocol = "TCP"
        r.startPort = 1
        r.endPort = 1

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
            priority = -1
        }

        SecurityGroupRuleInventory rule = sg2.rules.find {it.priority == 1 && it.type == "Ingress"}
        assert rule != null

        SecurityGroupRulePriorityAO ao = new SecurityGroupRulePriorityAO();
        ao.ruleUuid = rule.uuid
        ao.priority = 1
        sg2 = updateSecurityGroupRulePriority {
            securityGroupUuid = sg2.uuid
            type = "Ingress"
            rules = [ao]
        }

        assert sg2.rules.find {it.uuid == rule.uuid && it.priority == 1}
    }

    void testUpdateMultipleRulesPriority() {
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            sg3 = addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = [r]
                priority = -1
            }
        }

        List<SecurityGroupRuleInventory> ruleInvs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleInventory rule = sg3.rules.find {it.priority == i && it.type == "Egress"}
            assert rule != null
            ruleInvs.add(rule)
        }

        List<SecurityGroupRulePriorityAO> aos = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            SecurityGroupRulePriorityAO ao1 = new SecurityGroupRulePriorityAO();
            ao1.ruleUuid = ruleInvs.get(i).uuid
            ao1.priority = 5 - i
            aos.add(ao1)
        }

        sg3 = updateSecurityGroupRulePriority {
            securityGroupUuid = sg3.uuid
            type = "Egress"
            rules = aos
        }

        assert sg3.rules.find {it.uuid == ruleInvs.get(0).uuid && it.priority == 5}
        assert sg3.rules.find {it.uuid == ruleInvs.get(1).uuid && it.priority == 4}
        assert sg3.rules.find {it.uuid == ruleInvs.get(2).uuid && it.priority == 3}
        assert sg3.rules.find {it.uuid == ruleInvs.get(3).uuid && it.priority == 2}
        assert sg3.rules.find {it.uuid == ruleInvs.get(4).uuid && it.priority == 1}
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

        testUpdateRulesPriorityError()
        testUpdateSignalRulePriority()
        testUpdateMultipleRulesPriority()
    }
}
