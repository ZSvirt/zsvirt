package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by heathhose on 17-5-10.
 */
class DoubleVirtualRouterVmGetCandidateVmNicsCase extends SubCase{

    EnvSpec env

    VmInstanceInventory vm
    VmInstanceInventory vm2
    LoadBalancerListenerInventory listener

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
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
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
                        name = "pubL3-1"
                        category = "Public"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                    l3Network {
                        name = "manageL3-1"

                        ip {
                            startIp = "11.167.100.10"
                            endIp = "11.167.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.167.100.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"

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
                        name = "pubL3-2"

                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }

                    l3Network {
                        name = "manageL3-2"

                        ip {
                            startIp = "11.167.200.10"
                            endIp = "11.167.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.167.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vr1"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    usePublicL3Network("pubL3-1")
                    useManagementL3Network("manageL3-1")
                    useImage("vr")
                }

                virtualRouterOffering {
                    name = "vr2"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    usePublicL3Network("pubL3-2")
                    useManagementL3Network("manageL3-2")
                    useImage("vr")
                }

                lb {
                    name = "lb"
                    useVip("pubL3-1")
                }
            }

        }
    }

    @Override
    void test() {
        env.create {
            testCreateDoubleVmDoubleVirtualRouterVm()
            testAddWrongVmNicToLoadBalancer()
            testGetCandidateVmNicsForLoadBalancer()
        }
    }

    void testCreateDoubleVmDoubleVirtualRouterVm(){
        def image = env.inventoryByName("image") as ImageInventory
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def offer1 = env.inventoryByName("vr1") as InstanceOfferingInventory
        def offer2 = env.inventoryByName("vr2") as InstanceOfferingInventory
        def l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        def l32 = env.inventoryByName("l3-2") as L3NetworkInventory
        def host1 = env.inventoryByName("kvm1") as HostInventory
        def host2  = env.inventoryByName("kvm2") as HostInventory

        createSystemTag {
            resourceUuid = offer1.uuid
            resourceType = "InstanceOfferingVO"
            tag = "guestL3Network::${l31.uuid}".toString()
        }

        createSystemTag {
            resourceUuid = offer2.uuid
            resourceType = "InstanceOfferingVO"
            tag = "guestL3Network::${l32.uuid}".toString()
        }

        vm = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l31.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host1.uuid
        } as VmInstanceInventory

        vm2 = createVmInstance {
            name = "vm2"
            imageUuid = image.uuid
            l3NetworkUuids = [l32.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host2.uuid
        } as VmInstanceInventory

        retryInSecs{
            assert dbFindByUuid(vm.uuid,VmInstanceVO.class) != null
            assert dbFindByUuid(vm2.uuid,VmInstanceVO.class) != null
        }


    }

    void testGetCandidateVmNicsForLoadBalancer(){
        def l31 = env.inventoryByName("l3-1") as L3NetworkInventory

        def vmNics = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.getUuid()
        } as List<VmNicInventory>

        assert vmNics.size() == 1
        assert vmNics.get(0).getL3NetworkUuid() == l31.uuid

        def l3Networks = getCandidateL3NetworksForLoadBalancer {
            listenerUuid = listener.getUuid()
        } as List<L3NetworkInventory>
        assert l3Networks.size() == 1
        assert l3Networks.get(0).getUuid() == l31.uuid

        destroyVmInstance {
            uuid = vm.uuid
        }
        vmNics = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.getUuid()
        } as List<VmNicInventory>
        assert vmNics.size() == 0

        l3Networks = getCandidateL3NetworksForLoadBalancer {
            listenerUuid = listener.getUuid()
        } as List<L3NetworkInventory>
        assert l3Networks.size() == 1
        assert l3Networks.get(0).getUuid() == l31.uuid

        destroyVmInstance {
            uuid = vm2.uuid
        }
        vmNics = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.getUuid()
        } as List<VmNicInventory>
        assert vmNics.size() == 0
    }

    void testAddWrongVmNicToLoadBalancer(){
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        listener  = createLoadBalancerListener {
            name = "listener"
            protocol = "tcp"
            loadBalancerPort = 22
            instancePort = 22
            loadBalancerUuid = lb.uuid
        } as LoadBalancerListenerInventory

        def vm2 = queryVmInstance { conditions = ["name=vm2"] } as List<VmInstanceInventory>

        expect(AssertionError.class){
            addVmNicToLoadBalancer {
                vmNicUuids = [vm2.get(0).getVmNics().get(0).getUuid()]
                listenerUuid = listener.uuid
            }
        }

        def vip = dbFindByUuid(lb.getVipUuid(),VipVO.class) as VipVO
        assert (vip.getPeerL3NetworkUuids() == null || vip.getPeerL3NetworkUuids().isEmpty())
    }

    @Override
    void clean() {
        env.delete()
    }
}
