package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VipUseForList
import org.zstack.utils.data.SizeUnit
/**
 * Created by shixin on 18-02-08.
 */
class PortForwardingVipUseForCase extends SubCase {

    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
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
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm-pf"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testVipUseForWhenCreatePortForwarding()
            testVipUseForWhenAttachPortForwarding()
            testVipUseForWhenVmStopStart()
        }
    }

    void testVipUseForWhenCreatePortForwarding(){
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        VipInventory vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        }

        def vm = env.inventoryByName("vm-pf") as VmInstanceInventory
        PortForwardingRuleInventory pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        deletePortForwardingRule {
            uuid = pf1Inv.uuid
        }
        assert !Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).isExists()

        PortForwardingRuleInventory pf2Inv = createPortForwardingRule{
            name = "pf-2"
            vipPortStart = 22
            privatePortStart = 22
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.vmNics.get(0).uuid
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        deletePortForwardingRule {
            uuid = pf2Inv.uuid
        }
        assert !Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).isExists()

        /* simulate install pf failed */
        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            rsp.setSuccess(false)
            rsp.setError("on purpose")
            return rsp
        }

        boolean s = false
        try {
            createPortForwardingRule{
                name = "pf-3"
                vipPortStart = 22
                privatePortStart = 22
                protocolType = PortForwardingProtocolType.TCP.toString()
                vmNicUuid = vm.vmNics.get(0).uuid
                vipUuid = vip.uuid
            }
        } catch (AssertionError e) {
            s = true
        }
        assert s
        assert Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.vipUuid, vip.getUuid()).find() == null
        retryInSecs() {
            assert !Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).isExists()
        }

        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            return rsp
        }

        deleteVip {
            uuid = vip.getUuid()
        }
    }

    void testVipUseForWhenAttachPortForwarding(){
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        VipInventory vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        }

        def vm = env.inventoryByName("vm-pf") as VmInstanceInventory
        PortForwardingRuleInventory pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        /* attach & detach will not change vip usefor */
        attachPortForwardingRule {
            ruleUuid = pf1Inv.uuid
            vmNicUuid = vm.getVmNics().get(0).uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        detachPortForwardingRule {
            uuid = pf1Inv.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        /* simulate install pf failed */
        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            rsp.setSuccess(false)
            rsp.setError("on purpose")
            return rsp
        }

        boolean s = false
        try {
            attachPortForwardingRule {
                ruleUuid = pf1Inv.uuid
                vmNicUuid = vm.getVmNics().get(0).uuid
            }
        } catch (AssertionError e) {
            s = true
        }
        assert s
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            return rsp
        }

        deleteVip {
            uuid = vip.getUuid()
        }
    }

    void testVipUseForWhenVmStopStart(){
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        VipInventory vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        }

        def vm = env.inventoryByName("vm-pf") as VmInstanceInventory
        PortForwardingRuleInventory pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.vmNics.get(0).uuid
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        /* attach & detach will not change vip usefor */
        stopVmInstance {
            uuid = vm.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        startVmInstance {
            uuid = vm.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        /* simulate install pf failed */
        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            rsp.setSuccess(false)
            rsp.setError("on purpose")
            return rsp
        }

        boolean s = false
        try {
            stopVmInstance {
                uuid = vm.uuid
            }
            startVmInstance {
                uuid = vm.uuid
            }
        } catch (AssertionError e) {
            s = true
        }
        assert s
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        env.simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { HttpEntity<String> e,EnvSpec spec ->
            def rsp = new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            return rsp
        }

        deletePortForwardingRule {
            uuid = pf1Inv.uuid
        }

        deleteVip {
            uuid = vip.getUuid()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
