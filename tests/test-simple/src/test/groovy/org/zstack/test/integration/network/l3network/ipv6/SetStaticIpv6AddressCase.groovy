package org.zstack.test.integration.network.l3network.ipv6

import com.googlecode.ipv6.IPv6Address
import org.zstack.compute.vm.StaticIpOperator
import org.zstack.sdk.GetL3NetworkDhcpIpAddressResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UsedIpInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

/**
 * Created by shixin on 2018/09/10.
 */
class SetStaticIpv6AddressCase extends SubCase {
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
            testCreateVmOfPrivateIPv6Network()
            testCreateVmWithStaticIpv6()
            testAttachL3NetworkToVmNicWithStaticIpv6()
        }
    }

    void testCreateVmOfPrivateIPv6Network() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_vlan = env.inventoryByName("l3-vlan-ipv4")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        /* create vm with 2 nic: 1 nic with ipv4, the other with ipv6 */
        VmInstanceInventory vm = createVmInstance {
            name = "vm-static-ip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3_vlan.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
        }
        assert vm.getVmNics().size() == 2

        VmNicInventory nic
        for (VmNicInventory nicInv: vm.getVmNics()) {
            assert nicInv.getUsedIps().size() == 1
            for (UsedIpInventory ip : nicInv.usedIps) {
                if (ip.l3NetworkUuid == l3_statefull.uuid) {
                    nic = nicInv
                }
            }

        }
        assert nic != null

        /* add ipv4 to the ipv6 nic */
        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        /* reboot the vm to allocate ipv6 address */
        rebootVmInstance {
            uuid = vm.uuid
        }

        UsedIpInventory ip4 = null
        UsedIpInventory ip6 = null
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        nic = queryVmNic {conditions = ["uuid=${nic.uuid}"]} [0]
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                ip4 = ip
            } else if (ip.ipVersion == IPv6Constants.IPv6) {
                ip6 = ip
            }
        }
        nic.getUsedIps().size() == 2
        assert ip4 != null
        assert ip6 != null

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::02").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::03").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::04").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::05").toString())
        ip6s.remove(ip6.ip)
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.11")
        ip4s.add("192.168.110.12")
        ip4s.add("192.168.110.13")
        ip4s.add("192.168.110.14")
        ip4s.remove(ip4.ip)
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        stopVmInstance {
            uuid = vm.uuid
        }

        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }

        nic = queryVmNic {conditions = ["uuid=${nic.uuid}"]} [0]
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ip4Str
            } else if (ip.ipVersion == IPv6Constants.IPv6) {
                assert ip.ip == ip6Str
            }
        }

        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
        staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) == null

        ip4Str = ip4s.get(1)
        ip6Str = ip6s.get(1)
        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip4Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 1
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) == null

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testCreateVmWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::12").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::13").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::14").toString())
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.21")
        ip4s.add("192.168.110.22")
        ip4s.add("192.168.110.23")
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_statefull.uuid, ip4Str),
                          String.format("staticIp::%s::%s", l3_statefull.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ip6Str))]
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        VmNicInventory nic = vm.getVmNics().get(0)
        assert nic.usedIps.size() == 2
        for (UsedIpInventory ip : nic.usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ip4Str
            } else if (ip.ipVersion == IPv6Constants.IPv6){
                assert ip.ip == ip6Str
            }
        }
        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        expect(AssertionError.class) {
            deleteVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = "invalid uuid"
            }
        }

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 1
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testAttachL3NetworkToVmNicWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::22").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::23").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::24").toString())
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.31")
        ip4s.add("192.168.110.32")
        ip4s.add("192.168.110.33")
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        attachL3NetworkToVm {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_statefull.uuid, ip4Str),
                          String.format("staticIp::%s::%s", l3_statefull.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ip6Str))]
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]

        for (VmNicInventory nic : vm.vmNics) {
            if (nic.l3NetworkUuid == l3_statefull.uuid) {
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    if (ip.ipVersion == IPv6Constants.IPv4) {
                        assert ip.ip == ip4Str
                    } else if (ip.ipVersion == IPv6Constants.IPv6){
                        assert ip.ip == ip6Str
                    }
                }
            }
        }

        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        stopVmInstance {
            uuid = vm.uuid
        }
        ip6Str = ip6s.get(1)
        ip4Str = ip4s.get(1)
        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpByVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)
    }
}

