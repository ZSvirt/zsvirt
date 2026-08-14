package org.zstack.test.integration.network.l3network

import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.sdk.AttachL3NetworkToVmAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class AttachL3NetWorkToVmCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
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
                        name = "pubL3"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }
                    }

                    l3Network {
                        name = "pubL3-no-dhcp"
                        category = "Public"
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            /**
             * 1. create vm with no ipRange L3
             * 2. add ipRange on L3 and create vm
             * 3. attach L3 with no ipRange to vm
             **/
            createVmWithL3NoIpRange()
            addIpRangeAndCreateVm()
            attachNoIpRangeL3ToVM()
        }
    }

    void createVmWithL3NoIpRange() {
        def image = env.inventoryByName("image1") as ImageInventory
        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        def noDhcpL3 = env.inventoryByName("pubL3-no-dhcp") as L3NetworkInventory

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                cpuNum = 4
                memorySize = gb(8)
                imageUuid = image.uuid
                l3NetworkUuids = [pubL3.uuid]
            }
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                cpuNum = 4
                memorySize = gb(8)
                imageUuid = image.uuid
                l3NetworkUuids = [noDhcpL3.uuid]
            }
        }
    }

    void addIpRangeAndCreateVm() {
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        addIpRange {
            name = "ipr-4"
            l3NetworkUuid = pubL3.getUuid()
            startIp = "192.168.101.101"
            endIp = "192.168.101.200"
            gateway = "192.168.101.1"
            netmask = "255.255.255.0"
        }

        vm = createVmInstance {
            name = "vm"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3.uuid]
        } as VmInstanceInventory
    }

    void attachNoIpRangeL3ToVM() {
        L2NetworkInventory l2Inv = env.inventoryByName("l2")

        def l3 = createL3Network {
            name = "L3WithNoIpRange"
            l2NetworkUuid = l2Inv.uuid
        } as L3NetworkInventory

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ['Flat':['DHCP']]
        }

        AttachL3NetworkToVmAction action = new AttachL3NetworkToVmAction()
        action.l3NetworkUuid = l3.uuid
        action.vmInstanceUuid = vm.uuid
        action.sessionId = adminSession()

        AttachL3NetworkToVmAction.Result ret = action.call()
        assert ret.error != null
    }
}
