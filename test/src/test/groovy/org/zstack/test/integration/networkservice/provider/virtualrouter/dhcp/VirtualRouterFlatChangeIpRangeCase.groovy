package org.zstack.test.integration.networkservice.provider.virtualrouter.dhcp

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin.ruan on 2018/04/18.
 */
class VirtualRouterFlatChangeIpRangeCase extends SubCase {
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
                        totalCpu = 100
                        totalMem = SizeUnit.GIGABYTE.toByte(1024)
                    }

                    attachPrimaryStorage("local")
                    attachBackupStorage("sftp")
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
                            types = [NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l32"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     NetworkServiceType.DHCP.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
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

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testChangeIpRange()
            testDeleteIpRange()
        }
    }

    void testChangeIpRange() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        L3NetworkInventory l32 = env.inventoryByName("l32") as L3NetworkInventory

        for (int i = 0; i < 8; i++) {
            createVmInstance {
                name = String.format("test-%s",i.toString())
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                defaultL3NetworkUuid = l3.uuid
                l3NetworkUuids = [l3.uuid, l32.uuid]
            }
        }

        IpRangeInventory ipr = l3.ipRanges.get(0)
        deleteIpRange {
            uuid = ipr.uuid
        }

        for (int i = 0; i < 8; i++) {
            def vm = queryVmInstance { conditions=[String.format("name=test-%s", i.toString())]}[0] as VmInstanceInventory
            assert vm.vmNics.size() == 1
            assert vm.state == VmInstanceState.Running.toString()
        }
    }

    void testDeleteIpRange() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l32 = env.inventoryByName("l32") as L3NetworkInventory

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            defaultL3NetworkUuid = l32.uuid
            l3NetworkUuids = [l32.uuid]
        }
        String usedIpUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vm1.getVmNics().get(0).getUuid())

        deleteIpRange {
            uuid = l32.getIpRanges().get(0).getUuid()
        }

        vm1 = queryVmInstance {conditions=["name=vm-1"]}[0]
        assert vm1.getVmNics().size() == 0
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).isExists()

        /* test stopped vm */
        addIpRange {
            name = "TestIpRange_2"
            l3NetworkUuid = l32.getUuid()
            startIp = "192.168.101.10"
            endIp = "192.168.101.100"
            netmask = "255.255.255.0"
            gateway = "192.168.101.1"
        }

        vm1 = attachL3NetworkToVm {
            l3NetworkUuid = l32.uuid
            vmInstanceUuid = vm1.uuid
        }
        assert vm1.getVmNics().size() == 1
        usedIpUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vm1.getVmNics().get(0).getUuid())

        stopVmInstance {
            uuid = vm1.uuid
        }

        l32 = queryL3Network {conditions=["name=l32"]} [0]
        deleteIpRange {
            uuid = l32.getIpRanges().get(0).getUuid()
        }

        vm1 = queryVmInstance {conditions=["name=vm-1"]}[0]
        assert vm1.getVmNics().size() == 0
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).isExists()

        /* test paused vm */
        addIpRange {
            name = "TestIpRange_3"
            l3NetworkUuid = l32.getUuid()
            startIp = "192.168.101.10"
            endIp = "192.168.101.100"
            netmask = "255.255.255.0"
            gateway = "192.168.101.1"
        }

        vm1 = attachL3NetworkToVm {
            l3NetworkUuid = l32.uuid
            vmInstanceUuid = vm1.uuid
        }
        assert vm1.getVmNics().size() == 1
        usedIpUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vm1.getVmNics().get(0).getUuid())

        startVmInstance {
            uuid = vm1.uuid
        }

        pauseVmInstance {
            uuid = vm1.uuid
        }

        l32 = queryL3Network {conditions=["name=l32"]} [0]
        deleteIpRange {
            uuid = l32.getIpRanges().get(0).getUuid()
        }

        vm1 = queryVmInstance {conditions=["name=vm-1"]}[0]
        assert vm1.getVmNics().size() == 0
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).isExists()
    }
}
