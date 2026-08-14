package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import java.util.stream.Collectors

/**
 * Created by shixin.ruan on 2024-06-06.
 */
class FlatWithIpamCase extends SubCase{

    EnvSpec env
    FlatDhcpBackend dhcpBackend
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        dhcpBackend = bean(FlatDhcpBackend.class)
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalCpu = 1000
                        totalMem = SizeUnit.GIGABYTE.toByte(10000)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.HostRoute.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE,
                                     UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                }
                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pub-l3"
                        category = "Public"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.HostRoute.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ipv6 {
                            name = "ipv6-Stateless-DHCP"
                            networkCidr = "2024:05:07:86:01::/64"
                            addressMode = "Stateless-DHCP"
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm-1"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-pub"
                useImage("image")
                useDefaultL3Network("pub-l3")
                useL3Networks("pub-l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testPrivateNetworkWithoutIpam()
            testPublicNetworkWithoutIpam()
            testEipWithoutIpam()
            testStaticIpSystemTag()
        }
    }

    void testPrivateNetworkWithoutIpam(){
        VmInstanceInventory vm = env.inventoryByName("vm-1")
        L3NetworkInventory l31 = env.inventoryByName("l3-1")
        L3NetworkInventory pub = env.inventoryByName("pub-l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        HostInventory host = env.inventoryByName ("host-1")

        VmNicInventory nic = vm.vmNics.get(0)
        VipInventory vip4 = createVip {
            name = "vip4"
            l3NetworkUuid = pub.uuid
        }

        EipInventory eip4 = createEip {
            name = "eip4"
            vipUuid = vip4.uuid
            vmNicUuid = nic.uuid
        }

        /* because only 1 host, only 1 BatchApplyDhcpCmd */
        FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            batchApplyDhcpCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        /* because only 1 host, only 1 BatchApplyDhcpCmd */
        FlatDhcpBackend.FlushDhcpNamespaceCmd flushCmd = null
        env.afterSimulator(FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH) { rsp, HttpEntity<String> e ->
            flushCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.FlushDhcpNamespaceCmd.class)
            return rsp
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l31.uuid
            service = "DHCP"
        }
        assert flushCmd != null

        batchApplyDhcpCmd = null
        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }
        assert batchApplyDhcpCmd == null

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        batchApplyDhcpCmd = null
        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
        }
        VmNicInventory nic2 = vm2.vmNics.get(0)
        assert nic2.ip == null
        assert batchApplyDhcpCmd == null

        List<UsedIpInventory> ipv4s = getFreeIp {
            l3NetworkUuid = nic.l3NetworkUuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        }
        FreeIpInventory ipv4 = ipv4s.get(0)
        VmInstanceInventory vm3 = createVmInstance {
            name = "vm-3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
            systemTags = [
                    "staticIp::" + l31.uuid + "::${ipv4.ip}",
                    "ipv4Gateway::" + l31.uuid + "::${ipv4.gateway}",
                    "ipv4Netmask::" + l31.uuid + "::${ipv4.netmask}"
            ]
        }
        assert batchApplyDhcpCmd == null

        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm2.uuid
        }

        rebootVmInstance {
            uuid = vm3.uuid
        }
        assert batchApplyDhcpCmd == null

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        batchApplyDhcpCmd = null
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l31.uuid
            networkServices = ['Flat':['DHCP']]
        }
        assert batchApplyDhcpCmd != null
        assert batchApplyDhcpCmd.dhcpInfos.size() == 1
        FlatDhcpBackend.ApplyDhcpCmd info = batchApplyDhcpCmd.dhcpInfos.get(0)
        assert info.dhcp.size() == 2 /* vm-2 doesn't have ip address */
        List<String> ips = info.dhcp.stream().map { i -> i.ip}.collect(Collectors.toList())
        assert ips.contains(ipv4.ip)

        VmInstanceInventory vm4 = createVmInstance {
            name = "vm-4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
        }

        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm2.uuid
        }

        rebootVmInstance {
            uuid = vm3.uuid
        }

        rebootVmInstance {
            uuid = vm4.uuid
        }

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        destroyVmInstance {
            uuid = vm2.uuid
        }
        destroyVmInstance {
            uuid = vm3.uuid
        }
        destroyVmInstance {
            uuid = vm4.uuid
        }

        detachEip {
            uuid = eip4.uuid
        }
    }

    void testPublicNetworkWithoutIpam(){
        VmInstanceInventory vm = env.inventoryByName("vm-pub")
        L3NetworkInventory pub = env.inventoryByName("pub-l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        HostInventory host = env.inventoryByName ("host-1")

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = pub.uuid
            service = "DHCP"
        }

        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pub.uuid]
        }
        List<UsedIpInventory> ipv4s = getFreeIp {
            l3NetworkUuid = pub.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        }
        FreeIpInventory ipv4 = ipv4s.get(0)
        List<UsedIpInventory> ipv6s = getFreeIp {
            l3NetworkUuid = pub.uuid
            ipVersion = IPv6Constants.IPv6
            limit = 1
        }
        FreeIpInventory ipv6 = ipv6s.get(0)
        String ip6tag = IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.ip)
        String gw6tag = IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.gateway)
        VmInstanceInventory vm3 = createVmInstance {
            name = "vm-3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pub.uuid]
            systemTags = [
                    "staticIp::" + pub.uuid + "::${ipv4.ip}",
                    "staticIp::" + pub.uuid + "::${ip6tag}",
                    "ipv4Gateway::" + pub.uuid + "::${ipv4.gateway}",
                    "ipv6Gateway::" + pub.uuid + "::${gw6tag}",
                    "ipv4Netmask::" + pub.uuid + "::${ipv4.netmask}",
                    "ipv6Prefix::" + pub.uuid + "::64"
            ]
        }

        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm2.uuid
        }

        rebootVmInstance {
            uuid = vm3.uuid
        }

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = pub.uuid
            networkServices = ['Flat':['DHCP']]
        }
        VmInstanceInventory vm4 = createVmInstance {
            name = "vm-4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pub.uuid]
        }

        /* reboot vm */
        rebootVmInstance {
            uuid = vm.uuid
        }

        rebootVmInstance {
            uuid = vm2.uuid
        }

        rebootVmInstance {
            uuid = vm3.uuid
        }

        rebootVmInstance {
            uuid = vm4.uuid
        }

        /* reconnect host */
        reconnectHost {
            uuid = host.uuid
        }

        destroyVmInstance {
            uuid = vm2.uuid
        }
        destroyVmInstance {
            uuid = vm3.uuid
        }
        destroyVmInstance {
            uuid = vm4.uuid
        }
    }

    void testEipWithoutIpam(){
        VmInstanceInventory vm = env.inventoryByName("vm-1")
        L3NetworkInventory l31 = env.inventoryByName("l3-1")
        L3NetworkInventory pub = env.inventoryByName("pub-l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        HostInventory host = env.inventoryByName ("host-1")

        IpRangeInventory pubIpr6 = (queryIpRange {
            conditions = ["l3NetworkUuid=${pub.uuid}", "ipVersion=${IPv6Constants.IPv6}"]
        } as List<IpRangeInventory>).get(0)

        addIpv6RangeByNetworkCidr {
            name = "l3-1-ip6"
            l3NetworkUuid = l31.getUuid()
            networkCidr = "2024:6:12:86:1::/64"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        vm = queryVmInstance {conditions = ["uuid=${vm.uuid}"]}[0]
        VmNicInventory nic1 = vm.vmNics.get(0)
        UsedIpInventory ip61
        for (UsedIpInventory ip : nic1.usedIps) {
            if (ip.ipVersion == 6) {
                ip61 = ip
            }
        }
        VipInventory vip4 = createVip {
            name = "vip4"
            l3NetworkUuid = pub.uuid
        }
        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = pub.uuid
            ipRangeUuid = pubIpr6.uuid
        }

        EipInventory eip4 = createEip {
            name = "eip4"
            vipUuid = vip4.uuid
        }
        EipInventory eip6 = createEip {
            name = "eip5"
            vipUuid = vip6.uuid
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l31.uuid
            service = "DHCP"
        }

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-2"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
        }
        VmNicInventory nic2 = vm2.vmNics.get(0)
        List<UsedIpInventory> ipv4s = getFreeIp {
            l3NetworkUuid = l31.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        }
        FreeIpInventory ipv4 = ipv4s.get(0)
        List<UsedIpInventory> ipv6s = getFreeIp {
            l3NetworkUuid = l31.uuid
            ipVersion = IPv6Constants.IPv6
            limit = 1
        }
        FreeIpInventory ipv6 = ipv6s.get(0)
        String ip6tag = IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.ip)
        String gw6tag = IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.gateway)
        VmInstanceInventory vm3 = createVmInstance {
            name = "vm-3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
            systemTags = [
                    "staticIp::" + l31.uuid + "::${ipv4.ip}",
                    "staticIp::" + l31.uuid + "::${ip6tag}",
                    "ipv4Gateway::" + l31.uuid + "::${ipv4.gateway}",
                    "ipv6Gateway::" + l31.uuid + "::${gw6tag}",
                    "ipv4Netmask::" + l31.uuid + "::${ipv4.netmask}",
                    "ipv6Prefix::" + l31.uuid + "::64"
            ]
        }
        VmNicInventory nic3 = vm3.vmNics.get(0)
        UsedIpInventory ip63
        for (UsedIpInventory ip : nic3.usedIps) {
            if (ip.ipVersion == 6) {
                ip63 = ip
            }
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
            vmNicUuid = nic3.uuid
        }
        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic3.uuid
            usedIpUuid = ip63.uuid
        }

        detachEip {
            uuid = eip4.uuid
        }
        detachEip {
            uuid = eip6.uuid
        }

        attachEip {
            eipUuid = eip4.uuid
            vmNicUuid = nic1.uuid
        }
        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = nic1.uuid
            usedIpUuid = ip61.uuid
        }
    }

    void testStaticIpSystemTag(){
        L3NetworkInventory l31 = env.inventoryByName("l3-1")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")

        List<IpRangeInventory> ipranges = queryIpRange {conditions=["l3NetworkUuid=${l31.uuid}"]}
        for (IpRangeInventory ipr : ipranges) {
            deleteIpRange {
                uuid = ipr.uuid
            }
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l31.uuid
            service = 'DHCP'
        }

        expect(AssertionError.class){
            /* no netmask failed */
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l31.uuid]
                systemTags = [
                        "staticIp::" + l31.uuid + "::192.168.1.100",
                ]
            }
        }

        expect(AssertionError.class){
            /* no netmask failed */
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l31.uuid]
                systemTags = [
                        "staticIp::" + l31.uuid + "::192.168.1.100",
                        "ipv4Gateway::" + l31.uuid + "::255.255.255.0",
                ]
            }
        }

        expect(AssertionError.class){
            /* no prefix length failed */
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l31.uuid]
                systemTags = [
                        "staticIp::" + l31.uuid + "::2024:6:13:86:1--aa",
                ]
            }
        }

        IpRangeInventory ipr4 = addIpRangeByNetworkCidr {
            name = "cidr-1"
            l3NetworkUuid = l31.getUuid()
            networkCidr = "192.168.1.0/24"
        }

        VmInstanceInventory vm4 = createVmInstance {
            name = "vm-ipv4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
            systemTags = [
                    "staticIp::" + l31.uuid + "::192.168.1.100",
            ]
        }
        VmNicInventory nic4 = vm4.vmNics.get(0)
        UsedIpInventory ip = nic4.usedIps.get(0)
        assert nic4.ip == "192.168.1.100"
        assert nic4.netmask == ipr4.netmask
        assert nic4.gateway == ipr4.gateway
        assert ip.ipRangeUuid == ipr4.uuid

        deleteIpRange {
            uuid = ipr4.uuid
        }

        IpRangeInventory ipr6 = addIpv6RangeByNetworkCidr {
            name = "ipv6"
            l3NetworkUuid = l31.getUuid()
            networkCidr = "2024:6:13:86:1::/64"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        VmInstanceInventory vm6 = createVmInstance {
            name = "vm-ipv6"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
            systemTags = [
                    "staticIp::" + l31.uuid + "::2024:6:13:86:1--aa",
            ]
        }

        VmNicInventory nic6 = vm6.vmNics.get(0)
        ip = nic6.usedIps.get(0)
        assert nic6.ip == "2024:6:13:86:1::aa"
        assert nic6.netmask == ipr6.netmask
        assert nic6.gateway == ipr6.gateway
        assert ip.ipRangeUuid == ipr6.uuid
    }


    @Override
    void clean() {
        env.delete()
    }
}
