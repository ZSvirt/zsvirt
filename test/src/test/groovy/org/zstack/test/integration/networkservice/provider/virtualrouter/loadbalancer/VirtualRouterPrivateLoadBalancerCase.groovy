package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VipUseForList
import org.zstack.utils.data.SizeUnit
/**
 * @author: zhanyong.miao
 * @date: 2019-11-22
 * */
class VirtualRouterPrivateLoadBalancerCase extends SubCase{
    DatabaseFacade dbf
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

                lb {
                    name = "lb"
                    useVip("l3")
                }
            }

            vm {
                name = "vm1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm3"
                useImage("image")
                useL3Networks("l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            privateLoadBalancerCase()
        }
    }

    void privateLoadBalancerCase() {
        LoadBalancerInventory lb = env.inventoryByName("lb")

        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, lb.vipUuid).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, lb.vipUuid).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, lb.vipUuid).count() == 0

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 22
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        } as LoadBalancerListenerInventory

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 2

        addVmNicToLoadBalancer {
            vmNicUuids = [result[0].uuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, lb.vipUuid).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == 1
    }

    @Override
    void clean() {
        env.delete()
    }

}
