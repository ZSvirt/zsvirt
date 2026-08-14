package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.StartVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
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
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3")
                useDiskOfferings("diskOffering")
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
        CreateVmInstanceAction a = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: vm.instanceOfferingUuid,
                imageUuid: vm.imageUuid,
                l3NetworkUuids: [vm.defaultL3NetworkUuid],
                sessionId: currentEnvSpec.session.uuid
        )
        // can't checkout concrete content of error message
        // assert a.call().error.details.indexOf("no LocalStorage primary storage") > 0
        assert a.call().error == null

        StartVmInstanceAction startVmInstanceAction = new StartVmInstanceAction(
                uuid: vm1.uuid,
                sessionId: Test.currentEnvSpec.session.uuid
        )

        // startVmInstanceAction.call().error.details.indexOf("volume stored location primary storage is in a state of maintenance") > 0
        assert startVmInstanceAction.call().error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
