package org.zstack.test.integration.image.platform

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.backup.DownloadImageReply
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin on 2018/03/21.
 */
class AddImageToCephBackStorageCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VmInstanceInventory vm
    BackupStorageInventory bs

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
                attachBackupStorage("ceph-bk")
                cephBackupStorage {
                    name = "ceph-bk"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "/bk"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            bs = env.inventoryByName("ceph-bk") as BackupStorageInventory
            testAddQcowImage()
            testAddImageFromSftp()
            testAddImageFromFtp()
        }
    }

    void testAddQcowImage() {


        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def reply = new DownloadImageReply()
            reply.format = ImageConstant.RAW_FORMAT_STRING
            return reply
        }

        def u = "http://192.168.1.1/vm-snapshot.qcow2"
        ImageInventory img = addImage {
            name = "vm-snapshot"
            url = u
            format = "iso"
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        }

        assert img.format == ImageConstant.RAW_FORMAT_STRING
        assert img.url == u
    }

    void testAddImageFromSftp(){
        ImageInventory img = addImage {
            name = "vm-snapshot"
            url = "sftp://root:password@192.168.1.1/vm-snapshot.qcow2"
            format = "iso"
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert img.url.indexOf("password") == -1

    }

    void testAddImageFromFtp(){
        ImageInventory img = addImage {
            name = "vm-snapshot"
            url = "ftp://root:password@192.168.1.1/vm-snapshot.qcow2"
            format = "iso"
            mediaType = "RootVolumeTemplate"
            system = false
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        assert img.url.indexOf("password") == -1
    }

    @Override
    void clean() {
        env.delete()
    }

}
