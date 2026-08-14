package org.zstack.test.integration.storage.primary.nfs.imagecleaner.imagecache

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SimpleQuery
import org.zstack.header.image.ImageDeletionPolicyManager
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.image.ImageGlobalConfig
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil
/**
 * 1. two NFS storage running two VMs with the same image
 * 2. delete the image and two VMs
 * 3. clean up image cache on the nfs
 * <p>
 * confirm the image cache of the nfs get cleaned up
 * confirm the image cache of the nfs1 doesn't get cleaned up
 */

/**
 * Created by lining on 2017/3/25.
 */
// base on TestNfsImageCleaner1
class CleanImageCacheOnPrimaryStorageCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf

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
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
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

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs1")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                nfsPrimaryStorage {
                    name = "nfs1"
                    url = "localhost:/nfs1"
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
                useCluster("cluster")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }


            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useCluster("cluster1")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDelete()
        }
    }

    void fastCloneVmBeforeDeletingImageCache() {
        def volume = createDataVolume {
            name = "test fast clone"
            diskOfferingUuid = env.inventoryByName("diskOffering").uuid
            primaryStorageUuid = env.inventoryByName("nfs").uuid
        } as VolumeInventory

        def sp = createVolumeSnapshot {
            name = "sp"
            volumeUuid = volume.uuid
        } as VolumeSnapshotInventory

        createDataVolumeFromVolumeSnapshot {
            name = "data-vol-from-sp"
            volumeSnapshotUuid = sp.uuid
            systemTags = [VolumeSystemTags.FAST_CREATE.tagFormat]
            primaryStorageUuid = env.inventoryByName("nfs").uuid
        }

        assert Q.New(VolumeSnapshotReferenceTreeVO.class).isNull(VolumeSnapshotReferenceTreeVO_.rootImageUuid).isExists()
    }

    void testDelete(){
        fastCloneVmBeforeDeletingImageCache()

        dbf = bean(DatabaseFacade.class)

        PrimaryStorageInventory nfs = env.inventoryByName("nfs")
        PrimaryStorageInventory nfs1 = env.inventoryByName("nfs1")

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        ImageInventory image1 = env.inventoryByName("image1")
        deleteImage {
            uuid = image1.uuid
        }

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class)
        q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image1.getUuid())
        q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, nfs.getUuid())
        ImageCacheVO c = q.find()

        def checked = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { rsp, HttpEntity<String> e ->
            NfsPrimaryStorageKVMBackendCommands.DeleteCmd cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.DeleteCmd.class)
            if(cmd.installPath.indexOf("imagecache") < 0 ) return rsp

            assert PathUtil.parentFolder(c.installUrl) == cmd.installPath
            checked = true
            return rsp
        }

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = env.inventoryByName("vm")
        destroyVmInstance {
            uuid = vm.uuid
        }
        VmInstanceInventory vm1 = env.inventoryByName("vm1")
        destroyVmInstance {
            uuid = vm1.uuid
        }

        def isFolder = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { rsp, HttpEntity<String> e ->
            NfsPrimaryStorageKVMBackendCommands.DeleteCmd cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.DeleteCmd.class)
            isFolder = cmd.folder
            return rsp
        }

        cleanUpImageCacheOnPrimaryStorage {
            uuid = nfs.uuid
        }

        retryInSecs {
            isFolder
        }
        q = dbf.createQuery(ImageCacheVO.class)
        q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image1.getUuid())
        q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, nfs1.getUuid())
        c = q.find()
        assert null != c

        q = dbf.createQuery(ImageCacheVO.class)
        q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image1.getUuid())
        q.add(ImageCacheVO_.primaryStorageUuid, SimpleQuery.Op.EQ, nfs.getUuid())
        c = q.find()
        assert null == c
    }
}