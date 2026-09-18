package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.sdk.AddSecurityGroupRuleAction
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.header.errorcode.ErrorCode

import java.util.concurrent.atomic.AtomicInteger

class AddSecurityGroupRuleConcurrencyCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testBatchAddSecurityGroupRule() {
        for (int i = 1; i <= 100; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.dstPortRange = i.toString()
            r.protocol = "TCP"

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
                priority = 1
            }
        }

        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY).list()

        assert rvos.size() == 100
        for (int i = 1; i <= 100; i++) {
            SecurityGroupRuleVO vo = rvos.find {it.priority == i}
            assert vo != null
        }

        AtomicInteger count = new AtomicInteger(0)
        def threads = []
        for (int i = 0; i < 50; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.dstPortRange = i.toString()
            r.protocol = "TCP"

            def thread = Thread.start {
                AddSecurityGroupRuleAction action = new AddSecurityGroupRuleAction(
                    securityGroupUuid: sg1.uuid,
                    rules: [r],
                    priority: 1,
                    sessionId: adminSession(),
                )

                action.call()
                count.incrementAndGet()
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs(50, 3) {
            assert count.get() == 50
        }

        List<SecurityGroupRuleVO> rvos_2 = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Egress).notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY).list()

        assert rvos_2.size() == 50
        for (int i = 1; i <= 50; i++) {
            SecurityGroupRuleVO vo = rvos_2.find {it.priority == i}
            assert vo != null
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

        testBatchAddSecurityGroupRule()
    }
}
