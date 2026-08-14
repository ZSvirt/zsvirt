package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.header.identity.AccountConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.snapshot.VolumeSnapshotQuotaConstant
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec

class OperateSnapShotWithNormalAccountCase extends SnapShotCaseSub{
    EnvSpec env
    AccountInventory accountInventory
    ImageInventory image
    VmInstanceInventory vm
    L3NetworkInventory l3
    DiskOfferingInventory diskOfferingInventory
    InstanceOfferingInventory instanceOffering
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
            /*
            * 1. create normal account and share resource
            * 2. set normal account snapshot quota value
            * 3. create vm with normal session
            * 4. create snapshot over quota
            * 5. revert root volume snapshot and check latest snapshot
            * 6. delete snapshot and check snapshot count
            * */
            prepare()
            setQuotaAndShareResource()
            testSnapShotOnRootVolumeWithQuota()
            testSnapShotOnDataVolumeWithQuota()
        }
    }

    void prepare() {
        image = env.inventoryByName("image") as ImageInventory
        instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        accountInventory = env.inventoryByName("normal") as AccountInventory
        diskOfferingInventory = env.inventoryByName("diskOffering") as DiskOfferingInventory

        adminSession = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory

        normalSession = logInByAccount {
            accountName = accountInventory.name
            password = "password"
        } as SessionInventory

        attachPredefineRoles(accountInventory.uuid, "image", "configuration", "networks", "vm", "volume", "snapshot")
    }

    void setQuotaAndShareResource() {

        def images = queryImage {
            sessionId = normalSession.uuid
        } as List<ImageInventory>

        def diskOfferings = queryDiskOffering {
            sessionId = normalSession.uuid
        } as List<DiskOfferingInventory>

        def l3NetWorks = queryL3Network {
            sessionId = normalSession.uuid
        } as List<L3NetworkInventory>

        def instanceOfferings = queryInstanceOffering {
            sessionId = normalSession.uuid
        } as List<InstanceOfferingInventory>

        assert images.size() == 0
        assert l3NetWorks.size() == 0
        assert diskOfferings.size() == 0
        assert instanceOfferings.size() == 0

        shareResource {
            resourceUuids = [diskOfferingInventory.uuid, l3.uuid, image.uuid, instanceOffering.uuid]
            toPublic = true
            sessionId = adminSession.uuid
        }

        updateQuota {
            identityUuid = accountInventory.uuid
            name = VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM
            value = 5
            sessionId = adminSession.uuid
        }

        vm = createVmInstance {
            name = "test-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            sessionId = normalSession.uuid
        } as VmInstanceInventory
    }

    void testSnapShotOnRootVolumeWithQuota() {
        withAccountSession("normal", "password") {
            List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
            for (int i = 0; i < 5; i++) {
                VolumeSnapshotInventory snapshotInv = createVolumeSnapshot {
                    volumeUuid = vm.getRootVolumeUuid()
                    name = String.format("test-snapshot-%s", i.toString())
                    sessionId = normalSession.uuid
                } as VolumeSnapshotInventory
                snapshotInvList.add(snapshotInv)
            }

            expect(AssertionError.class) {
                createVolumeSnapshot {
                    volumeUuid = vm.getRootVolumeUuid()
                    name = "test-snapshot-5"
                    sessionId = normalSession.uuid
                }
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 5
            assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInvList.get(4).uuid)
                    .eq(VolumeSnapshotVO_.latest, true)
                    .eq(VolumeSnapshotVO_.distance,5)
                    .find()

            stopVmInstance {
                uuid = vm.uuid
            }

            revertVolumeFromSnapshot {
                uuid = snapshotInvList.get(3).uuid
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 5
            assert Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.latest).eq(VolumeSnapshotVO_.uuid, snapshotInvList.get(3).uuid).findValue()

            deleteVolumeSnapshot {
                uuid = snapshotInvList.get(3).uuid
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 3
            assert Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.latest).eq(VolumeSnapshotVO_.uuid, snapshotInvList.get(2).uuid).findValue()
        }
    }

    void testSnapShotOnDataVolumeWithQuota() {
        withAccountSession("normal", "password") {

            startVmInstance {
                uuid = vm.uuid
            }

            def volume = createDataVolume {
                name = "test-data-volume"
                diskOfferingUuid = diskOfferingInventory.uuid

            } as VolumeInventory

            attachDataVolumeToVm {
                volumeUuid = volume.uuid
                vmInstanceUuid = vm.uuid
            }


            List<VolumeSnapshotInventory> snapshotInvList = new ArrayList<>()
            for (int i = 0; i < 2; i++) {
                VolumeSnapshotInventory snapshotInv = createVolumeSnapshot {
                    volumeUuid = volume.uuid
                    name = String.format("test-volume-snapshot-%s", i.toString())
                } as VolumeSnapshotInventory
                snapshotInvList.add(snapshotInv)
            }

            expect(AssertionError.class) {
                createVolumeSnapshot {
                    volumeUuid = volume.uuid
                    name = "test-volume-snapshot-2"
                }
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 5
            assert Q.New(VolumeSnapshotTreeVO.class).count() == 2
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotInvList.get(1).uuid)
                    .eq(VolumeSnapshotVO_.latest, true)
                    .eq(VolumeSnapshotVO_.distance,2)
                    .find()


            stopVmInstance {
                uuid = vm.uuid
            }

            revertVolumeFromSnapshot {
                uuid = snapshotInvList.get(0).uuid
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 5
            assert Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.latest).eq(VolumeSnapshotVO_.uuid, snapshotInvList.get(0).uuid).findValue()

            deleteVolumeSnapshot {
                uuid = snapshotInvList.get(0).uuid
            }

            assert Q.New(VolumeSnapshotVO.class).count() == 3
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volume.uuid).count() == 0
            assert Q.New(VolumeSnapshotTreeVO.class).count() == 1
        }
    }
}
