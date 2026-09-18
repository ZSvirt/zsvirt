package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.RuleTO
import org.zstack.network.securitygroup.SecurityGroupMembersTO
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType
import org.zstack.network.securitygroup.VmNicSecurityTO
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/26.
 */
class IPv6SecurityGroupCase extends SubCase {
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
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testSecurityGroupValidator()
            testApplySecurityGroup()
            testDetachL3NetworkFromSecurityGroup()
            testSecurityGroupApplyNetworkServices()
            testChangeSecurityGroupRules()
            testDeleteSecurityGroup()
        }
    }

    void testSecurityGroupValidator() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        SecurityGroupInventory sg4 = createSecurityGroup {
            name = "SecurityGroup4"
            ipVersion = 4
        }

        SecurityGroupInventory sg6 = createSecurityGroup {
            name = "SecurityGroup6"
            ipVersion = 6
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule4 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule4.allowedCidr = "192.168.0.1/24"
        rule4.type = SecurityGroupRuleType.Ingress.toString()
        rule4.protocol = SecurityGroupRuleProtocolType.TCP.toString()
        rule4.startPort = 100
        rule4.endPort = 200

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule6 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule6.allowedCidr = "2002::/64"
        rule6.type = SecurityGroupRuleType.Ingress.toString()
        rule6.protocol = SecurityGroupRuleProtocolType.TCP.toString()
        rule6.startPort = 100
        rule6.endPort = 200
        rule6.ipVersion = 6

        //rule same as default sg rule
        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule7 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule7.type = SecurityGroupRuleType.Ingress.toString()
        rule7.protocol = SecurityGroupRuleProtocolType.ALL.toString()
        rule7.startPort = null
        rule7.endPort = null
        rule7.ipVersion = 6

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg4.uuid
            delegate.rules = [rule4]
        }

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg6.uuid
            delegate.rules = [rule6]
        }

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg6.uuid
            delegate.rules = [rule7]
        }

        expectError {
            addSecurityGroupRule {
                delegate.securityGroupUuid = sg6.uuid
                delegate.rules = [rule7]
            }
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg4.uuid
            l3NetworkUuid = l3_statefull.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg6.uuid
            l3NetworkUuid = l3_statefull.uuid
        }

        sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]

        SecurityGroupRuleInventory rule1 = sg6.rules.stream().filter{r -> r.protocol == rule7.protocol && r.ipVersion == rule7.ipVersion &&
                r.remoteSecurityGroupUuid == null && r.startPort == -1 && r.endPort == -1}.collect(Collectors.toList()).get(0)
        //delete rule7
        deleteSecurityGroupRule {
            ruleUuids = asList(rule1.uuid)
        }
    }

    void testApplySecurityGroup() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host = env.inventoryByName("kvm-1")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm-sg"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
            hostUuid = host.uuid
        }
        VmNicInventory nic = vm.getVmNics()[0]

        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions=["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert ip6RuleMap.get(sg4.uuid).size() == 2

            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd = null
        addVmNicToSecurityGroup {
            securityGroupUuid = sg6.uuid
            vmNicUuids = [nic.uuid]
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd = null
        changeSecurityGroupState {
            uuid = sg4.uuid
            stateEvent = "disable"
        }

        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert !ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert !ip6RuleMap.containsKey(sg4.uuid)
            assert ip6RuleMap.containsKey(sg6.uuid)
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        changeSecurityGroupState {
            uuid = sg4.uuid
            stateEvent = "enable"
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        changeSecurityGroupState {
            uuid = sg6.uuid
            stateEvent = "disable"
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert ruleMap.containsKey(sg4.uuid)
            assert !ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert ip6RuleMap.containsKey(sg4.uuid)
            assert !ip6RuleMap.containsKey(sg6.uuid)
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        changeSecurityGroupState {
            uuid = sg6.uuid
            stateEvent = "enable"
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }
    }

    void testDetachL3NetworkFromSecurityGroup() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")

        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions=["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]
        VmInstanceInventory vm = queryVmInstance {conditions=["name=vm-sg"]}[0]
        VmNicInventory nic = vm.getVmNics()[0]

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        detachSecurityGroupFromL3Network {
            l3NetworkUuid = l3_statefull.uuid
            securityGroupUuid = sg4.uuid
        }

        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert !ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert !ip6RuleMap.containsKey(sg4.uuid)
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg4.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }
        retryInSecs {
            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        detachSecurityGroupFromL3Network {
            l3NetworkUuid = l3_statefull.uuid
            securityGroupUuid = sg6.uuid
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert ruleMap.containsKey(sg4.uuid)
            assert !ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert !ip6RuleMap.containsKey(sg6.uuid)
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd == null
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg6.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg6.uuid
            vmNicUuids = [nic.uuid]
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }
    }

    void testSecurityGroupApplyNetworkServices() {
        HostInventory host = env.inventoryByName("kvm-1")

        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions = ["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions = ["name=SecurityGroup6"]
        }[0]

        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm-sg"]
        }[0]
        VmNicInventory nic = vm.getVmNics()[0]

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        KVMAgentCommands.RefreshAllRulesOnHostCmd rcmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) { rsp, HttpEntity<String> e ->
            rcmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.RefreshAllRulesOnHostCmd.class)
            return rsp
        }
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 2
            assert ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg4.uuid).size() == 3
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 2
            assert ip6RuleMap.get(sg4.uuid).size() == 2
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd = null
        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert !ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        rcmd = null
        reconnectHost {
            uuid = host.uuid
        }
        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert !ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert ip6RuleMap.get(sg6.uuid).size() == 3
            
            VmNicSecurityTO vmNicTo = cmd.vmNicTOs.get(0)
            assert vmNicTo.vmNicUuid == nic.uuid
            assert vmNicTo.actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }
    }

    void testChangeSecurityGroupRules() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm-sg"]
        }[0]
        VmNicInventory nic = vm.getVmNics()[0]
        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions=["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]

        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [nic.uuid]
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule4 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule4.allowedCidr = "192.168.0.1/24"
        rule4.type = SecurityGroupRuleType.Ingress.toString()
        rule4.protocol = SecurityGroupRuleProtocolType.ICMP.toString()
        rule4.startPort = -1
        rule4.endPort = -1

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule41 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule41.allowedCidr = "192.168.1.1/24"
        rule41.type = SecurityGroupRuleType.Ingress.toString()
        rule41.protocol = SecurityGroupRuleProtocolType.UDP.toString()
        rule41.startPort = 100
        rule41.endPort = 200

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg4.uuid
            delegate.rules = [rule4, rule41]
        }

        sg4 = querySecurityGroup {
            conditions=["name=SecurityGroup4"]
        }[0]
        sg4.rules.size() == 3
        List<SecurityGroupRuleInventory> rules = sg4.rules
        SecurityGroupRuleInventory rule1 = rules.stream().filter{r -> r.protocol == SecurityGroupRuleProtocolType.ICMP.toString()}.collect(Collectors.toList()).get(0)
        deleteSecurityGroupRule {
            ruleUuids = asList(rule1.uuid)
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule6 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule6.allowedCidr = "2002::/64"
        rule6.type = SecurityGroupRuleType.Egress.toString()
        rule6.protocol = SecurityGroupRuleProtocolType.ICMP.toString()
        rule6.ipVersion = 6
        rule6.startPort = -1
        rule6.endPort = -1

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule61 = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule61.allowedCidr = "2003::/64"
        rule61.type = SecurityGroupRuleType.Egress.toString()
        rule61.protocol = SecurityGroupRuleProtocolType.UDP.toString()
        rule61.startPort = 100
        rule61.endPort = 200
        rule61.ipVersion = 6

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg6.uuid
            delegate.rules = [rule6, rule61]
        }
        sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]
        sg6.rules.size() == 3
        rules = sg6.rules
        rule1 = rules.stream().filter{r -> r.protocol == SecurityGroupRuleProtocolType.TCP.toString()}.collect(Collectors.toList()).get(0)
        deleteSecurityGroupRule {
            ruleUuids = asList(rule1.uuid)
        }
    }

    void testDeleteSecurityGroup() {
        SecurityGroupInventory sg4 = querySecurityGroup {
            conditions = ["name=SecurityGroup4"]
        }[0]
        SecurityGroupInventory sg6 = querySecurityGroup {
            conditions=["name=SecurityGroup6"]
        }[0]

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        deleteSecurityGroup {
            uuid = sg4.uuid
        }

        retryInSecs {
            assert cmd != null

            Map<String, List<RuleTO>> ruleMap = cmd.ruleTOs
            assert ruleMap.size() == 1
            assert !ruleMap.containsKey(sg4.uuid)
            assert ruleMap.containsKey(sg6.uuid)
            assert ruleMap.get(sg6.uuid).size() == 2

            Map<String, List<RuleTO>> ip6RuleMap = cmd.ip6RuleTOs
            assert ip6RuleMap.size() == 1
            assert ip6RuleMap.get(sg6.uuid).size() == 4
        }
    }
}

