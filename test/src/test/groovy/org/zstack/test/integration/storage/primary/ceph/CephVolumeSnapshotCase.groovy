package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.trash.StorageTrash
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageMsg
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageReply
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO_
import org.zstack.storage.encrypt.VolumeEncryptedInitialExtension
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper
import org.zstack.storage.encrypt.VolumeSnapshotEncryptionHelper
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by mingjian.deng on 2019/1/7.*/
class CephVolumeSnapshotCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    VolumeInventory data
    VolumeInventory root
    VolumeSnapshotInventory rootSnapshot
    VolumeSnapshotInventory dataSnapshot
    ImageInventory image1
    VmInstanceInventory vm

    StorageTrash trash

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
        env = CephEnv.CephStorageOneVmWithDataVolumeEnv()
    }

    @Override
    void test() {
        env.message(ResizeVolumeOnPrimaryStorageMsg.class) { ResizeVolumeOnPrimaryStorageMsg msg, CloudBus cloudBus ->
            ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply()
            msg.volume.size = msg.size
            reply.volume = msg.volume
            cloudBus.reply(msg, reply)
        }

        env.create {
            prepare()
            testCreateRootSnapshot()
            testCreateDataSnapshot()
            testCreateVolumeFromSnapshot()
            testCreateVolumeFromEncryptedSnapshot()
            stopVmInstance {
                uuid = vm.uuid
            }

            testRollbackVolumeFromSnapshot()
            testReImage()
            testRollbackVolumeFromRootSnapshotAfterReImage()


            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        image1 = env.inventoryByName("image1") as ImageInventory
        data = env.inventoryByName("volume") as VolumeInventory
        vm = env.inventoryByName("test-vm") as VmInstanceInventory
        root = queryVolume {
            conditions = ["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory

        VolumeSnapshotGlobalConfig.SNAPSHOT_BEFORE_REVERTVOLUME.updateValue(true)  // create snapshot before

        trash = bean(StorageTrash.class)
    }

    void testCreateDataSnapshot() {
        dataSnapshot = createVolumeSnapshot {
            volumeUuid = data.uuid
            name = "data-snapshot"
        } as VolumeSnapshotInventory

        assert dataSnapshot.primaryStorageInstallPath.startsWith("ceph://pri-v-d-")
        assert dataSnapshot.primaryStorageInstallPath.endsWith("/${data.uuid}@${dataSnapshot.uuid}")
        assert dataSnapshot.parentUuid == null
        assert dataSnapshot.status == VolumeSnapshotStatus.Ready.toString()
        assert dataSnapshot.volumeType == VolumeType.Data.toString()
        assert dataSnapshot.type == "Storage"

        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.volumeUuid, data.uuid).count() == 1
    }

    void testCreateRootSnapshot() {
        rootSnapshot = createVolumeSnapshot {
            volumeUuid = root.uuid
            name = "root-snapshot"
        } as VolumeSnapshotInventory

        assert rootSnapshot.primaryStorageInstallPath.startsWith("ceph://pri-v-r-")
        assert rootSnapshot.primaryStorageInstallPath.endsWith("/${root.uuid}@${rootSnapshot.uuid}")
        assert rootSnapshot.parentUuid == null
        assert rootSnapshot.status == VolumeSnapshotStatus.Ready.toString()
        assert rootSnapshot.volumeType == VolumeType.Root.toString()
        assert rootSnapshot.type == "Storage"
        assert rootSnapshot.primaryStorageUuid == ps.uuid

        assert Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.volumeUuid, root.uuid).count() == 1
    }

    void testCreateVolumeFromSnapshot() {
        def volume = createDataVolumeFromVolumeSnapshot {
            name = "test-data-vol-from-snap"
            volumeSnapshotUuid = dataSnapshot.uuid
        } as VolumeInventory

        assert volume.installPath == data.installPath - data.uuid + volume.uuid
    }

    void testCreateVolumeFromEncryptedSnapshot() {
        long snapshotVirtualSize = data.size + SizeUnit.MEGABYTE.toByte(16)

        long originalSnapshotVirtualSize = replaceCephRawVirtualSize(dataSnapshot.primaryStorageInstallPath, snapshotVirtualSize)
        VolumeInventory encryptedVolume
        setSnapshotEncrypted(true)
        try {
            withEncryptedSnapshotCloneStubs(snapshotVirtualSize) {
                encryptedVolume = createDataVolumeFromVolumeSnapshot {
                    name = "test-encrypted-data-vol-from-snap"
                    volumeSnapshotUuid = dataSnapshot.uuid
                } as VolumeInventory
            }
        } finally {
            replaceCephRawVirtualSize(dataSnapshot.primaryStorageInstallPath, originalSnapshotVirtualSize)
            setSnapshotEncrypted(false)
        }

        VolumeVO persisted = bean(DatabaseFacade.class).findByUuid(encryptedVolume.uuid, VolumeVO.class)
        assert persisted.size == snapshotVirtualSize :
                "VolumeVO.size should be updated from encrypted snapshot virtual size: expected=${snapshotVirtualSize} actual=${persisted.size}"
        long targetVirtualSize = cephRaw(encryptedVolume.installPath).virtualSize
        assert targetVirtualSize == snapshotVirtualSize :
                "Encrypted Ceph snapshot clone should create target RBD with snapshot virtual size: expected=${snapshotVirtualSize} actual=${targetVirtualSize}"
    }

    def cephVfs() {
        String fsId = Q.New(CephPrimaryStorageVO.class)
                .select(CephPrimaryStorageVO_.fsid)
                .eq(CephPrimaryStorageVO_.uuid, ps.uuid)
                .findValue()
        return CephPrimaryStorageSpec.vfs1(fsId, env)
    }

    def cephRaw(String installPath) {
        return cephVfs().getFile(CephPrimaryStorageSpec.cephPathToVFSPath(installPath), true)
    }

    long replaceCephRawVirtualSize(String installPath, long virtualSize) {
        def raw = cephRaw(installPath)
        long originalVirtualSize = raw.virtualSize
        raw.virtualSize = virtualSize
        raw.update()
        return originalVirtualSize
    }

    void setSnapshotEncrypted(boolean encrypted) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        VolumeSnapshotVO vo = dbf.findByUuid(dataSnapshot.uuid, VolumeSnapshotVO.class)
        vo.encrypted = encrypted
        dbf.update(vo)
    }

    private <T> T withEncryptedSnapshotCloneStubs(long snapshotVirtualSize, Closure<T> body) {
        def snapshotHelper = new NoopVolumeSnapshotEncryptionHelper()
        def secretHelper = new StubVolumeEncryptedSecretHelper()
        def initialExtension = bean(VolumeEncryptedInitialExtension.class)
        def originalSnapshotHelper = setField(initialExtension, VolumeEncryptedInitialExtension.class,
                "snapshotEncryptionHelper", snapshotHelper)
        Closure originalSnapshotSizeSimulator = env.getSimulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH)
        Closure originalCephFactory = Test.functionForMockTestObjectFactory.put(CephPrimaryStorageBase.class,
                { CephPrimaryStorageBase base ->
                    setField(base, CephPrimaryStorageBase.class, "volumeEncryptedSecretHelper", secretHelper)
                    return base
                })

        env.simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH) {
            def rsp = new CephPrimaryStorageBase.GetVolumeSnapshotSizeRsp()
            rsp.size = snapshotVirtualSize
            return rsp
        }
        env.simulator(CephPrimaryStorageBase.KVM_HOST_LUKS_CLONE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.KVMHostLuksCloneCmd.class)
            cephVfs().createCephRaw(CephPrimaryStorageSpec.cephPathToVFSPath(cmd.dstPath),
                    cmd.virtualSizeForLuksClone == null ? 0L : cmd.virtualSizeForLuksClone)
            return new CephPrimaryStorageBase.KVMHostLuksRsp()
        }

        try {
            return body.call()
        } finally {
            env.simulator(CephPrimaryStorageBase.GET_VOLUME_SNAPSHOT_SIZE_PATH, originalSnapshotSizeSimulator)
            setField(initialExtension, VolumeEncryptedInitialExtension.class,
                    "snapshotEncryptionHelper", originalSnapshotHelper)
            if (originalCephFactory == null) {
                Test.functionForMockTestObjectFactory.remove(CephPrimaryStorageBase.class)
            } else {
                Test.functionForMockTestObjectFactory.put(CephPrimaryStorageBase.class, originalCephFactory)
            }
        }
    }

    private static Object setField(Object target, Class targetClass, String fieldName, Object value) {
        def field = targetClass.getDeclaredField(fieldName)
        field.accessible = true
        Object original = field.get(target)
        field.set(target, value)
        return original
    }

    void testRollbackVolumeFromSnapshot() {
        def installPath = data.installPath
        def size = data.size

        def volumeDeleted = false
        env.preSimulator(CephPrimaryStorageBase.DELETE_PATH) { rsp, HttpEntity<String> e ->
            volumeDeleted = true
            return rsp
        }

        revertVolumeFromSnapshot {
            uuid = dataSnapshot.uuid
        }

        data = queryVolume {
            conditions = ["uuid=${data.uuid}"]
        }[0] as VolumeInventory

        def snapshot = queryVolumeSnapshot {
            conditions = ["name~=revert-volume-point-${data.uuid}-%"]
        }[0] as VolumeSnapshotInventory

        assert data.installPath == installPath   // installPath not changed
        assert data.size == size
        assert snapshot.volumeUuid == data.uuid
        assert snapshot.primaryStorageInstallPath.contains(installPath+"@")
        assert !volumeDeleted
    }

    void testReImage() {
        VolumeVO originVol = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).find()
        def vm1 = reimageVmInstance {
            vmInstanceUuid = vm.uuid
        } as VmInstanceInventory

        assert vm1.uuid == vm.uuid
        assert vm1.rootVolumeUuid == vm.rootVolumeUuid
        def root1 = queryVolume {
            conditions = ["uuid=${vm1.rootVolumeUuid}"]
        }[0] as VolumeInventory

        assert root1.uuid == root.uuid
        assert root1.installPath.contains("/reset-image-${root.uuid}")
        assert trash.getTrashList(ps.uuid).size() == 1
    }

    void testRollbackVolumeFromRootSnapshotAfterReImage() {
        root = queryVolume {
            conditions = ["uuid=${root.uuid}"]
        }[0] as VolumeInventory

        def installPath = root.installPath
        def size = root.size
        def snapshotInstallPath = rootSnapshot.primaryStorageInstallPath
        assert root.installPath != snapshotInstallPath

        def volumeDeleted = false
        env.preSimulator(CephPrimaryStorageBase.DELETE_PATH) { rsp, HttpEntity<String> e ->
            volumeDeleted = true
            return rsp
        }

        revertVolumeFromSnapshot {
            uuid = rootSnapshot.uuid
        }

        def root = queryVolume {
            conditions = ["uuid=${root.uuid}"]
        }[0] as VolumeInventory

        def snapshot = queryVolumeSnapshot {
            conditions = ["name~=revert-volume-point-${root.uuid}-%"]
        }[0] as VolumeSnapshotInventory

        assert root.installPath == snapshotInstallPath.split("@")[0]   // installPath changed
        assert root.size == size

        assert snapshot.primaryStorageInstallPath.contains(installPath+"@")
        assert snapshot.volumeUuid == root.uuid
        assert !volumeDeleted
    }

    private static class StubVolumeEncryptedSecretHelper extends VolumeEncryptedSecretHelper {
        @Override
        String materializeAndSealVolumeDekForHost(String hostUuid, String volumeUuid) {
            return "sealed-dek"
        }
    }

    private static class NoopVolumeSnapshotEncryptionHelper extends VolumeSnapshotEncryptionHelper {
        @Override
        void inheritFromRelatedSnapshotKeyIfPossible(VolumeVO volume, String snapshotUuid) {
        }
    }
}
