package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.BackupStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList

class CreateVmWhenPSCapacityLessOrGreaterImageAndRootVolumeSizeCase extends SubCase {
    EnvSpec env


    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "50G"
                    url  = "http://zstack.org/download/test.iso"
                    platform = ImagePlatform.Linux.toString()
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                    format = "iso"
                    size = SizeUnit.GIGABYTE.toByte(100)
                    actualSize = SizeUnit.GIGABYTE.toByte(50)
                }

                image {
                    name = "10G"
                    url  = "http://zstack.org/download/test2.iso"
                    platform = ImagePlatform.Linux.toString()
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                    format = "iso"
                    size = SizeUnit.GIGABYTE.toByte(20)
                    actualSize = SizeUnit.GIGABYTE.toByte(10)
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
                    availableCapacity = SizeUnit.GIGABYTE.toByte(60)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(60)
                }


                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
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
            testCreateVmWhenPSCapacityLessThanImageAndRootVolumeSuccess()
            testCreateVmWhenPSCapacityGreaterThanImageAndRootVolumeFailure()
        }
    }

    void testCreateVmWhenPSCapacityLessThanImageAndRootVolumeSuccess() {
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            def bsSpec = spec.specByUuid(cmd.uuid) as BackupStorageSpec

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(20)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(10)
            rsp.availableCapacity = bsSpec.availableCapacity - SizeUnit.GIGABYTE.toByte(10)
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        def _10GImage = addImage {
            backupStorageUuids = asList(bs.uuid)
            name = "10G"
            url = "http://some-site/static/image.iso"
            format = "iso"
        } as ImageInventory

        createVmInstance {
            delegate.name = "test"
            delegate.imageUuid = _10GImage.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.diskAOs = [
                [
                    boot : true,
                    size : gb(20)
                ],
            ]
        }
    }

    void testCreateVmWhenPSCapacityGreaterThanImageAndRootVolumeFailure() {
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            def bsSpec = spec.specByUuid(cmd.uuid) as BackupStorageSpec

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(100)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(50)
            rsp.availableCapacity = bsSpec.availableCapacity - SizeUnit.GIGABYTE.toByte(50)
            rsp.totalCapacity = bsSpec.totalCapacity
            return rsp
        }

        def _50GImage = addImage {
            backupStorageUuids = asList(bs.uuid)
            name = "50G"
            url = "http://some-site/static/image.iso"
            format = "iso"
        } as ImageInventory

        expectApiFailure({
            createVmInstance {
                delegate.name = "test"
                delegate.imageUuid = _50GImage.uuid
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.cpuNum = 1
                delegate.memorySize = gb(1)
                delegate.diskAOs = [
                    [
                        boot : true,
                        size : gb(20)
                    ],
                ]
            }
        }) {
            assert delegate.code == "HOST_ALLOCATION.1001"
            assert delegate.opaque
            assert delegate.opaque["rejectedCandidates"] instanceof List
            assert (delegate.opaque["rejectedCandidates"] as List).size() == 1
            assert (delegate.opaque["rejectedCandidates"] as List)[0] instanceof Map
            assert ((delegate.opaque["rejectedCandidates"] as List)[0] as Map)["reject"] == "no capable primary storage for new-created VM"
            assert ((delegate.opaque["rejectedCandidates"] as List)[0] as Map)["rejectBy"] == "HostPrimaryStorageAllocatorFlow"
        }
    }
}
