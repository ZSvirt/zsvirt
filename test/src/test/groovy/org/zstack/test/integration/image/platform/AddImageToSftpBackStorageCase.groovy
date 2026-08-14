package org.zstack.test.integration.image.platform

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin on 2018/03/21.
 */
class AddImageToSftpBackStorageCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
                sftpBackupStorage {
                    name = "sftp"
                    url = "/sftp"
                    username = "username"
                    password = "password"
                    hostname = "hostname"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddQcowImage()
        }
    }

    void testAddQcowImage() {
        BackupStorageInventory bs = env.inventoryByName("sftp")

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.format = ImageConstant.QCOW2_FORMAT_STRING
            return rsp
        }

        ImageInventory img = addImage {
            name = "vm-snapshot"
            url = "http://192.168.1.1/vm-snapshot.qcow2"
            format = "iso"
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        }
        assert img.format == ImageConstant.QCOW2_FORMAT_STRING

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.format = ""
            return rsp
        }

        ImageInventory img_1 = addImage {
            name = "vm-snapshot-1"
            url = "http://192.168.1.1/vm-snapshot.qcow2"
            format = ImageConstant.QCOW2_FORMAT_STRING
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        }
        assert img_1.format == ImageConstant.QCOW2_FORMAT_STRING

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.format = null
            return rsp
        }

        ImageInventory img_2 = addImage {
            name = "vm-snapshot-2"
            url = "http://192.168.1.1/vm-snapshot.qcow2"
            format = ImageConstant.ISO_FORMAT_STRING
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        }
        assert img_2.format == ImageConstant.ISO_FORMAT_STRING
    }

    @Override
    void clean() {
        env.delete()
    }

}
