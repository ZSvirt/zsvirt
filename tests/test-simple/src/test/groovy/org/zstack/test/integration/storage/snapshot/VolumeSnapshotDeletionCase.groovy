package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.snapshot.VolumeSnapshotEO
import org.zstack.header.storage.snapshot.VolumeSnapshotEO_
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeEO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeEO_
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.header.volume.VolumeAO_
import org.zstack.header.volume.VolumeEO
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by kayo on 2018/2/26.
 */
class VolumeSnapshotDeletionCase extends SubCase {
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
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testPrimaryStorageDeletionCleansSnapshotTreeResources()
            testMaintainPrimaryStorageDeletion()
        }
    }

    void testMaintainPrimaryStorageDeletion() {
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory
        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            name = "test"
            volumeUuid = vm.getRootVolumeUuid()
        } as VolumeSnapshotInventory

        def psUuid = snapshot.primaryStorageUuid

        changePrimaryStorageState {
            uuid = psUuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }

        def cmd = null
        env.afterSimulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            return rsp
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psUuid
            clusterUuid = vm.getClusterUuid()
        }

        deletePrimaryStorage {
            uuid = psUuid
        }

        assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, vm.rootVolumeUuid).isExists()
        assert cmd == null
    }

    void testPrimaryStorageDeletionCleansSnapshotTreeResources() {
        def zone = env.inventoryByName("zone")
        def l2 = env.inventoryByName("l2")
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")

        def cluster = createCluster {
            name = "snapshot-delete-cluster"
            hypervisorType = "KVM"
            zoneUuid = zone.uuid
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = l2.uuid
            clusterUuid = cluster.uuid
        }

        def host = addKVMHost {
            name = "snapshot-delete-host"
            managementIp = "127.0.0.10"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }

        def ps = addLocalPrimaryStorage {
            name = "snapshot-delete-local-pri"
            url = "/snapshot-delete-local-ps"
            zoneUuid = zone.uuid
        }

        env.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { LocalStorageKvmBackend.InitRsp rsp, HttpEntity<String> e ->
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(100)
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(100)
            rsp.localStorageUsedCapacity = 0
            return rsp
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        env.simulator(SftpBackupStorageConstant.CONNECT_PATH) {
            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1000)
            return rsp
        }
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(1)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(1)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(999)
            return rsp
        }
        env.simulator(SftpBackupStorageConstant.GET_IMAGE_SIZE) {
            def rsp = new SftpBackupStorageCommands.GetImageSizeRsp()
            rsp.size = SizeUnit.GIGABYTE.toByte(1)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        def bs = addSftpBackupStorage {
            name = "snapshot-delete-sftp"
            url = "/snapshot-delete-sftp"
            username = "root"
            password = "password"
            hostname = "localhost"
        }

        attachBackupStorageToZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs.uuid
        }

        def image = addImage {
            name = "snapshot-delete-image"
            url = "http://zstack.org/download/snapshot-delete.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        env.simulator(LocalStorageKvmBackend.CREATE_VOLUME_FROM_CACHE_PATH) {
            def rsp = new LocalStorageKvmBackend.CreateVolumeFromCacheRsp()
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(100)
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(98)
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "snapshot-delete-vm"
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
            clusterUuid = cluster.uuid
            hostUuid = host.uuid
            primaryStorageUuidForRootVolume = ps.uuid
        } as VmInstanceInventory

        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            name = "delete-ps-root"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        List<String> volumeUuids = [vm.rootVolumeUuid]
        List<String> snapshotUuids = [snapshot.uuid]
        List<String> treeUuids = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.treeUuid)
                .in(VolumeSnapshotVO_.uuid, snapshotUuids)
                .listValues()
                .unique()
        assert treeUuids.size() == 1

        changePrimaryStorageState {
            uuid = ps.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        deletePrimaryStorage {
            uuid = ps.uuid
        }

        assert !Q.New(VolumeEO.class).in(VolumeAO_.uuid, volumeUuids).isExists()
        assert !Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, snapshotUuids).isExists()
        assert !Q.New(VolumeSnapshotEO.class).in(VolumeSnapshotEO_.uuid, snapshotUuids).isExists()
        assert !Q.New(VolumeSnapshotTreeVO.class).in(VolumeSnapshotTreeVO_.uuid, treeUuids).isExists()
        assert !Q.New(VolumeSnapshotTreeEO.class).in(VolumeSnapshotTreeEO_.uuid, treeUuids).isExists()
        assert !Q.New(ResourceVO.class).in(ResourceVO_.uuid, treeUuids).isExists()

        detachL2NetworkFromCluster {
            l2NetworkUuid = l2.uuid
            clusterUuid = cluster.uuid
        }

        deleteHost {
            uuid = host.uuid
        }

        deleteCluster {
            uuid = cluster.uuid
        }

        deleteImage {
            uuid = image.uuid
        }

        detachBackupStorageFromZone {
            zoneUuid = zone.uuid
            backupStorageUuid = bs.uuid
        }

        deleteBackupStorage {
            uuid = bs.uuid
        }
    }
}
