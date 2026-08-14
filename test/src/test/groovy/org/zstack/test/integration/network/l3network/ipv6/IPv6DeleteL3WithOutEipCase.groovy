package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/10/12.
 */
class IPv6DeleteL3WithOutEipCase extends SubCase {
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
            testDeleteL3WithoutEip()
        }
    }

    void testDeleteL3WithoutEip() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        List<VmInstanceInventory> vms = new ArrayList<>()
        List<VmNicInventory> nics = new ArrayList<>()
        for (int i = 0; i < 5; i ++) {
            VmInstanceInventory vm = createVmInstance {
                name = "vm-eip" + i.toString()
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_statefull.uuid)
            }
            vms.add(vm)
            VmNicInventory nic = vm.getVmNics()[0]
            nics.add(nic)
        }

        deleteL3Network {
            uuid = l3_slaac.uuid
        }

        for (VmNicInventory nic : nics) {
            assert Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).exists
            for (UsedIpInventory ip : nic.getUsedIps()) {
                if (ip.l3NetworkUuid == l3_slaac.uuid) {
                    assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.uuid).exists
                } else {
                    assert Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.uuid).exists
                }
            }
        }

        def thread1 = Thread.start {
            deleteL3Network {
                uuid = l3_statefull.uuid
            }
        }

        def thread2 = Thread.start {
            deleteL3Network {
                uuid = l3.uuid
            }
        }
        [thread1, thread2].each {it.join()}

        for (VmNicInventory nic : nics) {
            assert !Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).exists
            for (UsedIpInventory ip : nic.getUsedIps()) {
                assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, ip.uuid).exists
            }
        }
    }

}

