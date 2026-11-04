package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

/**
 * Created by shixin on 2018/11/30.
 */
class Ipv6DeleteRangeCase extends SubCase {
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
            testDeleteIpRange()
        }
    }

    void testDeleteIpRange(){
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_ipv4 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "ipv4"
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_ipv4.uuid
            networkServices = ['Flat':['DHCP']]
        }

        IpRangeInventory ip41 = addIpRange {
            name = "ipr-4-1"
            l3NetworkUuid = l3_ipv4.getUuid()
            startIp = "192.168.1.10"
            endIp = "192.168.1.50"
            gateway = "192.168.1.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory addressPool = addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3_ipv4.uuid
            startIp = "192.168.201.20"
            endIp = "192.168.201.50"
            netmask = "255.255.255.0"
            ipRangeType = org.zstack.header.network.l3.IpRangeType.AddressPool.toString()
        }
        IpRangeInventory range6 = addIpv6Range {
            name = "ipr-6-1"
            l3NetworkUuid = l3_ipv4.uuid
            startIp = "2020:09:17::0060"
            endIp = "2020:09:17::00e0"
            gateway = "2020:09:17::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }
        deleteIpRange {
            uuid = range6.uuid
        }
        deleteIpRange {
            uuid = addressPool.uuid
        }

        IpRangeInventory ip42 = addIpRange {
            name = "ipr-4-2"
            l3NetworkUuid = l3_ipv4.getUuid()
            startIp = "192.168.1.110"
            endIp = "192.168.1.150"
            gateway = "192.168.1.1"
            netmask = "255.255.255.0"
        }

        L3NetworkInventory l3_ipv6 = createL3Network {
            category = "Private"
            l2NetworkUuid = l2.uuid
            name = "ipv4"
            ipVersion = 6
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_ipv6.uuid
            networkServices = ['Flat':['DHCP']]
        }
        IpRangeInventory ip61 = addIpv6Range {
            name = "ipr-6-1"
            l3NetworkUuid = l3_ipv6.getUuid()
            startIp = "2003:2001::0060"
            endIp = "2003:2001::00e0"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }
        IpRangeInventory ip62 = addIpv6Range {
            name = "ipr-6-2"
            l3NetworkUuid = l3_ipv6.getUuid()
            startIp = "2003:2001::0160"
            endIp = "2003:2001::01e0"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        String systemTag1 = String.format("staticIp::%s::192.168.1.20", l3_ipv4.uuid)
        String systemTag2 = String.format("staticIp::%s::2003:2001--0070", l3_ipv6.uuid)
        VmInstanceInventory vm1 = createVmInstance {
            name = "test-vm-1"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_ipv4.uuid, l3_ipv6.uuid]
            defaultL3NetworkUuid = l3_ipv4.uuid
            systemTags = [systemTag1, systemTag2]
        }
        UsedIpInventory ip1 = queryIpAddress{conditions=["ip=2003:2001::70"]}[0]
        assert ip1.ipRangeUuid == ip61.uuid

        String systemTag3 = String.format("staticIp::%s::192.168.1.120", l3_ipv4.uuid)
        String systemTag4 = String.format("staticIp::%s::2003:2001--0170", l3_ipv6.uuid)
        VmInstanceInventory vm2 = createVmInstance {
            name = "test-vm-2"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_ipv4.uuid, l3_ipv6.uuid]
            defaultL3NetworkUuid = l3_ipv4.uuid
            systemTags = [systemTag3, systemTag4]
        }
        UsedIpInventory ip2 = queryIpAddress{conditions=["ip=2003:2001::170"]}[0]
        assert ip2.ipRangeUuid == ip62.uuid

        deleteIpRange {
            uuid = ip41.uuid
        }

        deleteIpRange {
            uuid = ip62.uuid
        }

        vm1 = queryVmInstance {conditions=["uuid=${vm1.uuid}".toString()]}[0]
        vm2 = queryVmInstance {conditions=["uuid=${vm2.uuid}".toString()]}[0]

        assert vm1.getVmNics().size() == 1
        assert vm2.getVmNics().size() == 1
    }
}

