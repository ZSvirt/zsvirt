package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by weiwang on 30/08/2017
 */
class GetAttachablePublicL3ForVRouterCase extends SubCase {
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

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l22")
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
                        category = "Public"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l22"
                    physicalInterface = "eth0"
                    vlan = 222

                    l3Network {
                        name = "l32"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.10.10"
                            endIp = "192.168.10.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.10.1"
                        }
                    }

                    l3Network {
                        name = "pubL32"
                        category = "Public"

                        ip {
                            startIp = "11.168.10.20"
                            endIp = "11.168.10.200"
                            netmask = "255.255.0.0"
                            gateway = "11.168.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr-image")
                }
            }

            vm {
                name = "vm-1"
                useImage("image")
                useInstanceOffering("instanceOffering")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetAttachablePublicL3ForVRouter()
        }
    }

    void testGetAttachablePublicL3ForVRouter() {
        ApplianceVmVO applianceVmVO = Q.New(ApplianceVmVO.class).list().get(0)
        List<L3NetworkInventory> inventories = getAttachablePublicL3ForVRouter {
            vmInstanceUuid = applianceVmVO.uuid
        }
        // NOTE(WeiW): l3 is not system, pubL3 is attached, pubL32 is overlapped
        assert inventories.size() == 1
        assert inventories.stream().filter{ inv -> inv.name.equals("l32") }.findFirst().present
    }
}
