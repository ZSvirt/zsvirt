package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2017/10/10.
 */
class ResumePrimaryStorageCapacityAfterCreateVmFailCase extends SubCase {
    EnvSpec env

    long psTotalSize = gb(5) - 1
    long volumeSize = gb(1)

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

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
                    totalCapacity = psTotalSize
                    availableCapacity = psTotalSize
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
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVmFailed()
        }
    }

    void testCreateVmFailed() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.updateValue(0)

        def ps = env.inventoryByName("local") as PrimaryStorageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def bs = env.inventoryByName("sftp") as BackupStorageInventory

        def image_virtual_size = volumeSize
        def image_physical_size = volumeSize
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            def bsSpec = spec.specByUuid(cmd.uuid) as BackupStorageSpec

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size 
            rsp.availableCapacity = bsSpec.availableCapacity
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }
        def sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        } as ImageInventory

        def beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult
        assert psTotalSize == beforeCapacityResult.availableCapacity

        createVmInstance {
            delegate.name = "vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.imageUuid = sizedImage.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    boot : true,
                    size : volumeSize,
                ],
                [
                    size : volumeSize,
                ],
            ]
        } as VmInstanceInventory

        def capacityResultAfterCreateOneVm = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult
        assert psTotalSize - volumeSize * 2 - image_virtual_size  == capacityResultAfterCreateOneVm.availableCapacity

        for (int i = 0; i < 10; i++) {
            expectApiFailure({
                createVmInstance {
                    delegate.name = "vm2"
                    delegate.cpuNum = 4
                    delegate.memorySize = gb(8)
                    delegate.imageUuid = sizedImage.uuid
                    delegate.l3NetworkUuids = [l3.uuid]
                    delegate.diskAOs = [
                        [
                            boot : true,
                            size : volumeSize,
                        ],
                        [
                            size : volumeSize,
                        ],
                    ]
                }
            }) {
                assert delegate.code == "SYS.1006"
            }

            retryInSecs(3){
                def capacityResult = getPrimaryStorageCapacity {
                    primaryStorageUuids = [ps.uuid]
                } as GetPrimaryStorageCapacityResult
                assert psTotalSize - volumeSize * 2 - image_virtual_size  == capacityResult.availableCapacity
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
