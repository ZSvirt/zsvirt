package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class GetCandidateVmNicForSecurityGroupCase extends SubCase {
    EnvSpec env

    AccountInventory accountInventory
    L3NetworkInventory l3Net, l3Net_2
    InstanceOfferingInventory offer
    ImageInventory image
    List<VmInstanceInventory> vmInvs = new ArrayList<>();
    SecurityGroupInventory sg1, sg2, sg3

    void buildVmInvenceory(String l3Uuid) {
        for (int i = 1; i <= 5; i++) {
            VmInstanceInventory vm = createVmInstance {
                name = "vm-$i-on-l3-$l3Uuid"
                imageUuid = image.uuid
                l3NetworkUuids = [l3Uuid]
                instanceOfferingUuid  = offer.uuid
            } as VmInstanceInventory

            vmInvs.add(vm)
        }
    }

    void testGetCandiateVmNicsByAdmin() {
        GetCandidateVmNicForSecurityGroupAction action = new GetCandidateVmNicForSecurityGroupAction()
        action.securityGroupUuid = sg1.uuid
        action.sessionId = adminSession()

        def result = action.call()
        assert result.value.inventories.size() == 14

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vmInvs[0].vmNics[0].uuid, vmInvs[1].vmNics[0].uuid, vmInvs[2].vmNics[0].uuid]
        }

        result = action.call()
        assert result.value.inventories.size() == 11
    }

    void testGetCandiateVmNicsByUser() {
        SessionInventory session = logInByAccount {
            accountName = accountInventory.name
            password = "password"
        } as SessionInventory

        GetCandidateVmNicForSecurityGroupAction action = new GetCandidateVmNicForSecurityGroupAction()
        action.securityGroupUuid = sg1.uuid
        action.sessionId = session.uuid

        def result = action.call()
        assert result.value.inventories.size() == 0
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

            buildVmInvenceory(l3Net.uuid)
            buildVmInvenceory(l3Net_2.uuid)

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

            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory
            attachPredefineRoles(accountInventory.uuid, "security-group")
        }

        testGetCandiateVmNicsByAdmin()
        testGetCandiateVmNicsByUser()
    }
}
