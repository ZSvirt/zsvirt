package org.zstack.test.integration.storage.primary.local

import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.ExpungeDataVolumeAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageSystemTags
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by lining on 2017/3/3.
 */
class LocalStorageRecalculateAvailableCapacityCase extends SubCase{
    EnvSpec env

    long volumeBitSize = SizeUnit.GIGABYTE.toByte(10)

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            diskOffering {
                name = "diskOffering"
                diskSize = volumeBitSize
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    architecture = "x86_64"
                    url = "http://zstack.org/download/test.qcow2"
                    virtio = true
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
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
                virtio = true
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testPSAvailableCapacity()
        }
    }

    /**
     * Test Case: test the effect of creating (deleted) cloud disk on available capacity of primary storage
     *
     * Test action:
     * 1.Build PS, host, VM
     * 2.Create a 10GB cloud disk
     * 3.Check primary storage available capacity
     * 4.Delete cloud disk
     * 5.Check primary storage available capacity
     * 6.Reconnect primary storage
     * 7.Check primary storage available capacity
     * 8.Completely remove the cloud disk
     * 9.Check primary storage available capacity
     */
    void testPSAvailableCapacity(){

        // 1.Build PS, host, VM by default
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        KVMHostSpec kvmHostSpec = env.specByName("kvm")


        // 2.Create a 10GB cloud disk
        String localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, kvmHostSpec.inventory.uuid))
        )
        DiskOfferingSpec diskOfferingSpec = env.specByName("diskOffering")
        VolumeInventory volumeInventory = createDataVolume{
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            name = "dataVolume"
            systemTags = [localStorageSystemTag]
            diskOfferingUuid = diskOfferingSpec.inventory.uuid
        }
        assert volumeInventory.uuid != null
        assert volumeInventory.size == volumeBitSize


        // 3.Check primary storage available capacity
        PrimaryStorageInventory primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 4.Delete cloud disk
        deleteDataVolume{
            uuid = volumeInventory.uuid
        }


        // 5.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 6.Reconnect primary storage
        primaryStorageInventory = reconnectPrimaryStorage{
            uuid = primaryStorageInventory.uuid
        }
        assert PrimaryStorageState.Enabled.name() == primaryStorageInventory.state
        assert PrimaryStorageStatus.Connected.name() == primaryStorageInventory.status


        // 7.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert volumeBitSize == primaryStorageInventory.totalCapacity - primaryStorageInventory.availableCapacity


        // 8.Completely remove the cloud disk
        ExpungeDataVolumeAction expungeDataVolumeAction = new ExpungeDataVolumeAction(
                uuid: volumeInventory.uuid,
                sessionId: Test.currentEnvSpec.session.uuid
        )
        ExpungeDataVolumeAction.Result expungeDataVolumeActionResult = expungeDataVolumeAction.call()
        assert null == expungeDataVolumeActionResult.error


        // 9.Check primary storage available capacity
        primaryStorageInventory =  queryPrimaryStorage {
                conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert primaryStorageInventory.totalCapacity == primaryStorageInventory.availableCapacity
    }

    @Override
    void clean() {
        env.delete()
    }
}
