package org.zstack.test.integration.storage.volume

import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.volume.VolumeStatus
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetDataVolumeAttachableVmAction
import org.zstack.sdk.GetVmAttachableDataVolumeAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase


/**
 * Created by camile on 17-8-17.
 */
class GetAttachCadidatesCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    VolumeInventory adminVolume, normalAccountNotInstantiatedVolume, normalAccountReadyVolume
    DiskOfferingInventory disk
    L3NetworkInventory l3
    InstanceOfferingInventory instanceOffering
    KVMHostInventory host
    BackupStorageInventory bs
    PrimaryStorageInventory ps
    ImageInventory image
    SessionInventory normalSession
    VmInstanceInventory normalAccountVm

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepareEnvironment()
            testGetNotInstantiatedVolumeAttachableVmByNormalAccount()
            testGetReadyVolumeAttachableVmByNormalAccount()
            testGetCadidatesResultEquals()
            testGetCandidateVmType()
            testGetCadidatesForNotInstantiatedVolumeOnLocalPs()
        }
    }

    void prepareEnvironment(){
        disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        host = env.inventoryByName("kvm") as KVMHostInventory
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        ps = env.inventoryByName("local") as PrimaryStorageInventory
        image = env.inventoryByName("image1") as ImageInventory
        dbf = bean(DatabaseFacade.class)

        adminVolume = createDataVolume {
            name = "adminDisk"
            diskOfferingUuid = disk.uuid
        } as VolumeInventory

        shareResource {
            resourceUuids = [image.uuid, l3.uuid, instanceOffering.uuid, disk.uuid]
            toPublic = true
        }

        def normalAccount = createAccount {
            name = "test1"
            password = "password"
        } as AccountInventory
        attachPredefineRoles(normalAccount.uuid, "vm", "volume")

        normalSession = logInByAccount {
            accountName = normalAccount.name
            password = "password"
        } as SessionInventory

        normalAccountVm = createVmInstance {
            name = "vm-normal"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            sessionId = normalSession.uuid
        } as VmInstanceInventory

        normalAccountNotInstantiatedVolume = createDataVolume {
            name = "normalAccountNotInstantiatedVolume"
            diskOfferingUuid = disk.uuid
        } as VolumeInventory

        normalAccountReadyVolume = createDataVolume {
            name = "normalAccountReadyVolume"
            diskOfferingUuid = disk.uuid
            primaryStorageUuid = ps.uuid
            systemTags = systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        } as VolumeInventory
    }

    void testGetNotInstantiatedVolumeAttachableVmByNormalAccount(){
        List<VmInstanceInventory> notInstantiatedRets = getDataVolumeAttachableVm {
            volumeUuid = normalAccountNotInstantiatedVolume.uuid
            sessionId = normalSession.uuid
        } as List<VmInstanceInventory>

        assert notInstantiatedRets.size() == 1
        assert notInstantiatedRets.get(0).uuid == normalAccountVm.uuid
        assert normalAccountNotInstantiatedVolume.status == VolumeStatus.NotInstantiated.toString()
    }

    void testGetReadyVolumeAttachableVmByNormalAccount(){
        List<VmInstanceInventory> readyRets = getDataVolumeAttachableVm {
            volumeUuid = normalAccountReadyVolume.uuid
            sessionId = normalSession.uuid
        } as List<VmInstanceInventory>

        assert readyRets.size() == 1
        assert readyRets.get(0).uuid == normalAccountVm.uuid
        assert normalAccountReadyVolume.status == VolumeStatus.Ready.toString()
    }

    void testGetCadidatesResultEquals() {
        ImageInventory otherImage = addImage {
            name = "other"
            url = "http://zstack.org/download/test.iso"
            platform = ImagePlatform.Other.toString()
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }
        VmInstanceInventory vm1 = createVmInstance {
            name = "vm23"
            imageUuid = otherImage.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            hostUuid = host.uuid
        }

        GetDataVolumeAttachableVmAction action1 = new GetDataVolumeAttachableVmAction()
        action1.volumeUuid = adminVolume.uuid
        action1.sessionId = adminSession()
        GetDataVolumeAttachableVmAction.Result result1 = action1.call()

        GetVmAttachableDataVolumeAction action2 = new GetVmAttachableDataVolumeAction()
        action2.vmInstanceUuid = vm1.uuid
        action2.sessionId = adminSession()
        GetVmAttachableDataVolumeAction.Result result2 = action2.call()
        assert  0 == result2.value.inventories.size()
        assert  2 == result1.value.inventories.size() // 1 normal account, 1 admin
        for (VmInstanceInventory inventory : result1.value.inventories){
            assert inventory.uuid!= vm1.uuid
        }

        deleteImage {
            uuid = otherImage.uuid
        }

        expungeImage {
            imageUuid = otherImage.uuid
        }
        result1 = action1.call()
        result2 = action2.call()
        assert  0 == result2.value.inventories.size()
        assert  2 == result1.value.inventories.size() // 1 normal account, 1 admin
        for (VmInstanceInventory inventory : result1.value.inventories){
            assert inventory.uuid!= vm1.uuid
        }

        destroyVmInstance {
            uuid = vm1.uuid
        }

        expungeVmInstance {
            uuid = vm1.uuid
        }
    }

    void testGetCandidateVmType(){
        List<VmInstanceInventory> rets = getDataVolumeAttachableVm {
            volumeUuid = adminVolume.uuid
        } as List<VmInstanceInventory>

        rets.forEach({it -> assert it.type != ApplianceVmConstant.APPLIANCE_VM_TYPE})

        rets = getDataVolumeAttachableVm {
            volumeUuid = normalAccountReadyVolume.uuid
            sessionId = normalSession.uuid
        } as List<VmInstanceInventory>

        rets.forEach({it -> assert it.type != ApplianceVmConstant.APPLIANCE_VM_TYPE})
    }

    void testGetCadidatesForNotInstantiatedVolumeOnLocalPs() {
        PrimaryStorageCapacityVO ps = dbf.findByUuid(ps.uuid,PrimaryStorageCapacityVO.class)
        long psAvailableCapacity = ps.availableCapacity

        DiskOfferingInventory diskOfferingInventory = createDiskOffering {
            name = "test"
            diskSize = psAvailableCapacity + 1
        } as DiskOfferingInventory

        VolumeInventory notInstantiateVolume = createDataVolume {
            name = "test-dataVolume"
            diskOfferingUuid = diskOfferingInventory.uuid
        } as VolumeInventory

        GetDataVolumeAttachableVmAction action = new GetDataVolumeAttachableVmAction()
        action.volumeUuid = notInstantiateVolume.uuid
        action.sessionId = adminSession()
        GetDataVolumeAttachableVmAction.Result result = action.call()
        assert 0 == result.value.inventories.size()
    }

    @Override
    void clean() {
        SQL.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                .hardDelete()
        env.delete()
    }
}