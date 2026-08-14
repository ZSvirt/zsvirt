package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/26.
 */
class IPv6EipCase extends SubCase {
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
            testIPv6EipApiValidator()
            testIPv6EipApplyNetworkService()
        }
    }

    void testIPv6EipApiValidator() {
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
        assert ipv6.netmask == IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr("2001:2003::/64")

        expect(AssertionError.class) {
            VipInventory vip6 = createVip {
                name = "vip6"
                l3NetworkUuid = l3_statefull_1.uuid
                requiredIp = "192.168.2.1"
            }
        }

        expect(AssertionError.class) {
            VipInventory vip4 = createVip {
                name = "vip4"
                l3NetworkUuid = l3_1.uuid
                requiredIp = "2001:2004::2004"
            }
        }

        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = l3_statefull_1.uuid
            requiredIp = "2001:2004::2004"
        }
        assert vip6.netmask == IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr("2001:2004::/64")

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

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip4.uuid
                vmNicUuid = nic.uuid
                usedIpUuid = ipv6.uuid
            }
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip6.uuid
                vmNicUuid = nic.uuid
                usedIpUuid = ipv4.uuid
            }
        }

        FlatEipBackend.ApplyEipCmd cmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }
        attachEip {
            eipUuid = eip4.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv4.uuid
        }
        assert cmd.eip.vmBridgeName == "br_eth0"

        cmd = null
        eip6 = attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic.uuid
            usedIpUuid = ipv6.uuid
        }
        assert cmd.eip.vmBridgeName == "br_eth0"
        assert eip6.guestIp == ipv6.ip

        stopVmInstance {
            uuid = vm.uuid
        }

        /* static ip should not be dhcp server ip or other used ip */
        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }

        List<UsedIpInventory> ipv6s = getFreeIp {
            l3NetworkUuid = l3_statefull.uuid
            ipVersion = IPv6Constants.IPv6
            limit = 1
        }

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            ip6 = ipv6s.get(0).ip
        }
        eip6 = queryEip { conditions=["name=eip6"] }[0]
        assert eip6.guestIp == ipv6s.get(0).ip
    }

    void testIPv6EipApplyNetworkService() {
        HostInventory host = env.inventoryByName("kvm-1")

        VmInstanceInventory vm = queryVmInstance {
            conditions=["name=vm-eip"]
        } [0]
        assert vm.getVmNics().size() == 1
        VmNicInventory nic = vm.getVmNics().get(0)

        FlatEipBackend.ApplyEipCmd cmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }
        startVmInstance {
            uuid = vm.uuid
            hostUuid = vm.lastHostUuid
        }
        assert cmd.eip.vmBridgeName == "br_eth0"

        FlatEipBackend.BatchApplyEipCmd bcmd = new FlatEipBackend.BatchApplyEipCmd()
        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            bcmd = json(entity.getBody(), FlatEipBackend.BatchApplyEipCmd.class)
            return rsp

        }
        reconnectHost {
            uuid = host.uuid
        }
        assert bcmd.eips.size() == 2
        FlatEipBackend.EipTO to1 = bcmd.eips.get(0)
        FlatEipBackend.EipTO to2 = bcmd.eips.get(1)
        assert to1.vmBridgeName == "br_eth0"
        assert to2.vmBridgeName == "br_eth0"

        EipInventory eip4 = queryEip {
            conditions=["name=eip4"]
        } [0]

        detachEip {
            uuid = eip4.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        reconnectHost {
            uuid = host.uuid
        }

        EipInventory eip6 = queryEip {
            conditions=["name=eip6"]
        } [0]

        detachEip {
            uuid = eip6.uuid
        }
    }

}

