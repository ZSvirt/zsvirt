package org.zstack.test.integration.storage.snapshot

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.BatchDeleteVolumeSnapshotResult
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.storage.primary.local.LocalStorageKvmBackend

/**
 * Create by weiwang at 2018-12-22
 */
public class BatchDeleteVolumeSnapshotCase extends SubCase {
    DatabaseFacade dbf
    EnvSpec env
    VmInstanceInventory cephVm
    VmInstanceInventory localVm
    VmInstanceInventory localVm2
    VmInstanceInventory localVm3

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
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            cephBackupStorage {
                name = "bs"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@127.0.0.1/?monPort=7777"]

                image {
                    name = "ceph-image1"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

                image {
                    name = "sftp-image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    actualSize = SizeUnit.GIGABYTE.toByte(60)
                    name = "sftp-image2"
                    url  = "http://zstack.org/download/test2.qcow2"
                }
            }

            zone {
                name = "zone"


                cephPrimaryStorage {
                    name = "ceph-ps"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.1/?monPort=7777"]
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-host"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-ps")
                    attachL2Network("l2")
                }

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
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

                attachBackupStorage("bs")
                attachBackupStorage("sftp")
            }

            vm {
                name = "cephVm"
                useCluster("ceph-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("ceph-image1")
            }

            vm {
                name = "cephVm2"
                useCluster("ceph-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("ceph-image1")
            }

            vm {
                name = "localVm"
                useCluster("local-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("sftp-image1")
            }

            vm {
                name = "localVm2"
                useCluster("local-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("sftp-image1")
            }

            vm {
                name = "localVm3"
                useCluster("local-cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("sftp-image2")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            cephVm = env.inventoryByName("cephVm") as VmInstanceInventory
            localVm = env.inventoryByName("localVm") as VmInstanceInventory
            localVm2 = env.inventoryByName("localVm2") as VmInstanceInventory
            localVm3 = env.inventoryByName("localVm3") as VmInstanceInventory

            updateGlobalConfig {
                category= VolumeSnapshotGlobalConfig.CATEGORY
                name = VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.name
                value = 4
            }

            testBatchDeleteVolumeSnapshotOnLocal()
            testBatchDeleteVolumeSnapshotOnCeph()
            testBatchDeleteVolumeSnapshotOnLocalWhenVmDestroyed()
            testBatchDeleteVolumeSnapshotOnCephWhenVmDestroyed()
            testDeleteVolumeSnapshotOnLocalWhenHasNoEnoughSpace()
            testBatchDeleteVolumeSnapshotTimeout()
        }
    }

    void testDeleteVolumeSnapshotOnLocalWhenHasNoEnoughSpace() {
        PrimaryStorageInventory local = env.inventoryByName("local")
        def snap = createVolumeSnapshot {
            name = "snapshot"
            volumeUuid = localVm3.rootVolumeUuid
        } as VolumeSnapshotInventory
        def volumeSnapshotVO = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snap.uuid).find() as VolumeSnapshotVO
        volumeSnapshotVO.size = SizeUnit.TERABYTE.toByte(10)
        dbf.update(volumeSnapshotVO)
        deleteVolumeSnapshot {
            uuid = snap.uuid
        }
        def localPrimaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, local.uuid).find() as PrimaryStorageVO
        assert local.getAvailableCapacity() - 1 == localPrimaryStorageVO.getCapacity().getAvailableCapacity()

        def snap1 = createVolumeSnapshot {
            name = "snapshot1"
            volumeUuid = localVm3.rootVolumeUuid
        } as VolumeSnapshotInventory
        SQL.New(VolumeSnapshotVO.class)
                .set(VolumeSnapshotVO_.size, SizeUnit.TERABYTE.toByte(120))
                .eq(VolumeSnapshotVO_.uuid, snap1.uuid)
                .update()
        SQL.New(VolumeVO.class)
                .set(VolumeVO_.size, SizeUnit.TERABYTE.toByte(120))
                .eq(VolumeVO_.uuid, localVm3.rootVolumeUuid)
                .update()
        expectError {
            deleteVolumeSnapshot {
                uuid = snap1.uuid
            }
        }

        SQL.New(VolumeSnapshotVO.class)
                .set(VolumeSnapshotVO_.size, SizeUnit.TERABYTE.toByte(110))
                .eq(VolumeSnapshotVO_.uuid, snap1.uuid)
                .update()
        def snap2 = createVolumeSnapshot {
            name = "snapshot2"
            volumeUuid = localVm3.rootVolumeUuid
        } as VolumeSnapshotInventory
        SQL.New(VolumeSnapshotVO.class)
            .set(VolumeSnapshotVO_.size, SizeUnit.TERABYTE.toByte(15))
            .eq(VolumeSnapshotVO_.uuid, snap2.uuid)
            .update()
        SQL.New(VolumeVO.class)
                .set(VolumeVO_.size, SizeUnit.TERABYTE.toByte(125))
                .eq(VolumeVO_.uuid, localVm3.rootVolumeUuid)
                .update()
        expectError {
            deleteVolumeSnapshot {
                uuid = snap1.uuid
            }
        }
    }

    //NOTE(weiw): 1. create 20 snapshots
    //            2. delete 1 and 2 and no need to merge
    //            3. delete all and only one merge called
    void testBatchDeleteVolumeSnapshotOnLocal() {
        for (i in 1..22) {
            def snapName = "localsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = localVm.rootVolumeUuid
                name = snapName
            }
        }

        List<KVMAgentCommands.MergeSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            def mergeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MergeSnapshotCmd.class)
            cmds.add(mergeCmd)
            return rsp
        }

        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=localsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=localsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 0


        VolumeSnapshotInventory snap6 = queryVolumeSnapshot {conditions=["name=localsnap6".toString()]}[0]
        VolumeSnapshotInventory snap8 = queryVolumeSnapshot {conditions=["name=localsnap8".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap6, snap8].uuid
        }
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 14
        assert cmds.size() == 0

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        }

        assert cmds.size() == 1
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 0
    }

    void testBatchDeleteVolumeSnapshotOnCeph() {
        for (i in 1..22) {
            def snapName = "cephsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = cephVm.rootVolumeUuid
                name = snapName
            }
        }
        List<CephPrimaryStorageBase.DeleteSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)
            return rsp
        }


        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=cephsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=cephsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 2
        cmds.clear()
        env.cleanSimulatorHandlers()
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 20

        VolumeSnapshotInventory snap3 = queryVolumeSnapshot {conditions=["name=cephsnap3".toString()]}[0]

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)

            if (delCmd.snapshotPath == snap3.primaryStorageInstallPath) {
                rsp.setSuccess(false)
                rsp.setError("test error")
            }
            return rsp
        }

        def result = batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        } as BatchDeleteVolumeSnapshotResult

        assert cmds.size() == 20
        assert result.results.error.toSet() == [null].toSet()
        // delete error will not return error, check org.zstack.storage.snapshot.VolumeSnapshotBase.handle(org.zstack.header.storage.snapshot.VolumeSnapshotPrimaryStorageDeletionMsg)
    }

    void testBatchDeleteVolumeSnapshotOnLocalWhenVmDestroyed(){
        for (i in 1..22) {
            def snapName = "localsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = localVm.rootVolumeUuid
                name = snapName
            }
        }

        destroyVmInstance {
            uuid = localVm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(localVm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Destroyed

        List<KVMAgentCommands.MergeSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.afterSimulator(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) { rsp, HttpEntity<String> e ->
            def mergeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MergeSnapshotCmd.class)
            cmds.add(mergeCmd)
            return rsp
        }

        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=localsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=localsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 0


        VolumeSnapshotInventory snap6 = queryVolumeSnapshot {conditions=["name=localsnap6".toString()]}[0]
        VolumeSnapshotInventory snap8 = queryVolumeSnapshot {conditions=["name=localsnap8".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap6, snap8].uuid
        }
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 14
        assert cmds.size() == 0

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        }

        assert cmds.size() == 1
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 0
    }

    void testBatchDeleteVolumeSnapshotOnCephWhenVmDestroyed(){
        for (i in 1..22) {
            def snapName = "cephsnap${i}".toString()
            createVolumeSnapshot {
                volumeUuid = cephVm.rootVolumeUuid
                name = snapName
            }
        }
        destroyVmInstance {
            uuid = cephVm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(cephVm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Destroyed

        List<CephPrimaryStorageBase.DeleteSnapshotCmd> cmds = Collections.synchronizedList(new ArrayList<>())
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)
            return rsp
        }


        VolumeSnapshotInventory snap1 = queryVolumeSnapshot {conditions=["name=cephsnap1".toString()]}[0]
        VolumeSnapshotInventory snap2 = queryVolumeSnapshot {conditions=["name=cephsnap2".toString()]}[0]

        batchDeleteVolumeSnapshot {
            uuids = [snap1, snap2].uuid
        }

        assert cmds.size() == 2
        cmds.clear()
        env.cleanSimulatorHandlers()
        assert (queryVolumeSnapshot {} as List<VolumeSnapshotInventory>).size() == 20

        VolumeSnapshotInventory snap3 = queryVolumeSnapshot {conditions=["name=cephsnap3".toString()]}[0]

        List<VolumeSnapshotInventory> snaps = queryVolumeSnapshot {}
        env.simulator(CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new CephPrimaryStorageBase.DeleteSnapshotRsp()
            def delCmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.DeleteSnapshotCmd.class)
            cmds.add(delCmd)

            if (delCmd.snapshotPath == snap3.primaryStorageInstallPath) {
                rsp.setSuccess(false)
                rsp.setError("test error")
            }
            return rsp
        }

        def result = batchDeleteVolumeSnapshot {
            uuids = snaps.uuid
        } as BatchDeleteVolumeSnapshotResult

        assert cmds.size() == 20
        assert result.results.error.toSet() == [null].toSet()
        // delete error will not return error, check org.zstack.storage.snapshot.VolumeSnapshotBase.handle(org.zstack.header.storage.snapshot.VolumeSnapshotPrimaryStorageDeletionMsg)

    }

    void testBatchDeleteVolumeSnapshotTimeout(){
        def snapTestDeleteTimeout = createVolumeSnapshot {
            volumeUuid = localVm2.rootVolumeUuid
            name = "snapTestDeleteTimeOut"
        } as VolumeSnapshotInventory

        assert snapTestDeleteTimeout.uuid != null

        env.simulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.MergeSnapshotRsp()
            rsp.success = false
            rsp.error = "error wanted"
            return rsp
        }

        def result = batchDeleteVolumeSnapshot {
            uuids = [snapTestDeleteTimeout.uuid]
        } as BatchDeleteVolumeSnapshotResult

        result.results.error != null
    }
}
