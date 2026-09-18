package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.SecurityGroupMembersTO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.SetVmNicSecurityGroupAction
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class CreateSystemtagWithSecurityGroupCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net, l3Net_2
    InstanceOfferingInventory offer
    ImageInventory image
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testCreateVmWithSystemtag() {
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        vm1 = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l3Net.uuid]
            instanceOfferingUuid  = offer.uuid
            systemTags = [String.format("l3::%s::SecurityGroupUuids::%s", l3Net.uuid, sg1.uuid)]
        } as VmInstanceInventory

        List<VmNicSecurityGroupRefVO> refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refvos.size() == 1
        assert refvos.get(0).securityGroupUuid == sg1.uuid
        assert refvos.get(0).priority == 1

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm1.vmNics[0].uuid
        }

        cmd = null

        vm2 = createVmInstance {
            name = "vm2"
            imageUuid = image.uuid
            l3NetworkUuids = [l3Net.uuid]
            instanceOfferingUuid  = offer.uuid
            systemTags = [String.format("l3::%s::SecurityGroupUuids::%s,%s", l3Net.uuid, sg1.uuid, sg2.uuid)]
        } as VmInstanceInventory

        refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refvos.size() == 2
        assert refvos.find { it.securityGroupUuid == sg1.uuid }.priority == 1
        assert refvos.find { it.securityGroupUuid == sg2.uuid }.priority == 2

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm2.vmNics[0].uuid
        }

        cmd = null

        createSystemTag {
            resourceType = VmInstanceVO.class.simpleName
            resourceUuid = vm2.uuid
            tag = String.format("l3::%s::SecurityGroupUuids::%s", l3Net.uuid, sg3.uuid)
        }

        rebootVmInstance {
            uuid = vm2.uuid
        }

        refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refvos.size() == 3
        assert refvos.find { it.securityGroupUuid == sg1.uuid }.priority == 1
        assert refvos.find { it.securityGroupUuid == sg2.uuid }.priority == 2
        assert refvos.find { it.securityGroupUuid == sg3.uuid }.priority == 3

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm2.vmNics[0].uuid
        }
    }

    void testAttachNicWithSystemtag() {
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        vm3 = createVmInstance {
            name = "vm3"
            imageUuid = image.uuid
            l3NetworkUuids = [l3Net.uuid]
            instanceOfferingUuid  = offer.uuid
            systemTags = [String.format("l3::%s::SecurityGroupUuids::%s", l3Net.uuid, sg3.uuid)]
        } as VmInstanceInventory

        List<VmNicSecurityGroupRefVO> refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm3.vmNics[0].uuid).list()
        assert refvos.size() == 1
        assert refvos.find { it.securityGroupUuid == sg3.uuid }.priority == 1

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).vmNicUuid == vm3.vmNics[0].uuid
        }

        cmd = null

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg3.uuid
            l3NetworkUuid = l3Net_2.uuid
        }

        vm3 = attachL3NetworkToVm {
            l3NetworkUuid = l3Net_2.uuid
            vmInstanceUuid = vm3.uuid
            systemTags = [String.format("l3::%s::SecurityGroupUuids::%s", l3Net_2.uuid, sg3.uuid)]
        }

        refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm3.vmNics[1].uuid).list()
        assert refvos.size() == 1
        assert refvos.find { it.securityGroupUuid == sg3.uuid }.priority == 1

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.size() == 2
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
            l3Net_2 = env.inventoryByName("l3-2") as L3NetworkInventory
            offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
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

            attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
            }
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg2.uuid
                l3NetworkUuid = l3Net.uuid
            }
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg3.uuid
                l3NetworkUuid = l3Net.uuid
            }
        }

        testCreateVmWithSystemtag()
        testAttachNicWithSystemtag()
    }
}
