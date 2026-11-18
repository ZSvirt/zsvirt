package org.zstack.test.integration.storage.cbt

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GarbageCollector
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.agent.AgentResponse
import org.zstack.kvm.KVMHostVO
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.cbt.UnexportNbdVolumesGC
import org.zstack.storage.cbt.CbtBackupKvmCommands

import org.zstack.header.cbt.CbtTaskResourceRefVO
import org.zstack.header.cbt.CbtTaskResourceRefVO_
import org.zstack.header.cbt.VolumeCbtBackupInfo
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.AccountConstant
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.CbtTaskInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.cbt.CbtBackupManagerImpl
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.premium.PremiumEnv
import org.zstack.utils.gson.JSONObjectUtil

import static org.mockito.Mockito.*

/** Exercise CBT cleanup through the real volume service, including a failed physical-delete reply. */
class CbtScratchExpungeFailureCase extends SubCase {
    EnvSpec env
    String unexportGcName

    @Override
    void setup() {
        useSpring(PremiumEnv.makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {
            diskOffering {
                name = "uninitialized-disk"
                diskSize = 1024L * 1024 * 1024
            }
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "host"
                        managementIp = "127.0.0.1"
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
                attachBackupStorage("sftp")
            }
            vm {
                name = "vm"
                cpu = 1
                memoryGB(1)
                useImage("image")
                disk {
                    boot = true
                }
                disk {
                    sizeGB(1)
                }
                useL3Networks("l3")
                useHost("host")
            }
        }
        env.simulator(CbtBackupKvmCommands.REST_API_UNEXPORT_NDB_VOLUMES_PATH) {
            return new AgentResponse()
        }
    }

    @Override
    void test() {
        env.create {
            testUnexportGcVolumeFiltering()
            testPhysicalDeleteFailureRetainsOnlyTheFailedScratch()
        }
    }

    private void testUnexportGcVolumeFiltering() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        unexportGcName = "gc-unexport-nbd-volumes-for-vm-${vm.uuid}-on-host-${vm.hostUuid}"
        VolumeInventory pending = createDataVolume {
            name = "uninitialized-nbd-volume"
            diskOfferingUuid = env.inventoryByName("uninitialized-disk").uuid
        }
        assert pending.status == "NotInstantiated" && !pending.installPath :
                "Fixture must have no backing storage: status=${pending.status}, path=${pending.installPath}"

        def gc = new UnexportNbdVolumesGC()
        gc.prepare(vm.uuid, [pending.uuid, vm.rootVolumeUuid])
        assert gc.volumes*.volumeUuid == [vm.rootVolumeUuid] :
                "GC must retain the initialized volume only: ${gc.volumes*.volumeUuid}"
        assert gc.volumes[0].installPath : "Initialized volume must retain its cleanup path"

        gc.prepare(vm.uuid, [pending.uuid])
        assert gc.volumes.isEmpty() : "Uninitialized volumes must not enter GC payload: ${gc.volumes}"

        testFilterBoundary(vm)
    }

    private void testFilterBoundary(VmInstanceInventory vm) {
        VolumeVO original = dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class)
        VolumeVO volume = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(original), VolumeVO.class)
        def db = mock(DatabaseFacade.class)
        when(db.findByUuid(vm.hostUuid, KVMHostVO.class)).thenReturn(dbFindByUuid(vm.hostUuid, KVMHostVO.class))
        when(db.findByUuid(vm.rootVolumeUuid, VolumeVO.class)).thenReturn(volume)
        def gc = new UnexportNbdVolumesGC()
        def dbField = GarbageCollector.getDeclaredField("dbf")
        dbField.accessible = true
        dbField.set(gc, db)

        volume.status = VolumeStatus.NotInstantiated
        gc.prepare(vm.uuid, [volume.uuid])
        assert gc.volumes*.installPath == [original.installPath] :
                "A volume with a backing path must remain in cleanup despite NotInstantiated status"
    }

    private void testPhysicalDeleteFailureRetainsOnlyTheFailedScratch() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        CbtTaskInventory task = createCbtTask {
            name = "scratch-expunge-failure"
            vmInstanceUuid = vm.uuid
        } as CbtTaskInventory
        CbtBackupManagerImpl manager = bean(CbtBackupManagerImpl.class)
        List<String> sourceVolumes = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vm.uuid).select(VolumeVO_.uuid).listValues()
        assert sourceVolumes.size() == 2
        FutureCompletion prepared = new FutureCompletion(null)
        manager.prepareCbtVolumes(sourceVolumes, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID,
                vm.hostUuid, task.uuid, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(prepared) {
            @Override
            void success(List<VolumeCbtBackupInfo> infos) {
                prepared.success()
            }

            @Override
            void fail(ErrorCode errorCode) {
                prepared.fail(errorCode)
            }
        })
        prepared.await(30000)
        assert prepared.success: prepared.errorCode

        List<String> scratchUuids = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, task.uuid)
                .eq(CbtTaskResourceRefVO_.resourceType, VolumeVO.simpleName)
                .select(CbtTaskResourceRefVO_.resourceUuid).listValues()
        assert scratchUuids.size() == 2
        List<VolumeVO> scratches = Q.New(VolumeVO.class).in(VolumeVO_.uuid, scratchUuids).list()
        VolumeVO failed = scratches.find { it.description == "cbt-volume-for-${vm.rootVolumeUuid}" }
        assert failed != null
        VolumeVO healthy = scratches.find { it.uuid != failed.uuid }
        assert healthy != null
        long before = availableCapacity(failed.primaryStorageUuid)
        int injectedFailures = 0
        // Only the selected scratch is in use; every other delete uses the normal simulator and VFS path.
        Closure deleteBits = env.getSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH)
        env.simulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def response = deleteBits(entity, spec)
            def command = JSONObjectUtil.toObject(entity.body, LocalStorageKvmBackend.DeleteBitsCmd.class)
            if (command.path == failed.installPath) {
                injectedFailures++
                response.setError("Case holds the selected scratch volume")
                response.inUse = true
            }
            return response
        }

        FutureCompletion rejected = new FutureCompletion(null)
        manager.destroyCbtVolumes(task.uuid, rejected)
        rejected.await(30000)
        assert !rejected.success
        assert !rejected.timeout
        assert rejected.errorCode.toString().contains("VOLUME.1000")
        assert injectedFailures == 1
        VolumeVO retained = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, failed.uuid).find()
        assert retained != null && retained.status == VolumeStatus.Deleted
        assert referenceCount(task.uuid, failed.uuid) == 1
        assert !Q.New(VolumeVO.class).eq(VolumeVO_.uuid, healthy.uuid).isExists()
        assert referenceCount(task.uuid, healthy.uuid) == 0
        retryInSecs {
            assert availableCapacity(failed.primaryStorageUuid) == before + healthy.size
        }

        env.simulator(LocalStorageKvmBackend.DELETE_BITS_PATH, deleteBits)
        FutureCompletion retried = new FutureCompletion(null)
        manager.destroyCbtVolumes(task.uuid, retried)
        retried.await(30000)
        assert retried.success: retried.errorCode
        assert !Q.New(VolumeVO.class).eq(VolumeVO_.uuid, failed.uuid).isExists()
        assert referenceCount(task.uuid, failed.uuid) == 0
        retryInSecs {
            assert availableCapacity(failed.primaryStorageUuid) == before + healthy.size + failed.size
        }
        FutureCompletion repeated = new FutureCompletion(null)
        manager.destroyCbtVolumes(task.uuid, repeated)
        repeated.await(30000)
        assert repeated.success: repeated.errorCode

        deleteCbtTask { uuid = task.uuid }
        assert (queryCbtTask { conditions = ["uuid=${task.uuid}".toString()] } as List).empty
    }

    private static long availableCapacity(String primaryStorageUuid) {
        return Q.New(PrimaryStorageCapacityVO.class)
                .eq(PrimaryStorageCapacityVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageCapacityVO_.availableCapacity).findValue()
    }

    private static long referenceCount(String taskUuid, String volumeUuid) {
        return Q.New(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceUuid, volumeUuid).count()
    }

    @Override
    void clean() {
        env.delete()
        if (unexportGcName != null) {
            assert !Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.name, unexportGcName).isExists() :
                    "Successful NBD unexport during cleanup must not leave a GC job: ${unexportGcName}"
        }
    }
}
