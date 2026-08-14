package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/26.
 */
class IPv6VipCase extends SubCase {
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
            testDeleteVipPeerL3Ref()
        }
    }

    void testDeleteVipPeerL3Ref() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_statefull_1 = env.inventoryByName("l3-Statefull-DHCP-1")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host = env.inventoryByName("kvm-1")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
            hostUuid = host.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        } [0]
        VmNicInventory nic = vm.getVmNics()[0]
        UsedIpInventory ipv6
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv6) {
                ipv6 = ip
            }
        }

        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = l3_statefull_1.uuid
            requiredIp = "2001:2004::2004"
        }

        EipInventory eip6 = createEip {
            name = "eip6"
            vipUuid = vip6.uuid
        }

        eip6 = attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv6.uuid
        }
        vip6 = queryVip {conditions = ["name=vip6"]}[0]
        assert vip6.peerL3NetworkUuids.size() == 1
        String peerL3 = vip6.peerL3NetworkUuids.get(0)
        assert peerL3 == l3_statefull.uuid

        detachL3NetworkFromVm {
            vmNicUuid = nic.uuid
        }
        vip6 = queryVip {conditions = ["name=vip6"]}[0]
        assert vip6.peerL3NetworkUuids == null || vip6.peerL3NetworkUuids.size() == 0
    }

}

