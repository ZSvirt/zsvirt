package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/10/03.
 */
class IPv6DeleteVmWithEipCase extends SubCase {
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
            testDeleteVmWithEip()
            testDeleteVm()
        }
    }

    void testDeleteVmWithEip() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

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
        }

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-eip-not-attachable"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }

        VmNicInventory nic = vm.getVmNics()[0]
        UsedIpInventory ipv4
        UsedIpInventory ipv6
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                ipv4 = ip
            } else {
                ipv6 = ip
            }
        }

        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = l3_slaac.uuid
        }

        VipInventory vip4 = createVip {
            name = "vip4"
            l3NetworkUuid = l3_1.uuid
        }

        EipInventory eip6 = createEip {
            name = "eip6"
            vipUuid = vip6.uuid
        }

        EipInventory eip4 = createEip {
            name = "eip4"
            vipUuid = vip4.uuid
        }

        List<VmNicInventory> nics = getEipAttachableVmNics {
            eipUuid = eip6.uuid
        }
        assert nics.size() == 2
        nics = getEipAttachableVmNics {
            eipUuid = eip4.uuid
        }
        assert nics.size() == 2

        attachEip {
            eipUuid = eip4.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv4.uuid
        }

        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv6.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testDeleteVm() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]

        destroyVmInstance {
            uuid = vm.uuid
        }
        expungeVmInstance {
            uuid = vm.uuid
        }

        assert !Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).isExists()
        for (UsedIpInventory ip : nic.getUsedIps()) {
            assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.uuid).isExists()
        }
    }

}

