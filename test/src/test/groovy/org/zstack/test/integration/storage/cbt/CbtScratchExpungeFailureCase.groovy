package org.zstack.test.integration.storage.cbt

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
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

/** Exercise CBT cleanup through the real volume service, including a failed physical-delete reply. */
class CbtScratchExpungeFailureCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(PremiumEnv.makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {
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
    }

    @Override
    void test() {
        env.create {
            testPhysicalDeleteFailureRetainsOnlyTheFailedScratch()
        }
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
    }
}
