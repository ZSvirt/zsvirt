package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by weiwang on 20/11/2017
 */
class VirtualRouterOfferingDisabledCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr-image"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = VyosConstants.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString()]
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-2"
                        category = "Public"

                        ip {
                            startIp = "12.16.20.10"
                            endIp = "12.16.20.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.20.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"
                    vlan = 200
                }

                virtualRouterOffering {
                    name = "vr-offering-1"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    isDefault = true
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr-image")
                }

                virtualRouterOffering {
                    name = "vr-offering-2"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr-image")
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDefaultVirtualRouterOfferingDisabled()
        }
    }

    void testDefaultVirtualRouterOfferingDisabled() {
        def image = env.inventoryByName("image1") as ImageInventory
        def l3nw = env.inventoryByName("l3-1") as L3NetworkInventory
        def vrOffering1 = env.inventoryByName("vr-offering-1") as VirtualRouterOfferingInventory
        def vrOffering2 = env.inventoryByName("vr-offering-2") as VirtualRouterOfferingInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        VirtualRouterVmInventory vr

        def vm = createVmInstance {
            name = "test1"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3nw.uuid]
        } as VmInstanceInventory

        retryInSecs {
            vr = queryVirtualRouterVm {}[0]
            assert vr.instanceOfferingUuid == vrOffering1.uuid
        }

        destroyVmInstance { uuid=vr.uuid }
        changeInstanceOfferingState {
            uuid = vrOffering1.uuid
            stateEvent = "disable"
        }

        rebootVmInstance { uuid = vm.uuid }
        retryInSecs {
            vr = queryVirtualRouterVm {}[0]
            assert vr.instanceOfferingUuid == vrOffering2.uuid
        }
    }
}
