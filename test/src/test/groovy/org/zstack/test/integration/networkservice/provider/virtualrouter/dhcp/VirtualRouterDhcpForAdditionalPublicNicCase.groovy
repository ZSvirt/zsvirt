package org.zstack.test.integration.networkservice.provider.virtualrouter.dhcp

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by shixin.ruan on 2021/05/13.
 */
public class VirtualRouterDhcpForAdditionalPublicNicCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.getSpringSpec())
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
                    system = true
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
                                     NetworkServiceType.SNAT.toString(),
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

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "11.168.101.10"
                            endIp = "11.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.101.1"
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
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDhcpForAdditionalPublicNetwork()
        }
    }

    void testDhcpForAdditionalPublicNetwork() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory pubL3_1 = env.inventoryByName("pubL3-1") as L3NetworkInventory
        VirtualRouterVmInventory vr = queryVirtualRouterVm {} [0]

        VirtualRouterCommands.AddDhcpEntryCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_ADD_DHCP_PATH) { VirtualRouterCommands.AddDhcpEntryRsp rsp, HttpEntity<String> e ->
            cmd = json(e.body, VirtualRouterCommands.AddDhcpEntryCmd.class)
            return rsp
        }

        expect (AssertionError.class) {
            createVmInstance {
                name = "test"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [pubL3_1.uuid]
                defaultL3NetworkUuid = pubL3_1.uuid
            }
        }

        /* attach pubL3_1 to vr, vr will provide dhcp server for pubL3_1 */
        attachL3NetworkToVm {
            vmInstanceUuid = vr.uuid
            l3NetworkUuid = pubL3_1.uuid
        }

        createVmInstance {
            name = "test"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3_1.uuid]
            defaultL3NetworkUuid = pubL3_1.uuid
        }

        assert cmd != null
    }
}
