package org.zstack.test.integration.networkservice.provider

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-7.
 */
class VirtualRouterCreateNetworkSegmentOverlapsCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.MEGABYTE.toByte(512)
                cpu = 5
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
                    name = "vr-image"
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
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalCpu = 2
                        totalMem = SizeUnit.GIGABYTE.toByte(4)
                    }

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 2
                        totalMem = SizeUnit.GIGABYTE.toByte(4)
                    }

                    kvm {
                        name = "kvm-3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                        totalCpu = 2
                        totalMem = SizeUnit.GIGABYTE.toByte(4)
                    }

                    kvm {
                        name = "kvm-4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                        totalCpu = 2
                        totalMem = SizeUnit.GIGABYTE.toByte(4)
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
                        name = "pubL3-1"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3-2"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vr-1"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr-image")
                    isDefault = true
                }

                virtualRouterOffering {
                    name = "vr-2"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-2")
                    usePublicL3Network("pubL3-2")
                    useImage("vr-image")
                }
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            testCreateVirtualRouterVm()
            testCreateVirtualRouterVmNetworkSegmentOverlaps()
        }
    }

    void testCreateVirtualRouterVm(){
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def vrOffer2 = env.inventoryByName("vr-2") as VirtualRouterOfferingInventory
        
        deleteInstanceOffering {
            uuid = vrOffer2.uuid
        }
        createVmInstance {
            name = "vm-1"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3nw.uuid]
        }

        retryInSecs{
            assert dbf.count(ApplianceVmVO.class) == 1
        }
    }

    void testCreateVirtualRouterVmNetworkSegmentOverlaps(){
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def vrOffer1 = env.inventoryByName("vr-1") as VirtualRouterOfferingInventory
        def vr = dbf.listAll(ApplianceVmVO.class).get(0) as ApplianceVmVO
        env.recreate("vr-2")

        deleteInstanceOffering {
            uuid = vrOffer1.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }
        retryInSecs{
            assert dbf.count(ApplianceVmVO.class) == 0
        }

        expect(AssertionError.class){
            createVmInstance {
                name = "vm-2"
                instanceOfferingUuid = offer.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3nw.uuid]
            }
        }
        retryInSecs{
            assert dbf.count(ApplianceVmVO.class) == 0
        }

    }

    @Override
    void clean() {
        env.delete()
    }
}
