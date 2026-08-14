package org.zstack.test.integration.network.l3network.ipv6

import com.googlecode.ipv6.IPv6Address
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/10.
 */
class Ipv6AddressAllocationCase extends SubCase {
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
            testRandomIpv6Allocation()
            testStateLessIpv6Allocation()
            testIpv6RangeFull()
            testSlaacIpv6RangeFull()
        }
    }

    void testRandomIpv6Allocation() {
        L3NetworkInventory l3 = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic1 = vm1.getVmNics()[0]

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic2 = vm2.getVmNics()[0]

        VmInstanceInventory vm3 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic3 = vm3.getVmNics()[0]

        assert nic1.ip != nic2.ip
        assert nic1.ip != nic3.ip
        assert nic2.ip != nic3.ip
    }

    void testStateLessIpv6Allocation() {
        IpRangeInventory ipr = env.inventoryByName("ipv6-Stateless-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3-Stateless-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic1 = vm1.getVmNics()[0]

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic2 = vm2.getVmNics()[0]

        VmInstanceInventory vm3 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList((l3.uuid))
        }
        VmNicInventory nic3 = vm3.getVmNics()[0]

        assert nic1.ip == IPv6NetworkUtils.getIPv6AddresFromMac(ipr.getNetworkCidr(), nic1.mac)
        assert nic2.ip == IPv6NetworkUtils.getIPv6AddresFromMac(ipr.getNetworkCidr(), nic2.mac)
        assert nic3.ip == IPv6NetworkUtils.getIPv6AddresFromMac(ipr.getNetworkCidr(), nic3.mac)
    }

    void testIpv6RangeFull() {
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_private_ipv6 = createL3Network {
            category = "Private"
            l2NetworkUuid = l2.uuid
            name = "private-ipv6"
            ipVersion = 6
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP", "Eip", "DNS"]]

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3_private_ipv6.uuid
            delegate.networkServices = netServices
        }

        /* there are should 13 usable address */
        IpRangeInventory ipr_1 = addIpv6RangeByNetworkCidr {
            name = "ipr-1"
            l3NetworkUuid = l3_private_ipv6.getUuid()
            networkCidr = "2003:2003::/124"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        /* dhcp server will occupy an ip address */
        List<VmNicInventory> nics = new ArrayList<>()
        for (int i = 0; i < 13; i++) {
            VmNicInventory nic = createVmNic {
                l3NetworkUuid = l3_private_ipv6.uuid
            }
            nics.add(nic)
        }

        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = l3_private_ipv6.uuid
            }
        }

        List<String> ips = nics.stream().distinct().collect(Collectors.toList())
        assert ips.size() == 13

        for (VmNicInventory nic : nics) {
            deleteVmNic {
                uuid = nic.uuid
            }
        }
    }

    void testSlaacIpv6RangeFull() {
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_private_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "private-ipv6"
            ipVersion = 6
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP", "Eip", "DNS"]]

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3_private_ipv6.uuid
            delegate.networkServices = netServices
        }

        /* there are should 16 usable address */
        IpRangeInventory ipr_1 = addIpv6Range {
            name = "ipr-1"
            l3NetworkUuid = l3_private_ipv6.getUuid()
            startIp = "2003:2004::2"
            endIp = "2003:2004::11"
            gateway = "2003:2004::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        /* dhcp server will occupy an ip address */
        List<VmNicInventory> nics = new ArrayList<>()
        for (int i = 0; i< 15; i++) {
            VmNicInventory nic = createVmNic {
                l3NetworkUuid = l3_private_ipv6.uuid
            }
            nics.add(nic)
        }

        expect(AssertionError.class) {
            createVmNic {
                l3NetworkUuid = l3_private_ipv6.uuid
            }
        }

        List<String> ips = nics.stream().distinct().collect(Collectors.toList())
        assert ips.size() == 15
        for (VmNicInventory nic : nics) {
            deleteVmNic {
                uuid = nic.uuid
            }
        }
    }

}

