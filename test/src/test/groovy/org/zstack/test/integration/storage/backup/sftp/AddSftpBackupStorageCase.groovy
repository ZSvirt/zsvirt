package org.zstack.test.integration.storage.backup.sftp

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.ImageVO
import org.zstack.header.image.ImageVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.AddSftpBackupStorageAction
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 2017/4
 */
class AddSftpBackupStorageCase extends SubCase {

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
            addDevicePathBSFailure()
            testCreateTemplateWillRecordMetadate()
            testImportImageFlagWhenAddBS()
            testSmpPSWithSftpBS()
        }
    }

    void testSmpPSWithSftpBS() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def image = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def clusterInv = env.inventoryByName("cluster") as ClusterInventory
        def sftp = env.inventoryByName("sftp") as BackupStorageInventory

        env.simulator(KvmBackend.CONNECT_PATH) {
            def rsp = new KvmBackend.ConnectRsp()
            rsp.totalCapacity = SizeUnit.TERABYTE.toByte(100)
            rsp.availableCapacity = SizeUnit.TERABYTE.toByte(100)
            return rsp
        }

        def ps = addSharedMountPointPrimaryStorage {
            name = "test-smp"
            url = "/mount_point"
            zoneUuid = zone.uuid
        } as PrimaryStorageInventory

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = clusterInv.uuid
        }

        def vm = createVmInstance {
            name = "test_vm"
            cpuNum = 4
            memorySize = gb(8)
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            primaryStorageUuidForRootVolume = ps.uuid
        } as VmInstanceInventory

        def rootSnap = createVolumeSnapshot {
            name = "test-root-snap"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        createRootVolumeTemplateFromVolumeSnapshot {
            name = "test-root-volume-template"
            snapshotUuid = rootSnap.uuid
            backupStorageUuids = [sftp.uuid]
        }
    }

    void testCreateTemplateWillRecordMetadate() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        def called = false
        env.afterSimulator(SftpBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE) { rsp, HttpEntity<String> e ->
            called = true
            return new SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp()
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        createRootVolumeTemplateFromRootVolume {
            name = "image-name"
            rootVolumeUuid = vm.rootVolumeUuid
        } as ImageInventory

        retryInSecs {
            assert called
        }
    }

    void testImportImageFlagWhenAddBS() {
        def imageUuid = "a603e80ea18f424f8a5f00371d484537"

        def image = env.inventoryByName("image1") as ImageInventory

        // same uuid occurs in metadata will be filtered out
        // when image eo still remained in db, just recover image from its eo
        env.simulator(SftpBackupStorageConstant.GET_IMAGES_METADATA) {
            def rsp = new SftpBackupStorageCommands.GetImagesMetaDataRsp()
            rsp.imagesMetaData = "{\"uuid\":\"${imageUuid}\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}" +
                    "\n\n{\"uuid\":\"${imageUuid}\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}" +
                    "\n\n{\"uuid\":\"${image.uuid}\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}"

            return rsp
        }

        env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }

        addSftpBackupStorage {
            name = "imagestore"
            description = "desc"
            username = "username"
            password = "password"
            hostname = "hostname"
            url = "/data"
            importImages = true
        }

        retryInSecs {
            assert Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).isExists()
            assert Q.New(ImageVO.class).eq(ImageVO_.uuid, image.uuid).isExists()
        }

        image = queryImage {
            conditions = ["uuid=${image.uuid}"]
        }[0] as ImageInventory

        // check bs ref is recovered
        assert image.backupStorageRefs.size() == 1
    }

    void addDevicePathBSFailure() {
        AddSftpBackupStorageAction action = new AddSftpBackupStorageAction()
        action.name = "sftp"
        action.url = "/dev/sftp"
        action.username = "root"
        action.password = "password"
        action.hostname = "192.168.0.3"
        action.sessionId = adminSession()
        AddSftpBackupStorageAction.Result res = action.call()
        assert res.error != null
        action.url = "/proc/xx"
        res = action.call()
        assert res.error != null
        action.url = "/sys/test"
        res = action.call()
        assert res.error != null
    }
}
