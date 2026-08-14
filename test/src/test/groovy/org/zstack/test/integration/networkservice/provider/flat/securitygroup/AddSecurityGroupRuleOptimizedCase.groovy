package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupRuleType
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

class AddSecurityGroupRuleOptimizedCase extends SubCase {

    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3, sg4

    void testAddSecurityGroupDuplicate() {
        SecurityGroupRuleAO rule1 = new SecurityGroupRuleAO()
        rule1.allowedCidr = "192.167.1.0/24"
        rule1.type = "Ingress"
        rule1.protocol = "UDP"
        rule1.startPort = 200
        rule1.endPort = 300
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [rule1]
        }

        SecurityGroupRuleAO ruleDup_1 = new SecurityGroupRuleAO()
        ruleDup_1.allowedCidr = "192.167.1.0/24"
        ruleDup_1.type = "Ingress"
        ruleDup_1.protocol = "UDP"
        ruleDup_1.startPort = 200
        ruleDup_1.endPort = 300

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg4.uuid
                rules = [ruleDup_1]
            }
        }

        SecurityGroupRuleAO ruleDup_2 = new SecurityGroupRuleAO()
        ruleDup_2.allowedCidr = '192.167.1.0/24'
        ruleDup_2.type = 'Ingress'
        ruleDup_2.protocol = 'UDP'
        ruleDup_2.dstPortRange = '200-300'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg4.uuid
                rules = [ruleDup_2]
            }
        }

        SecurityGroupRuleAO rule2 = new SecurityGroupRuleAO()
        rule2.allowedCidr = "192.169.1.0/24"
        rule2.type = "Ingress"
        rule2.protocol = "ICMP"
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [rule2]
        }

        SecurityGroupRuleAO ruleDup_3 = new SecurityGroupRuleAO()
        ruleDup_3.allowedCidr = "192.169.1.0/24"
        ruleDup_3.type = "Ingress"
        ruleDup_3.protocol = "ICMP"

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg4.uuid
                rules = [ruleDup_3]
            }
        }

        SecurityGroupRuleAO ruleDup_4 = new SecurityGroupRuleAO()
        ruleDup_4.srcIpRange = '192.169.1.0/24'
        ruleDup_4.type = 'Ingress'
        ruleDup_4.protocol = 'ICMP'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg4.uuid
                rules = [ruleDup_4]
            }
        }

        SecurityGroupRuleAO rule3 = new SecurityGroupRuleAO()
        rule3.allowedCidr = "192.170.1.0/24"
        rule3.type = "Ingress"
        rule3.protocol = "ICMP"
        rule3.startPort = 20
        rule3.startPort = 30

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg4.uuid
                rules = [rule3]
            }
        }
    }

    void testAddSecurityGroupRuleError() {
        SecurityGroupRuleAO errorRule = new SecurityGroupRuleAO()
        errorRule.allowedCidr = "192.168.1.0/24"
        errorRule.srcIpRange = "1.1.1.1-1.1.1.10"
        errorRule.type = "Ingress"
        errorRule.protocol = "ALL"
        errorRule.startPort = -1
        errorRule.endPort = -1
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.dstIpRange = "2.2.2.2-2.2.2.20"
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg2.uuid
                rules = [errorRule]
            }
        }

        errorRule.srcIpRange = null
        errorRule.dstIpRange = null
        errorRule.remoteSecurityGroupUuid = sg2.uuid
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.remoteSecurityGroupUuid = null
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
                remoteSecurityGroupUuids = [sg2.uuid]
            }
        }

        errorRule.allowedCidr = null
        errorRule.remoteSecurityGroupUuid = sg3.uuid
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
                remoteSecurityGroupUuids = [sg2.uuid]
            }
        }

        errorRule.remoteSecurityGroupUuid = null
        errorRule.protocol = "TCP"
        errorRule.startPort = null
        errorRule.endPort = null
        errorRule.dstPortRange = "1,2,3,4,5,6-7,8-10,11-12"
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.dstPortRange = "1,2-2,3-4"
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.dstPortRange = null
        errorRule.protocol = "ALL"
        errorRule.ipVersion = 4
        errorRule.type = "Egress"
        errorRule.dstIpRange = '2023:1:1:1::/64'
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.ipVersion = 6
        errorRule.dstIpRange = '2001:db8:2de::e13,2001:db8:2de::e14-2001:db8:2de::e15,2001:db8:2de::e16-2001:db8:2de::e17'
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.ipVersion = 6
        errorRule.dstIpRange = '172.16.90.157'
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.ipVersion = 4
        errorRule.dstIpRange = '1.1.1.1-1.1.1.10,,2.2.2.2,3.3.3.0/24'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.ipVersion = 4
        errorRule.dstIpRange = '1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24,'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.ipVersion = 4
        errorRule.dstIpRange = '1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24,,'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }
        
        errorRule.ipVersion = 4
        errorRule.dstIpRange = ',1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24'

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }

        errorRule.protocol = 'TCP'
        errorRule.dstIpRange = '1.1.1.1'
        errorRule.dstPortRange = '100-200,300,400-500,'
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [errorRule]
            }
        }
    }

    void testAddSecurityGroupRule() {
        SecurityGroupRuleAO rule1 = new SecurityGroupRuleAO()
        rule1.allowedCidr = "192.168.1.0/24"
        rule1.type = "Ingress"
        rule1.protocol = "TCP"
        rule1.startPort = 200
        rule1.endPort = 300
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule1]
        }

        SecurityGroupRuleInventory ruleInv = sg1.rules.find { it.allowedCidr == rule1.allowedCidr && it.type == rule1.type && it.protocol == rule1.protocol && it.startPort == rule1.startPort && it.endPort == rule1.endPort }
        assert ruleInv != null
        assert ruleInv.srcIpRange == ruleInv.allowedCidr
        assert ruleInv.dstIpRange == null
        assert ruleInv.dstPortRange == ruleInv.startPort.toString() + "-" + ruleInv.endPort.toString()
        assert ruleInv.state == "Enabled"
        assert ruleInv.action == "ACCEPT"
        assert ruleInv.priority == 1 // ingress first rule

        SecurityGroupRuleAO rule2 = new SecurityGroupRuleAO()
        rule2.srcIpRange = "2.2.2.2,2.2.2.10-2.2.2.100,2.2.200.0/24"
        rule2.type = "Ingress"
        rule2.protocol = "UDP"
        rule2.dstPortRange = "400-500"
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule2]
        }

        ruleInv = sg1.rules.find {it.srcIpRange = rule2.srcIpRange && it.type == rule2.type && it.protocol == rule2.protocol && it.dstPortRange == rule2.dstPortRange}
        assert ruleInv != null
        assert ruleInv.allowedCidr == "0.0.0.0/0"
        assert ruleInv.startPort == -1
        assert ruleInv.endPort == -1
        assert ruleInv.state == "Enabled"
        assert ruleInv.action == "ACCEPT"
        assert ruleInv.priority == 2 // ingress second rule

        SecurityGroupRuleAO rule3 = new SecurityGroupRuleAO()
        rule3.type = "Egress"
        rule3.protocol = "ICMP"
        rule3.remoteSecurityGroupUuid = sg2.uuid
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule3]
        }

        ruleInv = sg1.rules.find {it.type == rule3.type && it.protocol == rule3.protocol && it.remoteSecurityGroupUuid == rule3.remoteSecurityGroupUuid}
        assert ruleInv != null
        assert ruleInv.allowedCidr == "0.0.0.0/0"
        assert ruleInv.startPort == -1
        assert ruleInv.endPort == -1
        assert ruleInv.priority == 1 // egress first rule

        SecurityGroupRuleAO rule4 = new SecurityGroupRuleAO()
        rule4.type = "Egress"
        rule4.protocol = "TCP"
        rule4.dstIpRange = "4.4.4.4,4.4.100.0/24"
        rule4.dstPortRange = "600-700"
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule4]
        }

        ruleInv = sg1.rules.find {it.type == rule4.type && it.protocol == rule4.protocol && it.dstIpRange == rule4.dstIpRange && it.dstPortRange == rule4.dstPortRange}
        assert ruleInv != null
        assert ruleInv.allowedCidr == "0.0.0.0/0"
        assert ruleInv.startPort == -1
        assert ruleInv.endPort == -1
        assert ruleInv.priority == 2 // egress second rule

        SecurityGroupRuleAO rule6 = new SecurityGroupRuleAO()
        rule6.allowedCidr = "6.6.6.0/24"
        rule6.type = "Egress"
        rule6.protocol = "TCP"
        rule6.startPort = 600
        rule6.endPort = 600
        rule6.action = "DROP"
        rule6.state = "Disabled"
        rule6.ipVersion = 4
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [rule6]
        }

        ruleInv = sg1.rules.find { it.allowedCidr == rule6.allowedCidr && it.type == rule6.type && it.protocol == rule6.protocol && it.startPort == rule6.startPort && it.endPort == rule6.endPort && it.action == rule6.action && it.state == rule6.state }
        assert ruleInv != null
        assert ruleInv.dstIpRange == ruleInv.allowedCidr
        assert ruleInv.srcIpRange == null
        assert ruleInv.dstPortRange == ruleInv.startPort.toString()
        assert ruleInv.priority == 3 // egress 3 rule
    }

    void testAddRuleAssignPriority() {
        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.srcIpRange = "172.16." + i + ".0/24"
            r.protocol = "TCP"
            ingressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = ingressRules
        }

        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.securityGroupUuid, sg3.uuid).eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).list()
        rvo3 = rvos.find { it.priority == 3 }
        rvo4 = rvos.find { it.priority == 4 }
        rvo5 = rvos.find { it.priority == 5 }
        assert rvo3 != null
        assert rvo4 != null
        assert rvo5 != null


        List<SecurityGroupRuleAO> newRules = new ArrayList<>()
        for (int i = 1; i <= 3; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "200" + i
            r.srcIpRange = "172.16." + i + ".0/24"
            r.protocol = "TCP"
            newRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = newRules
            priority = 3
        }

        rvos = Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.securityGroupUuid, sg3.uuid).eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).list()
        rvos.each {
            if (it.uuid == rvo3.uuid) {
                assert it.priority == 6
            } else if (it.uuid == rvo4.uuid) {
                assert it.priority == 7
            } else if (it.uuid == rvo5.uuid) {
                assert it.priority == 8
            }
        }

    }

    void testDeleteRules() {
        List<SecurityGroupRuleAO> egressRules = new ArrayList<>()
        for (int i = 1; i <= 9; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.protocol = "TCP"
            egressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = egressRules
        }

        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.securityGroupUuid, sg3.uuid).eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Egress).list()
        SecurityGroupRuleVO rvo3 = rvos.find { it.priority == 3 }
        SecurityGroupRuleVO rvo4 = rvos.find { it.priority == 4 }
        SecurityGroupRuleVO rvo5 = rvos.find { it.priority == 5 }
        SecurityGroupRuleVO rvo6 = rvos.find { it.priority == 6 }
        SecurityGroupRuleVO rvo7 = rvos.find { it.priority == 7 }
        SecurityGroupRuleVO rvo8 = rvos.find { it.priority == 8 }
        SecurityGroupRuleVO rvo9 = rvos.find { it.priority == 9 }

        deleteSecurityGroupRule {
            ruleUuids = [rvo3.uuid, rvo5.uuid, rvo7.uuid]
        }

        rvos = Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.securityGroupUuid, sg3.uuid).eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Egress).list()

        assert rvos.find { it.uuid == rvo3.uuid } == null
        assert rvos.find { it.uuid == rvo5.uuid } == null
        assert rvos.find { it.uuid == rvo7.uuid } == null
        assert rvos.find { it.uuid == rvo4.uuid && it.priority == 3 } != null
        assert rvos.find { it.uuid == rvo6.uuid && it.priority == 4 } != null
        assert rvos.find { it.uuid == rvo8.uuid && it.priority == 5 } != null
        assert rvos.find { it.uuid == rvo9.uuid && it.priority == 6 } != null
    }

    void testAddRuleExceedLimit() {
        deleteSecurityGroup {
            uuid = sg3.uuid
        }

        sg3 = createSecurityGroup {
            name = "sg-3"
            ipVersion = 4
        }

        List<SecurityGroupRuleAO> egressRules = new ArrayList<>()
        for (int i = 1; i <= 100; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.dstPortRange = i
            r.protocol = "TCP"
            egressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = egressRules
        }

        SecurityGroupRuleAO rule_101 = new SecurityGroupRuleAO()
        rule_101.allowedCidr = "192.167.1.0/24"
        rule_101.type = "Engress"
        rule_101.protocol = "ALL"
        rule_101.startPort = -1
        rule_101.endPort = -1

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = [rule_101]
            }
        }

        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 80; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = i
            r.protocol = "TCP"
            ingressRules.add(r)
        }
        addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules =ingressRules
        }

        List<SecurityGroupRuleAO> rules_exceed = new ArrayList<>()
        for (int i = 1; i <= 21; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = 80 + i
            r.protocol = "TCP"
            rules_exceed.add(r)
        }

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = rules_exceed
            }
        }

        SecurityGroupRuleAO rule_82 = new SecurityGroupRuleAO()
        rule_82.type = "Ingress"
        rule_82.protocol = "TCP"
        rule_82.dstPortRange = 82

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = [rule_82]
                priority = 82
            }
        }
    }

    void testAddRuleDiscontinuously() {
        deleteSecurityGroup {
            uuid = sg3.uuid
        }

        sg3 = createSecurityGroup {
            name = "sg-3"
            ipVersion = 4
        }

        List<SecurityGroupRuleAO> egressRules = new ArrayList<>()
        for (int i = 1; i <= 10; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.dstPortRange = i
            r.protocol = "TCP"
            egressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = egressRules
        }

        SecurityGroupRuleAO rule_11 = new SecurityGroupRuleAO()
        rule_11.allowedCidr = "192.167.1.0/24"
        rule_11.type = "Egress"
        rule_11.protocol = "ALL"
        rule_11.startPort = -1
        rule_11.endPort = -1

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = [rule_11]
            priority = 11
        }

        assert sg3.rules.find { it.allowedCidr == rule_11.allowedCidr && it.type == rule_11.type && it.protocol == rule_11.protocol && it.startPort == rule_11.startPort && it.endPort == rule_11.endPort && it.priority == 11 } != null

        SecurityGroupRuleAO rule_13 = new SecurityGroupRuleAO()
        rule_13.allowedCidr = "192.167.3.0/24"
        rule_13.type = "Egress"
        rule_13.protocol = "ALL"
        rule_13.startPort = -1
        rule_13.endPort = -1
        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = [rule_13]
                priority = 13
            }
        }

        SecurityGroupRuleAO rule_12 = new SecurityGroupRuleAO()
        rule_12.dstIpRange = "2.2.2.2-2.2.2.10"
        rule_12.type = "Egress"
        rule_12.protocol = "ALL"

        SecurityGroupRuleAO ingressRule = new SecurityGroupRuleAO()
        ingressRule.type = "Ingress"
        ingressRule.protocol = "TCP"
        ingressRule.dstPortRange = "12-13"

        expect(AssertionError) {
            addSecurityGroupRule {
                securityGroupUuid = sg3.uuid
                rules = [rule_12, ingressRule]
                priority = 12
            }
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
                ipVersion = 4
            } as SecurityGroupInventory

            sg3 = createSecurityGroup {
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory

            sg4 = createSecurityGroup {
                name = "sg-4"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testAddSecurityGroupRule()
        testDeleteRules()
        testAddRuleExceedLimit()
        testAddRuleDiscontinuously()
        testAddSecurityGroupRuleError()
        testAddSecurityGroupDuplicate()
    }
}
