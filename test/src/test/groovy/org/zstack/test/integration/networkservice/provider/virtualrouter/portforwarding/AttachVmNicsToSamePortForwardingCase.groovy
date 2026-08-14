package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.ReconnectConsoleProxyAgentAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-17.
 */
class AttachVmNicsToSamePortForwardingCase extends SubCase{

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
                        name = "l3-2"

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
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
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

                portForwarding {
                    name = "pf-1"
                    vipPortStart = 22
                    privatePortStart = 22
                    protocolType = "TCP"
                    useVip("pubL3")
                }

                portForwarding {
                    name = "pf-2"
                    vipPortStart = 33
                    privatePortStart = 33
                    protocolType = "TCP"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3-1","l3-2")
                useDefaultL3Network("l3-1")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetAttachVmNicsToSamePortForwarding()
        }
    }

    void testGetAttachVmNicsToSamePortForwarding(){
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def pf = env.inventoryByName("pf-1") as PortForwardingRuleInventory
        def pf2 = env.inventoryByName("pf-2") as PortForwardingRuleInventory
        def vmnics = getPortForwardingAttachableVmNics {
            ruleUuid = pf.uuid
        } as List<VmNicInventory>
        assert !vmnics.isEmpty()

        def cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING){rsp, HttpEntity<String> entity ->
            cmd = json(entity.body,VirtualRouterCommands.CreatePortForwardingRuleCmd.class);
            return rsp
        }
        attachPortForwardingRule {
            ruleUuid = pf.uuid
            vmNicUuid  = vm.getVmNics().get(0).uuid
        }
        assert cmd != null
        assert dbFindByUuid(pf.uuid,PortForwardingRuleVO.class).getVmNicUuid() == vm.getVmNics().get(0).uuid

        //vm has two vmnic but cannot be attached to port forwarding
        vmnics = getPortForwardingAttachableVmNics {
            ruleUuid = pf2.uuid
        } as List<VmNicInventory>
        assert vmnics.isEmpty()

    }
    @Override
    void clean() {
        env.delete()
    }
}
