package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
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
import org.zstack.sdk.EipInventory
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.header.vm.VmInstanceState
/**
 * Created by boce on 07-22-21.
 * This case will simulate attach portForwarding rule with cidr successful then failed attach eip
 * Or attach eip successful and failed attach portForwarding rule with cidr
 */
class AttachPortForwardingToVnicWithEipCase extends SubCase{

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

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

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
            testAttachPortForwardingToVnicWithEip()
        }
    }

    void testAttachPortForwardingToVnicWithEip(){
        VmInstanceInventory vm1 = env.inventoryByName("vm1")
        L3NetworkInventory pub = env.inventoryByName("pubL3")

        def vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        } as VipInventory

        //PortForwarding Part

        PortForwardingRuleInventory pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            sessionId = adminSession()
            vipUuid = vip.getUuid()
            allowedCidr= "0.0.0.0/0"
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.uuid, pf1Inv.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE

        /* pause VM */
        pauseVmInstance {
            uuid = vm1.uuid
        }
        attachPortForwardingRule {
            ruleUuid = pf1Inv.uuid
            vmNicUuid  = vm1.getVmNics().get(0).uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                .eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.PORTFORWARDING_NETWORK_SERVICE_TYPE)
                .count() > 0
        /* resume vm */
        resumeVmInstance {
            uuid = vm1.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1

        //EIP Part

        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        boolean called = false
        env.simulator(VirtualRouterConstant.VR_CREATE_EIP) {
            called = true
            return new VirtualRouterCommands.CreateEipRsp()
        }

        //Could not attachEip when vmNic use portForwarding with allowedCidr
        expect(AssertionError.class) {
            attachEip {
                eipUuid = eipInv.uuid
                vmNicUuid = vm1.vmNics.get(0).uuid
            }
        }
        EipInventory eip = queryEip {conditions=["name=eip"]} [0]
        assert eip.vmNicUuid == null

        detachPortForwardingRule {
            uuid = pf1Inv.uuid
        }
        attachEip {
            eipUuid = eipInv.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }

        //Could not attach portForwarding with allowedCidr when vmNic use Eip
        expect(AssertionError.class) {
            attachPortForwardingRule {
                ruleUuid = pf1Inv.uuid
                vmNicUuid = vm1.getVmNics().get(0).uuid
            }
        }
        PortForwardingRuleInventory pf = queryPortForwardingRule {conditions=["name=pf-1"]} [0]
        assert pf.vmNicUuid == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
