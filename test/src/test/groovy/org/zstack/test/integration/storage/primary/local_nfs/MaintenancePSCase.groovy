package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/6/21.
 */
class MaintenancePSCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

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
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.2:/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3")
                usePrimaryStorage("local")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3")
                usePrimaryStorage("nfs")
                disk {}
                disk {
                    usePrimaryStorage("nfs")
                    useDiskOffering("diskOffering")
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testVmStatusAfterMaintenancePS()
        }
    }

    void testVmStatusAfterMaintenancePS(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        PrimaryStorageInventory local = env.inventoryByName("local")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VmInstanceInventory vm1 = env.inventoryByName("vm1")

        assert vm.allVolumes[0] instanceof VolumeInventory
        assert vm.allVolumes[0].primaryStorageUuid == local.uuid
        assert vm1.allVolumes[0] instanceof VolumeInventory
        assert vm1.allVolumes[0].primaryStorageUuid == nfs.uuid

        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        }[0]
        assert vm.state == VmInstanceState.Running.toString()

        retryInSecs(2){
            vm1 = queryVmInstance {
                conditions=["uuid=${vm1.uuid}"]
            }[0]
            assert vm1.state == VmInstanceState.Stopped.toString()
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        retryInSecs(2){
            vm = queryVmInstance {
                conditions=["uuid=${vm.uuid}"]
            }[0]
            assert vm.state == VmInstanceState.Stopped.toString()
        }

        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        def vm2 = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = vm.instanceOfferingUuid
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
        } as VmInstanceInventory
        assert vm2.allVolumes[0] instanceof VolumeInventory
        assert vm2.allVolumes[0].primaryStorageUuid == nfs.uuid

        startVmInstance {
            uuid = vm1.uuid
        }

        expectApiFailure({
            startVmInstance {
                uuid = vm.uuid
            }
        }) {
            assert delegate.code == "VM.1001"
            // failed to start VM: preStartVm returned an ErrorCode (e.g. from PrimaryStorageManagerImpl)
            assert delegate.details.contains("primary storage is in a state of maintenance")
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
