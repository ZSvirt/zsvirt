package org.zstack.test.integration.networkservice.provider.flat.eip


import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocateHostWithEipCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-pri")
                    attachL2Network("l2-pub")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-pri")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2-pri"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-pri"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "10.168.100.10"
                            endIp = "10.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "10.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-pub"
                    physicalInterface = "eth0"
                    vlan = 1001

                    l3Network {
                        name = "l3-pub"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "172.168.100.10"
                            endIp = "172.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "172.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testRestartVmWithEip()
        }
    }

    void testRestartVmWithEip() {
        HostInventory host1 = env.inventoryByName("host-1")
        HostInventory host2 = env.inventoryByName("host-2")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory pubL3 = env.inventoryByName("l3-pub")
        L3NetworkInventory l3 = env.inventoryByName("l3-pri")


        VmInstanceInventory vm = createVmInstance {
            name = "test"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        }
        VmNicInventory nic = vm.getVmNics().get(0)

        def vip = createVip {
            name = "vip"
            l3NetworkUuid = pubL3.uuid
        } as VipInventory
        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = nic.uuid
        } as EipInventory

        /* change host1 to maintain mode, l2-pub is not attached to host2
        * stop vm, then start vm will failed */
        changeHostState {
            uuid = host1.uuid
            stateEvent = "maintain"
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        expect (AssertionError.class) {
            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
