package org.zstack.test.integration.storage.backup.sftp

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.image.SyncImageSizeMsg
import org.zstack.header.image.SyncImageSizeReply
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageMsg
import org.zstack.header.storage.backup.SyncImageSizeOnBackupStorageReply
import org.zstack.header.volume.SyncVolumeSizeMsg
import org.zstack.header.volume.SyncVolumeSizeReply
import org.zstack.header.volume.VolumeConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/8
 */
class SftpBackupStorageSyncImageSizeCase extends SubCase {

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
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

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

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            SftpBackupStorageSyncImageSize()
        }
    }

    void SftpBackupStorageSyncImageSize() {

        boolean called = false
        int IMAGE_SIZE = 555555555555
        int IMAGE_ACUTUALSIZE = 333333

        env.message(SyncImageSizeOnBackupStorageMsg.class){
            SyncImageSizeOnBackupStorageMsg msg, CloudBus bus ->
                SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply()
                reply.setActualSize(IMAGE_ACUTUALSIZE)
                reply.setSize(IMAGE_SIZE)
                bus.reply(msg, reply)
                called = true
        }
        VmInstanceInventory vm = env.inventoryByName("vm")

        stopVmInstance {
            uuid = vm.uuid
            sessionId = loginAsAdmin().uuid
        }

        long count = Q.New(TaskProgressVO.class).count()
        ImageInventory img = createRootVolumeTemplateFromRootVolume {
            name = "template"
            rootVolumeUuid = vm.getRootVolumeUuid()
            sessionId = loginAsAdmin().uuid
        }
        assert called
        assert count < Q.New(TaskProgressVO.class).count()
        assert IMAGE_ACUTUALSIZE== img.actualSize
        assert IMAGE_SIZE==img.size
    }
}
