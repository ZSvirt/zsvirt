package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.SecurityGroupErrors
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ValidateSecurityGroupRuleAction
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ValidateSecurityGroupRuleCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testValidateErrorRuleCode() {
        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "TCP"
            action.srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            action.dstPortRange = "1-100"
            action.allowedCidr = "1.1.1.0/24"
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR.toString()
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "TCP"
            action.srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            action.dstIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            action.dstPortRange = "1-100"
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR.toString()
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Egress"
            action.protocol = "TCP"
            action.srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            action.dstPortRange = "1-100"
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_FILED_NOT_SUPPORT_ERROR.toString()
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "ALL"
            action.srcIpRange = "1.1.1.1-1.1.1.10,2.2.2.0/24"
            action.remoteSecurityGroupUuid = sg2.uuid
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR.toString()
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "TCP"
            action.allowedCidr = "192.168.1.0/24"
            action.startPort = 400
            action.endPort = 300
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_PORT_FIELD_ERROR.toString()
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "TCP"
            action.allowedCidr = "333.168.1.0/24"
            action.startPort = 500
            action.endPort = 600
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_IP_FIELD_ERROR.toString()
        }

        validateSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Ingress"
            protocol = "TCP"
            allowedCidr = "3.168.1.100, 33.168.1.0/24\n"
            startPort = 500
            endPort = 600
        }

        validateSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Ingress"
            protocol = "TCP"
            srcIpRange = "3.168.1.0/24,\n33.168.1.0/24"
            startPort = 500
            endPort = 600
        }

        validateSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Egress"
            protocol = "TCP"
            dstIpRange = "3.168.1.0/24,\n33.168.1.0/24"
            startPort = 500
            endPort = 600
        }

        validateSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            type = "Egress"
            protocol = "TCP"
            allowedCidr = "3.168.1.0/24,\n33.168.1.0/24"
            dstPortRange = "111-500\t\n"
        }

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = "Ingress"
        r.ipVersion = 4
        r.startPort = 1000
        r.endPort = 2000
        r.srcIpRange = "172.16.90.157"
        r.protocol = "TCP"
        sg1 = addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [r]
        }

        try {
            ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
            action.securityGroupUuid = sg1.uuid
            action.type = "Ingress"
            action.protocol = "TCP"
            action.srcIpRange = "172.16.90.157"
            action.dstPortRange = "1000-2000"
            action.sessionId = adminSession()
        } catch (ApiMessageInterceptionException e) {
            assert e.code == SecurityGroupErrors.RULE_DUPLICATE_ERROR.toString()
        }

        ValidateSecurityGroupRuleAction action = new ValidateSecurityGroupRuleAction()
        action.securityGroupUuid = sg1.uuid
        action.type = "Ingress"
        action.protocol = "TCP"
        action.srcIpRange = "172.16.90.157"
        action.dstPortRange = "1000-2000,3000-4000"
        action.sessionId = adminSession()

        ValidateSecurityGroupRuleAction.Result result = action.call()
        assert result.value.available == true
        assert result.value.code == 'SG.2000'
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

        testValidateErrorRuleCode()
    }
}
