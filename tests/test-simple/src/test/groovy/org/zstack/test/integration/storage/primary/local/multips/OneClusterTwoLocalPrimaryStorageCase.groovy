package org.zstack.test.integration.storage.primary.local.multips

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class OneClusterTwoLocalPrimaryStorageCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
                }

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                    size = SizeUnit.GIGABYTE.toByte(1)
                    actualSize = SizeUnit.GIGABYTE.toByte(1)
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
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(50)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(50)
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/localPS2"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(150)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(150)
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
                cpu = 1
                memoryGB(1)
                useImage("image1")
                useL3Networks("pubL3")
                useHost("kvm")
                disk {
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVm()
        }
    }

    void testCreateVm() {
        BackupStorageInventory bs = env.inventoryByName("sftp")
        L3NetworkInventory l3 = env.inventoryByName("pubL3")

        // local1 50
        PrimaryStorageInventory localPS1 = env.inventoryByName("local") as PrimaryStorageInventory

        // local2 150
        PrimaryStorageInventory localPS2 = env.inventoryByName("local2") as PrimaryStorageInventory

        def image_virtual_size = SizeUnit.GIGABYTE.toByte(100)
        def image_physical_size = SizeUnit.GIGABYTE.toByte(10)
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        expectError {
            createVmInstance {
                name = "new_vm"
                cpuNum = 1
                memorySize = gb(1)
                imageUuid = sizedImage.uuid
                l3NetworkUuids = [l3.uuid]
                primaryStorageUuidForRootVolume = localPS1.uuid
            }
        }

        createVmInstance {
            name = "new_vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = localPS2.uuid
        }
    }

}
