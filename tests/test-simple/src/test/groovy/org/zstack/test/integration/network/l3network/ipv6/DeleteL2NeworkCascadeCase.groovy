package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/10/17.
 */
class DeleteL2NeworkCascadeCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testDeleteL2Nework()
        }
    }

    void testDeleteL2Nework() {
        L3NetworkInventory l3_vlan_ipv6 = env.inventoryByName("l3-vlan-ipv6")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_vlan_ipv4 = env.inventoryByName("l3-vlan-ipv4")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L2NetworkInventory l2  = env.inventoryByName("vlan-200")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_vlan_ipv6.getUuid()
            networkCidr = "192.168.110.0/24"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_vlan_ipv6.uuid)
        }

        deleteL2Network {
            uuid = l2.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        }[0]
        assert vm.defaultL3NetworkUuid == null
    }


}

