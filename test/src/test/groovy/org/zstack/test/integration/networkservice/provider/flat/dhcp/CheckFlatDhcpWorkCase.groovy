package org.zstack.test.integration.networkservice.provider.flat.dhcp

import junit.framework.Assert
import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.GetL3NetworkDhcpIpAddressResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceL3NetworkRefInventory
import org.zstack.sdk.UsedIpInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import java.util.stream.Collectors

/**
 * Created by heathhose on 17-4-1.
 */
class CheckFlatDhcpWorkCase extends SubCase{

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
                    }

                    kvm {
                        name = "host-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
                useL3Networks("l3-1","l3-2")
                useInstanceOffering("instanceOffering")
                useHost("host-1")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1","l3-2")
                useInstanceOffering("instanceOffering")
                useHost("host-2")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testFlatDhcpUpgrade()
            checkDhcpWork()
            testDisableIpv4Dhcp()
            testDisableDualStackDhcp()
        }
    }

    void testFlatDhcpUpgrade() {
        final L3NetworkInventory l32 = env.inventoryByName("l3-2")
        FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.deleteInherentTag(l32.uuid)
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
        }
        assert ret.ip == null
        assert ret.ip6 == null
        dhcpBackend.upgradeFlatDhcpServerIp()
        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
    }

    void checkDhcpWork(){
        VmInstanceInventory vm = env.inventoryByName("vm-1")
        final L3NetworkInventory l31 = env.inventoryByName("l3-1")
        final L3NetworkInventory l32 = env.inventoryByName("l3-2")

        VmNicInventory n = CollectionUtils.findOneOrNull(vm.getVmNics(),
                {VmNicInventory it -> (it.getL3NetworkUuid() == l31.getUuid()) })
        assert n != null
        assert n.getDeviceId() == 0

        n = CollectionUtils.findOneOrNull(vm.getVmNics(),
                {VmNicInventory it -> (it.getL3NetworkUuid() == l32.getUuid()) })
        assert n != null
        assert n.getDeviceId() == 1

        List<FlatDhcpBackend.DhcpInfo> dhcpInfoList = new ArrayList<FlatDhcpBackend.DhcpInfo>()
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            assert null != cmd
            cmd.dhcpInfos.each { info ->
                dhcpInfoList.addAll(info.dhcp)
            }
            return rsp
        }
        goOn:
        for (FlatDhcpBackend.DhcpInfo info : dhcpInfoList) {
            if (!info.isDefaultL3Network && info.hostname != null) {
                Assert.fail(String.format("wrong hostname set. %s", JSONObjectUtil.toJsonString(info)))
            }

            for (VmNicInventory nic : vm.getVmNics()) {
                if (info.ip.equals(nic.getIp()) && info.gateway.equals(nic.getGateway()) && info.netmask.equals(nic.getNetmask())) {
                    break goOn
                }
            }

            Assert.fail(String.format("nic %s", JSONObjectUtil.toJsonString(cmd.dhcp)))
        }
    }

    void testDisableIpv4Dhcp(){
        final L3NetworkInventory l31 = env.inventoryByName("l3-1")
        VmInstanceInventory vm = env.inventoryByName("vm-1")

        List<FlatDhcpBackend.FlushDhcpNamespaceCmd> flushCmds = Collections.synchronizedList(new ArrayList<FlatDhcpBackend.FlushDhcpNamespaceCmd>())
        env.afterSimulator(FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.FlushDhcpNamespaceCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.FlushDhcpNamespaceCmd.class)
            flushCmds.add(cmd)
            return rsp
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l31.uuid
            networkServices = ['Flat':['DHCP']]
        }
        assert flushCmds.size() == 2
        flushCmds.clear()
        l31 = queryL3Network{ conditions = ["uuid=${l31.uuid}"]}[0]
        for (NetworkServiceL3NetworkRefInventory ref : l31.networkServices) {
            if (ref.networkServiceType == "DHCP") {
                assert false
            }
        }
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
        }
        assert ret.ip6 == null
        assert ret.ip == null

        List<FlatDhcpBackend.BatchPrepareDhcpCmd> bCmds = Collections.synchronizedList(new ArrayList<FlatDhcpBackend.BatchPrepareDhcpCmd>())
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchPrepareDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchPrepareDhcpCmd.class)
            bCmds.add(cmd)
            return rsp
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l31.uuid
            networkServices = ["Flat":["DHCP"]]
        }
        assert bCmds.size() == 2
        bCmds.clear()
        l31 = queryL3Network{ conditions = ["uuid=${l31.uuid}"]}[0]
        List<String> services = l31.networkServices.stream().map {ref -> ref.networkServiceType}.collect(Collectors.toList())
        assert services.contains("DHCP")

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l31.uuid
            service = 'DHCP'
        }

        /* dhcp is disabled, can not change dhcp server ip */
        expect(AssertionError.class) {
            changeL3NetworkDhcpIpAddress {
                l3NetworkUuid = l31.uuid
                dhcpServerIp = "172.16.10.10"
            }
        }

        /* l31 ip range: 192.168.100.10 ~ 192.168.100.100，  */

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l31.uuid
            networkServices = ["Flat":["DHCP"]]
            systemTags = [String.format("flatNetwork::DhcpServer::192.168.100.149::ipUuid::NULL")]
        }
        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
        }
        assert ret.ip == "192.168.100.149"

        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
            dhcpServerIp = "192.168.100.150"
        }
        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
        }
        assert ret.ip == "192.168.100.150"
        String oldDhcpServer = ret.ip

        /* can not change dhcp server ip, because IP address is used */
        VmNicInventory l31Nic
        for (VmNicInventory nic : vm.vmNics) {
            if (nic.l3NetworkUuid == l31.uuid) {
                l31Nic = nic
            }
        }
        expect(AssertionError.class) {
            changeL3NetworkDhcpIpAddress {
                l3NetworkUuid = l31.uuid
                dhcpServerIp = l31Nic.ip
            }
        }

        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
        }
        assert ret.ip == oldDhcpServer

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = l31.getUuid()
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>

        bCmds.clear()
        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
            dhcpServerIp = freeIp4s.get(0).ip
        }
        assert bCmds.size() == 2
        bCmds.clear()

        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l31.uuid
        }
        assert ret.ip == freeIp4s.get(0).ip
        assert ret.ip6 == null

        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, oldDhcpServer).isExists()
    }

    void testDisableDualStackDhcp(){
        final L3NetworkInventory l32 = env.inventoryByName("l3-2")

        List<FlatDhcpBackend.FlushDhcpNamespaceCmd> flushCmds = Collections.synchronizedList(new ArrayList<FlatDhcpBackend.FlushDhcpNamespaceCmd>())
        env.afterSimulator(FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.FlushDhcpNamespaceCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.FlushDhcpNamespaceCmd.class)
            flushCmds.add(cmd)
            return rsp
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l32.uuid
            networkServices = ['Flat':['DHCP']]
        }
        assert flushCmds.size() == 2
        flushCmds.clear()
        l32 = queryL3Network{ conditions = ["uuid=${l32.uuid}"]}[0]
        for (NetworkServiceL3NetworkRefInventory ref : l32.networkServices) {
            if (ref.networkServiceType == "DHCP") {
                assert false
            }
        }
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
        }
        assert ret.ip6 == null
        assert ret.ip == null

        List<FlatDhcpBackend.BatchPrepareDhcpCmd> bCmds = Collections.synchronizedList(new ArrayList<FlatDhcpBackend.BatchPrepareDhcpCmd>())
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchPrepareDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchPrepareDhcpCmd.class)
            bCmds.add(cmd)
            return rsp
        }

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l32.uuid
            networkServices = ["Flat":["DHCP"]]
        }
        assert bCmds.size() == 2
        bCmds.clear()
        l32 = queryL3Network{ conditions = ["uuid=${l32.uuid}"]}[0]
        List<String> services = l32.networkServices.stream().map {ref -> ref.networkServiceType}.collect(Collectors.toList())
        assert services.contains("DHCP")

        UsedIpInventory usedIp4 = queryIpAddress {conditions=["l3NetworkUuid=${l32.uuid}", "ipVersion=4"]
            limit = 1} [0]
        UsedIpInventory usedIp6 = queryIpAddress {conditions=["l3NetworkUuid=${l32.uuid}", "ipVersion=6"]
            limit = 1} [0]
        expect(AssertionError.class) {
            changeL3NetworkDhcpIpAddress {
                l3NetworkUuid = l32.uuid
                dhcpServerIp = usedIp4.ip
            }
        }
        expect(AssertionError.class) {
            changeL3NetworkDhcpIpAddress {
                l3NetworkUuid = l32.uuid
                dhcpServerIp = usedIp6.ip
            }
        }

        def freeIp6s = getFreeIp {
            l3NetworkUuid = l32.getUuid()
            ipVersion = IPv6Constants.IPv6
            limit = 1
        } as List<FreeIpInventory>

        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
            dhcpv6ServerIp = freeIp6s.get(0).ip
        }
        assert bCmds.size() == 2
        bCmds.clear()

        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
        }
        assert ret.ip6 == freeIp6s.get(0).ip

        /* change again */
        freeIp6s = getFreeIp {
            l3NetworkUuid = l32.getUuid()
            ipVersion = IPv6Constants.IPv6
            limit = 1
        } as List<FreeIpInventory>

        def freeIp4s = getFreeIp {
            l3NetworkUuid = l32.getUuid()
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>

        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
            dhcpServerIp = freeIp4s.get(0).ip
            dhcpv6ServerIp = freeIp6s.get(0).ip
        }
        assert bCmds.size() == 2
        bCmds.clear()

        ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l32.uuid
        }
        assert ret.ip == freeIp4s.get(0).ip
        assert ret.ip6 == freeIp6s.get(0).ip
    }

    @Override
    void clean() {
        env.delete()
    }
}
