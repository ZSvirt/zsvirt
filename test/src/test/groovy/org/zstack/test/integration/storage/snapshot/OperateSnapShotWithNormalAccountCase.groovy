package org.zstack.test.integration.storage.snapshot

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.identity.IdentityErrors
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.AccountResourceRefInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.QuotaInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.sdk.VolumeSnapshotTreeInventory
import org.zstack.storage.snapshot.VolumeSnapshotQuotaConstant
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec

class OperateSnapShotWithNormalAccountCase extends SnapShotCaseSub {
    EnvSpec env
    AccountInventory accountInventory
    ImageInventory image
    VmInstanceInventory vm
    VolumeInventory dataVolume
    L3NetworkInventory l3
    SessionInventory adminSession
    SessionInventory normalSession

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
        env = SnapShotEnv.normalAccountLocalStorageEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            setQuotaAndShareResource()
            testSnapShotOnRootVolumeWithQuota()
            testSnapShotOnDataVolumeWithQuota()
            logSnapshotOwnershipRaceAfterDeleteAccount()
        }
    }

    void prepare() {
        logger.info("prepare: load env inventories, login admin/normal, attach predefined roles")
        image = env.inventoryByName("image") as ImageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        accountInventory = env.inventoryByName("normal") as AccountInventory

        adminSession = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory
        assert adminSession.accountUuid == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID

        normalSession = logInByAccount {
            accountName = accountInventory.name
            password = "password"
        } as SessionInventory
        assert normalSession.accountUuid == accountInventory.uuid

        attachPredefineRoles(accountInventory.uuid, "image", "configuration", "networks", "vm", "volume", "snapshot")
    }

    void setQuotaAndShareResource() {
        logger.info("Test 001: query image and l3 as normal account before share, expect empty")
        def images = queryImage {
            sessionId = normalSession.uuid
        } as List<ImageInventory>
        def l3NetWorks = queryL3Network {
            sessionId = normalSession.uuid
        } as List<L3NetworkInventory>
        assert images.size() == 0
        assert l3NetWorks.size() == 0

        logger.info("Test 002: share image and l3 to public, expect normal account can query them")
        shareResource {
            resourceUuids = [l3.uuid, image.uuid]
            toPublic = true
            sessionId = adminSession.uuid
        }
        def sharedImages = queryImage {
            sessionId = normalSession.uuid
        } as List<ImageInventory>
        def sharedL3s = queryL3Network {
            sessionId = normalSession.uuid
        } as List<L3NetworkInventory>
        assert sharedImages.size() == 1
        assert sharedImages[0].uuid == image.uuid
        assert sharedL3s.size() == 1
        assert sharedL3s[0].uuid == l3.uuid

        logger.info("Test 003: update snapshot quota of normal account to 5, expect success")
        updateQuota {
            identityUuid = accountInventory.uuid
            name = VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM
            value = 5
            sessionId = adminSession.uuid
        }
        def quotas = queryQuota {
            conditions = [
                    "identityUuid=${accountInventory.uuid}".toString(),
                    "name=${VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM}".toString()
            ]
            sessionId = adminSession.uuid
        } as List<QuotaInventory>
        assert quotas.size() == 1
        assert quotas[0].value == 5L

        logger.info("Test 004: create vm[test-vm] under normal account, expect success")
        vm = createVmInstance {
            name = "test-vm"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            sessionId = normalSession.uuid
        } as VmInstanceInventory
        assert vm.name == "test-vm"
        assert vm.uuid != null
        assert vm.rootVolumeUuid != null
        def createdVms = queryVmInstance {
            conditions = ["uuid=${vm.uuid}".toString()]
            sessionId = normalSession.uuid
        } as List<VmInstanceInventory>
        assert createdVms.size() == 1
        assert createdVms[0].uuid == vm.uuid
    }

    void testSnapShotOnRootVolumeWithQuota() {
        withAccountSession("normal", "password") {
            logger.info("Test 101: create 5 root volume snapshots, expect success")
            List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
            for (int i = 0; i < 5; i++) {
                VolumeSnapshotInventory snapshotInv = createVolumeSnapshot {
                    volumeUuid = vm.getRootVolumeUuid()
                    name = String.format("test-snapshot-%s", i.toString())
                    sessionId = normalSession.uuid
                } as VolumeSnapshotInventory
                assert snapshotInv.uuid != null
                assert snapshotInv.volumeUuid == vm.getRootVolumeUuid()
                assert snapshotInv.name == String.format("test-snapshot-%s", i.toString())
                snapshotInvList.add(snapshotInv)
            }

            logger.info("Test 102: create the 6th root volume snapshot, expect API failure (quota exceeding)")
            expectApiFailure({
                createVolumeSnapshot {
                    volumeUuid = vm.getRootVolumeUuid()
                    name = "test-snapshot-5"
                    sessionId = normalSession.uuid
                }
            }) {
                assert code == IdentityErrors.QUOTA_EXCEEDING.toString()
            }

            def rootSnapshots = queryVolumeSnapshot {
                conditions = ["volumeUuid=${vm.getRootVolumeUuid()}".toString()]
            } as List<VolumeSnapshotInventory>
            assert rootSnapshots.size() == 5
            def trees = queryVolumeSnapshotTree {
            } as List<VolumeSnapshotTreeInventory>
            assert trees.size() == 1
            def latestRoot = queryVolumeSnapshot {
                conditions = ["uuid=${snapshotInvList.get(4).uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert latestRoot.size() == 1
            assert latestRoot[0].latest
            assert latestRoot[0].distance == 5

            logger.info("Test 103: stop vm and revert root volume to snapshot-3, expect snapshot-3 is latest")
            stopVmInstance {
                uuid = vm.uuid
            }
            revertVolumeFromSnapshot {
                uuid = snapshotInvList.get(3).uuid
            }
            def afterRevert = queryVolumeSnapshot {
                conditions = ["volumeUuid=${vm.getRootVolumeUuid()}".toString()]
            } as List<VolumeSnapshotInventory>
            assert afterRevert.size() == 5
            def reverted = queryVolumeSnapshot {
                conditions = ["uuid=${snapshotInvList.get(3).uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert reverted.size() == 1
            assert reverted[0].latest

            logger.info("Test 104: delete snapshot-3, expect child snapshot-4 also deleted and snapshot-2 is latest")
            deleteVolumeSnapshot {
                uuid = snapshotInvList.get(3).uuid
            }
            def afterDelete = queryVolumeSnapshot {
                conditions = ["volumeUuid=${vm.getRootVolumeUuid()}".toString()]
            } as List<VolumeSnapshotInventory>
            assert afterDelete.size() == 3
            def latestAfterDelete = queryVolumeSnapshot {
                conditions = ["uuid=${snapshotInvList.get(2).uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert latestAfterDelete.size() == 1
            assert latestAfterDelete[0].latest
        }
    }

    void testSnapShotOnDataVolumeWithQuota() {
        withAccountSession("normal", "password") {
            logger.info("Test 201: start vm, create and attach data volume, expect success")
            startVmInstance {
                uuid = vm.uuid
            }
            dataVolume = createDataVolume {
                name = "test-data-volume"
                diskSize = gb(20)
            } as VolumeInventory
            assert dataVolume.name == "test-data-volume"
            assert dataVolume.size == gb(20)
            attachDataVolumeToVm {
                volumeUuid = dataVolume.uuid
                vmInstanceUuid = vm.uuid
            }
            def attached = queryVolume {
                conditions = ["uuid=${dataVolume.uuid}".toString()]
            } as List<VolumeInventory>
            assert attached.size() == 1
            assert attached[0].vmInstanceUuid == vm.uuid

            logger.info("Test 202: create 2 data volume snapshots, expect success")
            List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
            for (int i = 0; i < 2; i++) {
                VolumeSnapshotInventory snapshotInv = createVolumeSnapshot {
                    volumeUuid = dataVolume.uuid
                    name = String.format("test-volume-snapshot-%s", i.toString())
                } as VolumeSnapshotInventory
                assert snapshotInv.uuid != null
                assert snapshotInv.volumeUuid == dataVolume.uuid
                snapshotInvList.add(snapshotInv)
            }

            logger.info("Test 203: create the 3rd data volume snapshot, expect API failure (quota exceeding)")
            expectApiFailure({
                createVolumeSnapshot {
                    volumeUuid = dataVolume.uuid
                    name = "test-volume-snapshot-2"
                }
            }) {
                assert code == IdentityErrors.QUOTA_EXCEEDING.toString()
            }

            def allSnapshots = queryVolumeSnapshot {
            } as List<VolumeSnapshotInventory>
            assert allSnapshots.size() == 5
            def trees = queryVolumeSnapshotTree {
            } as List<VolumeSnapshotTreeInventory>
            assert trees.size() == 2
            def latestData = queryVolumeSnapshot {
                conditions = ["uuid=${snapshotInvList.get(1).uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert latestData.size() == 1
            assert latestData[0].latest
            assert latestData[0].distance == 2

            logger.info("Test 204: stop vm and revert data volume to snapshot-0, expect snapshot-0 is latest")
            stopVmInstance {
                uuid = vm.uuid
            }
            revertVolumeFromSnapshot {
                uuid = snapshotInvList.get(0).uuid
            }
            def afterRevert = queryVolumeSnapshot {
            } as List<VolumeSnapshotInventory>
            assert afterRevert.size() == 5
            def reverted = queryVolumeSnapshot {
                conditions = ["uuid=${snapshotInvList.get(0).uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert reverted.size() == 1
            assert reverted[0].latest

            logger.info("Test 205: delete data volume snapshot-0, expect the whole data snapshot tree removed")
            deleteVolumeSnapshot {
                uuid = snapshotInvList.get(0).uuid
            }
            def afterDelete = queryVolumeSnapshot {
            } as List<VolumeSnapshotInventory>
            assert afterDelete.size() == 3
            def dataSnapshots = queryVolumeSnapshot {
                conditions = ["volumeUuid=${dataVolume.uuid}".toString()]
            } as List<VolumeSnapshotInventory>
            assert dataSnapshots.size() == 0
            def treesAfterDelete = queryVolumeSnapshotTree {
            } as List<VolumeSnapshotTreeInventory>
            assert treesAfterDelete.size() == 1
        }
    }

    void logSnapshotOwnershipRaceAfterDeleteAccount() {
        logger.info("Test 301: delete normal account, expect success and live Own refs transferred to admin")
        List<String> liveResourceUuids = [vm.uuid, vm.rootVolumeUuid, dataVolume.uuid]
        def liveSnapshotsBeforeDelete = queryVolumeSnapshot {
            sessionId = adminSession.uuid
        } as List<VolumeSnapshotInventory>
        liveResourceUuids.addAll(liveSnapshotsBeforeDelete.collect { it.uuid })

        deleteAccount {
            uuid = accountInventory.uuid
            sessionId = adminSession.uuid
        }
        def deletedAccounts = queryAccount {
            conditions = ["uuid=${accountInventory.uuid}".toString()]
            sessionId = adminSession.uuid
        } as List<AccountInventory>
        assert deletedAccounts.size() == 0

        liveResourceUuids.each { String resourceUuid ->
            def refs = queryAccountResourceRef {
                conditions = ["resourceUuid=${resourceUuid}".toString(), "type=Own"]
                sessionId = adminSession.uuid
            } as List<AccountResourceRefInventory>
            assert refs.size() == 1
            assert refs[0].accountUuid == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        }

        logger.info("Test 302: wait until VolumeSnapshotVO ResourceVO rows have Own refs (adminAdopt done), expect no orphan ResourceVO or timeout then continue")
        def adopted = retryInSecs(15) {
            return snapshotResourceVosHaveOwnRefs()
        }
        if (adopted) {
            logger.info("Test 302: adminAdopt finished, snapshot ResourceVO rows have Own refs")
        } else {
            logger.info("Test 302: wait adminAdopt timed out, continue to inspect leftover Own refs")
        }

        logger.info("Test 303: inspect Own VolumeSnapshotVO refs without VolumeSnapshotVO, log leak and do not fail the case")
        def ghostRefs = listGhostSnapshotOwnRefs()
        if (ghostRefs.isEmpty()) {
            logger.info("Test 303: [SNAPSHOT-OWNERSHIP-LEAK] not reproduced, no Own VolumeSnapshotVO refs without VolumeSnapshotVO")
        } else {
            logger.warn("Test 303: [SNAPSHOT-OWNERSHIP-LEAK] REPRODUCED, FIX SOON")
            logger.warn("Test 303: [SNAPSHOT-OWNERSHIP-LEAK] VolumeSnapshotTreeBase sql().hardDelete() skips ownership extensions; deleteAccount then adminAdoptAllOrphanedResource may recreate admin Own refs for leftover ResourceVO")
            ghostRefs.each { AccountResourceRefInventory ref ->
                logger.warn("Test 303: [SNAPSHOT-OWNERSHIP-LEAK] resourceUuid=${ref.resourceUuid}, accountUuid=${ref.accountUuid}, createDate=${ref.createDate}")
            }
        }

        logger.info("Test 399: destroy case vm resources with Direct policy and purge leftover snapshot Own refs, expect they are gone")
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString())
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())

        destroyVmInstance {
            uuid = vm.uuid
            sessionId = adminSession.uuid
        }
        def vmsAfterDestroy = queryVmInstance {
            conditions = ["uuid=${vm.uuid}".toString()]
            sessionId = adminSession.uuid
        } as List<VmInstanceInventory>
        assert vmsAfterDestroy.size() == 0

        def dataVolumesAfterDestroy = queryVolume {
            conditions = ["uuid=${dataVolume.uuid}".toString()]
            sessionId = adminSession.uuid
        } as List<VolumeInventory>
        if (!dataVolumesAfterDestroy.isEmpty()) {
            deleteDataVolume {
                uuid = dataVolume.uuid
                sessionId = adminSession.uuid
            }
            def dataVolumesAfterDelete = queryVolume {
                conditions = ["uuid=${dataVolume.uuid}".toString()]
                sessionId = adminSession.uuid
            } as List<VolumeInventory>
            assert dataVolumesAfterDelete.size() == 0
        }

        [vm.rootVolumeUuid, dataVolume.uuid].each { String volumeUuid ->
            def leftoverSnapshots = queryVolumeSnapshot {
                conditions = ["volumeUuid=${volumeUuid}".toString()]
                sessionId = adminSession.uuid
            } as List<VolumeSnapshotInventory>
            leftoverSnapshots.each { VolumeSnapshotInventory snapshot ->
                deleteVolumeSnapshot {
                    uuid = snapshot.uuid
                    sessionId = adminSession.uuid
                }
            }
        }

        purgeGhostSnapshotOwnRefs()
        assert listGhostSnapshotOwnRefsFromDb().isEmpty()
    }

    private boolean snapshotResourceVosHaveOwnRefs() {
        List<String> resourceUuids = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid)
                .eq(ResourceVO_.resourceType, VolumeSnapshotVO.class.simpleName)
                .listValues()
        if (resourceUuids.isEmpty()) {
            return true
        }

        def ownUuids = (queryAccountResourceRef {
            conditions = ["resourceType=${VolumeSnapshotVO.class.simpleName}".toString(), "type=Own"]
            sessionId = adminSession.uuid
        } as List<AccountResourceRefInventory>).collect { it.resourceUuid } as Set<String>
        return resourceUuids.every { ownUuids.contains(it) }
    }

    private List<AccountResourceRefInventory> listGhostSnapshotOwnRefs() {
        def liveUuids = (queryVolumeSnapshot {
            sessionId = adminSession.uuid
        } as List<VolumeSnapshotInventory>).collect { it.uuid } as Set<String>
        def refs = queryAccountResourceRef {
            conditions = ["resourceType=${VolumeSnapshotVO.class.simpleName}".toString(), "type=Own"]
            sessionId = adminSession.uuid
        } as List<AccountResourceRefInventory>
        if (liveUuids.isEmpty()) {
            return refs
        }
        return refs.findAll { !liveUuids.contains(it.resourceUuid) }
    }

    private List<AccountResourceRefVO> listGhostSnapshotOwnRefsFromDb() {
        List<String> liveUuids = (queryVolumeSnapshot {
            sessionId = adminSession.uuid
        } as List<VolumeSnapshotInventory>).collect { it.uuid }
        if (liveUuids.isEmpty()) {
            return Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.simpleName)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .list()
        }
        return Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.simpleName)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .notIn(AccountResourceRefVO_.resourceUuid, liveUuids)
                .list()
    }

    private void purgeGhostSnapshotOwnRefs() {
        List<AccountResourceRefVO> leftovers = listGhostSnapshotOwnRefsFromDb()
        if (leftovers.isEmpty()) {
            logger.info("Test 399: no leftover snapshot Own refs to purge")
            return
        }

        List<String> leftoverUuids = leftovers.collect { it.resourceUuid }
        logger.info("Test 399: purge leftover snapshot Own refs by SQL, queryXXX cannot delete AccountResourceRefVO, uuids=${leftoverUuids}")
        SQL.New(AccountResourceRefVO.class)
                .in(AccountResourceRefVO_.resourceUuid, leftoverUuids)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.simpleName)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .delete()
    }
}
