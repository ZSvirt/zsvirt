package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class ChangeVmNicSecurityPolicyCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testChangeVmNicSecurityPolicy() {
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        String vm1_nic_uuid = vm1.vmNics[0].uuid

        VmNicSecurityPolicyInventory policy = queryVmNicSecurityPolicy {
            conditions = ["vmNicUuid=${vm1_nic_uuid}"]
        }[0]

        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"

        changeVmNicSecurityPolicy {
            vmNicUuid = vm1_nic_uuid
            ingressPolicy = "ALLOW"
            egressPolicy = "DENY"
        }

        policy = queryVmNicSecurityPolicy {
            conditions = ["vmNicUuid=${vm1_nic_uuid}"]
        }[0]

        assert policy.ingressPolicy == "ALLOW"
        assert policy.egressPolicy == "DENY"

        changeVmNicSecurityPolicy {
            vmNicUuid = vm1_nic_uuid
            ingressPolicy = "DENY"
        }

        policy = queryVmNicSecurityPolicy {
            conditions = ["vmNicUuid=${vm1_nic_uuid}"]
        }[0]

        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "DENY"

        changeVmNicSecurityPolicy {
            vmNicUuid = vm1_nic_uuid
            egressPolicy = "ALLOW"
        }

        policy = queryVmNicSecurityPolicy {
            conditions = ["vmNicUuid=${vm1_nic_uuid}"]
        }[0]

        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"

        changeVmNicSecurityPolicy {
            vmNicUuid = vm1_nic_uuid
            ingressPolicy == "DENY"
            egressPolicy = "ALLOW"
        }

        assert policy.ingressPolicy == "DENY"
        assert policy.egressPolicy == "ALLOW"
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

        testChangeVmNicSecurityPolicy()

    }
}
