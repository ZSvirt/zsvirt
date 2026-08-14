package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.EipInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class VirtualRouterPortforwardingAndEipCase extends SubCase {

    EnvSpec env
    VipInventory vip1
    VipInventory vip2
    EipInventory eip1
    VmInstanceInventory vm

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
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            def pub = env.inventoryByName("pubL3") as L3NetworkInventory

            vip1 = createVip {
                name = "vip-1"
                l3NetworkUuid = pub.uuid
            }

            vip2 = createVip {
                name = "vip-2"
                l3NetworkUuid = pub.uuid
            }

            eip1 = createEip {
                name =  "eip-1"
                vipUuid = vip2.uuid
            }

            vm = env.inventoryByName("vm-1")

            testAttachEipOnVmAttachedPortforwarding()
            testAttachPortforwardingOnVmAttachedEip()
        }
    }

    void testAttachEipOnVmAttachedPortforwarding() {
        def pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            vipUuid = vip1.uuid
            vmNicUuid = (vm.vmNics[0] as VmNicInventory).uuid
        } as PortForwardingRuleInventory

        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = (vm.vmNics[0] as VmNicInventory).uuid
        }

        deletePortForwardingRule {
            uuid = pf1Inv.uuid
        }

        detachEip {
            uuid = eip1.uuid
        }
    }

    void testAttachPortforwardingOnVmAttachedEip() {
        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = (vm.vmNics[0] as VmNicInventory).uuid
        }

        def pf1Inv = createPortForwardingRule{
            name = "pf-1"
            vipPortStart = 11
            privatePortStart = 11
            protocolType = PortForwardingProtocolType.TCP.toString()
            vipUuid = vip1.uuid
            vmNicUuid = (vm.vmNics[0] as VmNicInventory).uuid
        } as PortForwardingRuleInventory

        deletePortForwardingRule {
            uuid = pf1Inv.uuid
        }

        detachEip {
            uuid = eip1.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
