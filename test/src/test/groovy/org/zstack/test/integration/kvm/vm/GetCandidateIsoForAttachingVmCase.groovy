package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.image.ImageBackupStorageRefVO
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImageStatus
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.AttachIsoToVmInstanceAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/5/29.
 */
class GetCandidateIsoForAttachingVmCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
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
                    name = "iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url  = "http://zstack.org/download/test.iso"
                }

                image {
                    name = "iso_1"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url  = "http://zstack.org/download/test.iso"
                }

                image {
                    name = "image-test"
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                    url = "http://zstack.org/download/image_test.qcow2"
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
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            runTest()
        }
    }

    void runTest() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        BackupStorageInventory bs = env.inventoryByName("sftp")
        ZoneInventory zone = env.inventoryByName("zone")
        ImageInventory testImage = env.inventoryByName("image-test")

        AttachIsoToVmInstanceAction testvalidate = new AttachIsoToVmInstanceAction()
        testvalidate.isoUuid = testImage.uuid
        testvalidate.vmInstanceUuid = vm.uuid
        testvalidate.sessionId = adminSession()
        AttachIsoToVmInstanceAction.Result validateRes = testvalidate.call()
        assert validateRes.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()

        List<ImageInventory> isoList = getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }
        assert 2 == isoList.size()

        ImageInventory iso = isoList.get(0)
        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        isoList = getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }
        assert 1 == isoList.size()
        assert iso.uuid != isoList.get(0).uuid

       env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }

        def bs2 = addSftpBackupStorage {
            name = "sftp2"
            description = "test"
            username = "username"
            password = "foobar"
            hostname = "127.0.0.22"
            url = "/sftp2"
        }

        attachBackupStorageToZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs2.uuid
        }

        DatabaseFacade dbf = bean(DatabaseFacade.class)
        ImageBackupStorageRefVO refVO = new ImageBackupStorageRefVO()
        refVO.imageUuid = isoList.get(0).uuid
        refVO.backupStorageUuid = bs2.uuid
        refVO.status = ImageStatus.Ready
        refVO.installPath = "/sftp2/image2.iso"
        refVO = dbf.persistAndRefresh(refVO)

        isoList = getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }
        assert 1 == isoList.size()

        detachBackupStorageFromZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs2.uuid
        }

        deleteBackupStorage {
            uuid = bs2.uuid
        }

        detachBackupStorageFromZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs.uuid
        }

        assert 0 == getCandidateIsoForAttachingVm {
            vmInstanceUuid = vm.uuid
        }.size()

    }

    @Override
    void clean() {
        env.delete()
    }
}
