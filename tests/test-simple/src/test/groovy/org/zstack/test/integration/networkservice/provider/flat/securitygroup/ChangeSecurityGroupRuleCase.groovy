package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
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

class ChangeSecurityGroupRuleCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3, sg4

    void testChangeRulePriority() {
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

        SecurityGroupRuleInventory rule3 = sg1.rules.find { it.priority == 3 }
        SecurityGroupRuleInventory rule4 = sg1.rules.find { it.priority == 4 }
        SecurityGroupRuleInventory rule5 = sg1.rules.find { it.priority == 5 }
        SecurityGroupRuleInventory rule7 = sg1.rules.find { it.priority == 7 }
        SecurityGroupRuleInventory rule8 = sg1.rules.find { it.priority == 8 }
        SecurityGroupRuleInventory rule9 = sg1.rules.find { it.priority == 9 }
        SecurityGroupRuleInventory rule10 = sg1.rules.find { it.priority == 10 }

        changeSecurityGroupRule {
            uuid = rule3.uuid
            priority = 5
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule3.uuid).find().priority == 5
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule4.uuid).find().priority == 3
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule5.uuid).find().priority == 4

        changeSecurityGroupRule {
            uuid = rule3.uuid
            priority = 2
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule3.uuid).find().priority == 2
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule4.uuid).find().priority == 4
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule5.uuid).find().priority == 5

        changeSecurityGroupRule {
            uuid = rule7.uuid
            priority = 8
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule7.uuid).find().priority == 8
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule8.uuid).find().priority == 7

        changeSecurityGroupRule {
            uuid = rule7.uuid
            priority = 7
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule7.uuid).find().priority == 7
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule8.uuid).find().priority == 8

        changeSecurityGroupRule {
            uuid = rule9.uuid
            priority = -1
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule9.uuid).find().priority == 10
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule10.uuid).find().priority == 9
    }

    void testChangeRuleIpRange() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "testChangeRuleIpRange"
        r.ipVersion = 4
        r.dstPortRange = "100-200"
        r.dstIpRange = "1.1.1.0/24"
        r.protocol = "TCP"

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule = sg2.rules.find { it.description == "testChangeRuleIpRange" }

        changeSecurityGroupRule {
            uuid = rule.uuid
            dstIpRange = "1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24"
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().dstIpRange == "1.1.1.1-1.1.1.10,2.2.2.2,3.3.3.0/24"
    }

    void testChangeRuleRemoteGroup() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "testChangeRuleRemoteGroup"
        r.ipVersion = 4
        r.dstPortRange = "100-200"
        r.dstIpRange = "1.1.1.0/24"
        r.protocol = "TCP"

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule = sg2.rules.find { it.description == "testChangeRuleRemoteGroup" }

        changeSecurityGroupRule {
            uuid = rule.uuid
            remoteSecurityGroupUuid = sg3.uuid
        }

        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().remoteSecurityGroupUuid == sg3.uuid
        assert Q.New(SecurityGroupRuleVO).eq(SecurityGroupRuleVO_.uuid, rule.uuid).find().dstIpRange == null
    }

    void testChangeDefaultRule() {
        List<SecurityGroupRuleAO> ingressRules = new ArrayList<>()
        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = "100" + i
            r.srcIpRange = "172.17." + i + ".0/24"
            r.protocol = "TCP"
            ingressRules.add(r)
        }

        sg3 = addSecurityGroupRule {
            securityGroupUuid = sg3.uuid
            rules = ingressRules
        }

        SecurityGroupRuleInventory default_rule = sg3.rules.find { it.type == "Ingress" && it.priority == 0 && it.ipVersion == 4 }
        assert default_rule != null

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                priority = 2
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                srcIpRange = "1.1.1.0/24"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                dstPortRange = "1-100"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                remoteSecurityGroupUuid = sg1.uuid
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                protocol = "UDP"
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = default_rule.uuid
                action = "DROP"
            }
        }

        assert default_rule.state == 'Enabled'
        default_rule = changeSecurityGroupRule {
            uuid = default_rule.uuid
            state = 'Disabled'
        }

        assert default_rule.state == 'Disabled'

        default_rule = changeSecurityGroupRule {
            uuid = default_rule.uuid
            description = ''
        }

        assert default_rule.description == null
    }

    void testChangeRulePriorityError() {
        sg3 = querySecurityGroup {
            conditions = ["uuid=${sg3.uuid}"]
        }[0]

        assert sg3 != null
        SecurityGroupRuleInventory rule_1 = sg3.rules.find { it.type == "Ingress" && it.priority == 1 && it.ipVersion == 4 }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule_1.uuid
                priority = 6
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule_1.uuid
                priority = 7
            }
        }
    }

    void testChangeRuleDuplicate() {
        sg4 = createSecurityGroup {
            name = "sg-4"
            ipVersion = 4
        } as SecurityGroupInventory

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "egress-rule-1"
        r.ipVersion = 4
        r.dstPortRange = "100-200"
        r.dstIpRange = "1.1.1.1"
        r.protocol = "TCP"

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r]
        }

        SecurityGroupRuleAO r2 = new SecurityGroupRuleAO()
        r2.type = "Egress"
        r2.description = "egress-rule-2"
        r2.ipVersion = 4
        r2.dstPortRange = "100-200"
        r2.protocol = "TCP"

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r2]
        }

        SecurityGroupRuleInventory rule1 = sg4.rules.find { it.description == "egress-rule-1" }
        SecurityGroupRuleInventory rule2 = sg4.rules.find { it.description == "egress-rule-2" }

        assert rule1 != null
        assert rule2 != null

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                dstIpRange = ''
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                srcIpRange = '2.2.2.2'
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                dstIpRange = ''
                dstPortRange = ''
                protocol = 'ALL'
                remoteSecurityGroupUuid = sg4.uuid
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                priority = 3
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                priority = 0
            }
        }

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                dstIpRange = '2.2.2.2'
                remoteSecurityGroupUuid = sg4.uuid
            }
        }

        SecurityGroupRuleAO r3 = new SecurityGroupRuleAO()
        r3.type = "Ingress"
        r3.description = "ingress-rule-1"
        r3.ipVersion = 4
        r3.protocol = "TCP"
        r3.startPort = 20
        r3.endPort = 20

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r3]
        }

        SecurityGroupRuleAO r4 = new SecurityGroupRuleAO()
        r4.type = "Ingress"
        r4.description = "ingress-rule-2"
        r4.ipVersion = 4
        r4.protocol = "TCP"
        r4.dstPortRange = "20-21"

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r4]
        }

        SecurityGroupRuleInventory rule3 = sg4.rules.find { it.description == "ingress-rule-1" }
        SecurityGroupRuleInventory rule4 = sg4.rules.find { it.description == "ingress-rule-2" }

        assert rule3 != null
        assert rule4 != null

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule3.uuid
                dstPortRange = '20-21'
            }
        }
    }

    void testChangeRuleParamLimit() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Ingress'
        r.description = 'ingress-rule-1'
        r.ipVersion = 4
        r.dstPortRange = '300-400'
        r.srcIpRange = '3.3.3.3'
        r.protocol = 'TCP'

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule1 = sg4.rules.find { it.description == 'ingress-rule-1' }

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = sg4.uuid
        }

        assert rule1.srcIpRange == null
        assert rule1.remoteSecurityGroupUuid == sg4.uuid

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            srcIpRange = '4.4.4.4'
        }
        assert rule1.srcIpRange == '4.4.4.4'
        assert rule1.remoteSecurityGroupUuid == null

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            srcIpRange = ''
        }
        assert rule1.srcIpRange == null
        assert rule1.remoteSecurityGroupUuid == null

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = sg4.uuid
        }
        assert rule1.srcIpRange == null
        assert rule1.remoteSecurityGroupUuid == sg4.uuid

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = ''
        }

        assert rule1.srcIpRange == null
        assert rule1.remoteSecurityGroupUuid == null

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            dstPortRange = '400-500'
        }

        assert rule1.dstPortRange == '400-500'

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            protocol = 'UDP'
        }

        assert rule1.protocol == 'UDP'
        assert rule1.dstPortRange == '400-500'

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            protocol = 'ALL'
        }

        assert rule1.protocol == 'ALL'
        assert rule1.dstPortRange == null

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            protocol = 'ICMP'
        }

        assert rule1.protocol == 'ICMP'
        assert rule1.dstPortRange == null

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            protocol = 'TCP'
            dstPortRange = '500-600'
        }

        assert rule1.protocol == 'TCP'
        assert rule1.dstPortRange == '500-600'

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            srcIpRange = '5.5.5.5'
            protocol = 'UDP'
            dstPortRange = '700-800'
            action = 'DROP'
            state = 'Disabled'
            description = 'change description'
        }

        assert rule1.srcIpRange == '5.5.5.5'
        assert rule1.protocol == 'UDP'
        assert rule1.dstPortRange == '700-800'
        assert rule1.action == 'DROP'
        assert rule1.state == 'Disabled'
        assert rule1.description == 'change description'

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = sg4.uuid
            protocol = 'ICMP'
            action = 'ACCEPT'
            state = 'Enabled'
            description = ''
        }

        assert rule1.srcIpRange == null
        assert rule1.remoteSecurityGroupUuid == sg4.uuid
        assert rule1.protocol == 'ICMP'
        assert rule1.dstPortRange == null
        assert rule1.action == 'ACCEPT'
        assert rule1.state == 'Enabled'
        assert rule1.description == null

        expect(AssertionError) {
            changeSecurityGroupRule {
                uuid = rule1.uuid
                protocol = 'UDP'
            }
        }
    }

    void testChangeRuleWithOldRules() {
        deleteSecurityGroup {
            uuid = sg4.uuid
        }

        sg4 = createSecurityGroup {
            name = "sg-4"
            ipVersion = 4
        } as SecurityGroupInventory

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Ingress'
        r.description = 'testChangeRuleWithOldRules'
        r.ipVersion = 4
        r.dstPortRange = '500-600'
        r.srcIpRange = '5.5.5.5'
        r.protocol = 'UDP'

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule1 = sg4.rules.find { it.description == 'testChangeRuleWithOldRules' }

        SQL.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg4.uuid)
                .eq(SecurityGroupRuleVO_.uuid, rule1.uuid)
                .set(SecurityGroupRuleVO_.remoteSecurityGroupUuid, sg3.uuid)
                .update()

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            protocol = 'TCP'
            dstPortRange = '700-800'
            action = 'DROP'
            state = 'Disabled'
        }

        assert rule1.protocol == 'TCP'
        assert rule1.dstPortRange == '700-800'
        assert rule1.action == 'DROP'
        assert rule1.state == 'Disabled'
        assert rule1.remoteSecurityGroupUuid == sg3.uuid
        assert rule1.srcIpRange == '5.5.5.5'

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = sg2.uuid
            protocol = 'ICMP'
            action = 'ACCEPT'
            state = 'Enabled'
            description = ''
        }

        assert rule1.protocol == 'ICMP'
        assert rule1.dstPortRange == null
        assert rule1.action == 'ACCEPT'
        assert rule1.state == 'Enabled'
        assert rule1.description == null
        assert rule1.remoteSecurityGroupUuid == sg2.uuid
        assert rule1.srcIpRange == null

        SQL.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg4.uuid)
                .eq(SecurityGroupRuleVO_.uuid, rule1.uuid)
                .set(SecurityGroupRuleVO_.srcIpRange, '6.6.6.6')
                .update()

        rule1 = changeSecurityGroupRule {
            uuid = rule1.uuid
            remoteSecurityGroupUuid = ''
        }

        assert rule1.protocol == 'ICMP'
        assert rule1.dstPortRange == null
        assert rule1.action == 'ACCEPT'
        assert rule1.state == 'Enabled'
        assert rule1.description == null
        assert rule1.remoteSecurityGroupUuid == null
        assert rule1.srcIpRange == '6.6.6.6'


        SecurityGroupRuleAO r2 = new SecurityGroupRuleAO()
        r2.type = 'Ingress'
        r2.description = 'testChangeRuleWithAutoModify'
        r2.ipVersion = 4
        r2.allowedCidr = '8.8.8.8'
        r2.protocol = 'UDP'
        r2.startPort = 200
        r2.endPort = 300

        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r2]
        }

        SecurityGroupRuleInventory rule2 = sg4.rules.find { it.description == 'testChangeRuleWithAutoModify' }

        assert rule2.srcIpRange == '8.8.8.8'
        assert rule2.allowedCidr == '8.8.8.8'
        assert rule2.startPort == 200
        assert rule2.endPort == 300
        assert rule2.dstPortRange == '200-300'

        rule2 = changeSecurityGroupRule {
            uuid = rule2.uuid
            srcIpRange = '9.9.9.9'
        }

        assert rule2.srcIpRange == '9.9.9.9'
        assert rule2.allowedCidr == '0.0.0.0/0'
        assert rule2.startPort == -1
        assert rule2.endPort == -1
        assert rule2.dstPortRange == '200-300'

        rule2 = changeSecurityGroupRule {
            uuid = rule2.uuid
            dstPortRange = '400-500'
        }

        assert rule2.srcIpRange == '9.9.9.9'
        assert rule2.allowedCidr == '0.0.0.0/0'
        assert rule2.startPort == -1
        assert rule2.endPort == -1
        assert rule2.dstPortRange == '400-500'
    }

    void testStripSpaceTabNewLineFromRule() {
        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Egress"
        r.description = "testStripSpaceTabNewLine"
        r.ipVersion = 4
        r.dstPortRange = "433, 8888"
        r.dstIpRange = "1.1.1.1, 2.2.2.2, 3.3.3.0/24"
        r.protocol = "TCP"

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        SecurityGroupRuleInventory rule = sg2.rules.find { it.description == "testStripSpaceTabNewLine" }

        SQL.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .eq(SecurityGroupRuleVO_.uuid, rule.uuid)
                .set(SecurityGroupRuleVO_.dstPortRange, "433, \n \t8888\n")
                .set(SecurityGroupRuleVO_.dstIpRange, "\t1.1.1.1, \n2.2.2.2, \t \n3.3.3.0/24 \n")
                .update()

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        addVmNicToSecurityGroup{
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm2.vmNics.get(0).uuid]
        }

        Boolean find = false
        retryInSecs(100) {
            assert cmd != null : "ApplySecurityGroupRuleCmd not received"
            for (ruleTOLists in cmd.ruleTOs.values()) {
                for (ruleTO in ruleTOLists) {
                    if (ruleTO.dstIpRange == "1.1.1.1,2.2.2.2,3.3.3.0/24" && ruleTO.dstPortRange == "433,8888" ) {
                        find = true
                        break
                    }
                }
            }
            assert find : "RuleTOs not found with expected dstIpRange and dstPortRange"
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

        testChangeRulePriority()
        testChangeRuleIpRange()
        testChangeRuleRemoteGroup()
        testChangeDefaultRule()
        testChangeRulePriorityError()
        testChangeRuleDuplicate()
        testChangeRuleParamLimit()
        testChangeRuleWithOldRules()
        testStripSpaceTabNewLineFromRule()
    }
}
