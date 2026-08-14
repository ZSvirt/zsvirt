package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

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
 * Created by tao.yang on Oct. 18, 2022.
 * This case will simulate attach 2 port forwarding rules with the same protocol type to same vm nic,
 * and one of them has AllowedCidr.
 */
class Attach2PortForwardingRulesToSameVmNicCase extends SubCase {

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
                        name = "l3-1"

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
                name = "vm1"
                useImage("image")
                useL3Networks("l3-1")
                useInstanceOffering("instanceOffering")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testAttachRuleWithPrivatePortRangeAndAllowedCidrToVm()
            testAttachRuleWithSoloPrivatePortAndAllowedCidrToVm()
        }
    }

    void testAttachRuleWithPrivatePortRangeAndAllowedCidrToVm(){
        def vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        def vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        } as VipInventory

        def pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 22
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
        } as PortForwardingRuleInventory
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.uuid, pf1Inv.getUuid())
                .select(VipNetworkServicesRefVO_.serviceType)
                .findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        def pf2Inv = createPortForwardingRule{
            name = "pf-2"
            vipPortStart = 10
            vipPortEnd = 20
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
            allowedCidr= "0.0.0.0/0"
        } as PortForwardingRuleInventory
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                .eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE)
                .count() == 2L
        
        attachPortForwardingRule {
            ruleUuid = pf1Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf1Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() != null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf2Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf2Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null

        detachPortForwardingRule {
            uuid = pf1Inv.uuid
        }
        //swap
        attachPortForwardingRule {
            ruleUuid = pf2Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf2Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() != null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf1Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf1Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null
        
        deleteVip {
            uuid = vip.uuid
        }
    }

    void testAttachRuleWithSoloPrivatePortAndAllowedCidrToVm(){
        def vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        def vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        } as VipInventory

        def pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 22
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
            allowedCidr= "0.0.0.0/0"
        } as PortForwardingRuleInventory
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.uuid, pf1Inv.getUuid())
                .select(VipNetworkServicesRefVO_.serviceType)
                .findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        def pf2Inv = createPortForwardingRule{
            name = "pf-2"
            vipPortStart = 10
            vipPortEnd = 20
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
        } as PortForwardingRuleInventory
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                .eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE)
                .count() == 2L

        def pf3Inv = createPortForwardingRule{
            name = "pf-3"
            vipPortStart = 33
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
        } as PortForwardingRuleInventory
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                .eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE)
                .count() == 3L

        attachPortForwardingRule {
            ruleUuid = pf1Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf1Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() != null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf2Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf2Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf3Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf3Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null

        detachPortForwardingRule {
            uuid = pf1Inv.uuid
        }
        //swap 1 and 2
        attachPortForwardingRule {
            ruleUuid = pf2Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf2Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() != null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf1Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf1Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null

        detachPortForwardingRule {
            uuid = pf2Inv.uuid
        }
        // swap 1 and 3
        attachPortForwardingRule {
            ruleUuid = pf3Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf3Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() != null

        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf1Inv.uuid
                vmNicUuid  = vm1.getVmNics().get(0).uuid
            }
        }
        assert Q.New(PortForwardingRuleVO.class)
                .eq(PortForwardingRuleVO_.uuid, pf1Inv.getUuid())
                .select(PortForwardingRuleVO_.vmNicUuid)
                .findValue() == null

    }

    @Override
    void clean() {
        env.delete()
    }
}