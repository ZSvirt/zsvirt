package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants


/**
 * Created by shixin on 2018/09/10.
 */
class Ipv6GetFreeIpCase extends SubCase {
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
            testGetFreeIp()
        }
    }

    void testGetFreeIp(){
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv6"
            ipVersion = 6
        }
        IpRangeInventory ipr = addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            startIp = "2003:2001::0010"
            endIp = "2003:2001::0020"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3_pub_ipv6.uuid
        }
        assert freeIps.size() == 17
        freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.uuid
        }
        assert freeIps.size() == 17

        GetIpAddressCapacityResult res1 = getIpAddressCapacity { l3NetworkUuids=[l3_pub_ipv6.uuid] }
        GetIpAddressCapacityResult res2 = getIpAddressCapacity { ipRangeUuids=[ipr.uuid] }
        assert res1.totalCapacity.intValue() == 17
        assert res2.totalCapacity.intValue() == 17

        ipr = addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            startIp = "2003:2001::0030"
            endIp = "2003:2001::0040"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3_pub_ipv6.uuid
        }
        assert freeIps.size() == 34
        freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.uuid
        }
        assert freeIps.size() == 17

        res1 = getIpAddressCapacity { l3NetworkUuids=[l3_pub_ipv6.uuid] }
        res2 = getIpAddressCapacity { ipRangeUuids=[ipr.uuid] }
        assert res1.totalCapacity.intValue() == 34
        assert res2.totalCapacity.intValue() == 17

        List<VmNicInventory> nics = new ArrayList<>()
        for (int i = 0; i < 20; i++) {
            VmNicInventory nic = createVmNic {
                l3NetworkUuid = l3_pub_ipv6.uuid
            }
            nics.add(nic)
        }

        freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3_pub_ipv6.uuid
        }
        assert freeIps.size() == 14

        res1 = getIpAddressCapacity { l3NetworkUuids=[l3_pub_ipv6.uuid] }
        assert res1.totalCapacity.intValue() == 34
        assert res1.availableCapacity.intValue() == 14
        assert res1.usedIpAddressNumber == 20

        L3NetworkInventory l3_pub_ipv6_1 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv6"
            ipVersion = 6
        }

        ipr = addIpv6RangeByNetworkCidr {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6_1.getUuid()
            networkCidr = "2003:2002::/124"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3_pub_ipv6_1.uuid
        }
        assert freeIps.size() == 14
        freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.uuid
        }
        assert freeIps.size() == 14

        res1 = getIpAddressCapacity { l3NetworkUuids=[l3_pub_ipv6_1.uuid] }
        res2 = getIpAddressCapacity { ipRangeUuids=[ipr.uuid] }
        assert res1.totalCapacity.intValue() == 14
        assert res1.availableCapacity.intValue() == 14
        assert res2.totalCapacity.intValue() == 14
        assert res2.availableCapacity.intValue() == 14

        for (VmNicInventory nic : nics) {
            deleteVmNic {
                uuid = nic.uuid
            }
        }
    }

}

