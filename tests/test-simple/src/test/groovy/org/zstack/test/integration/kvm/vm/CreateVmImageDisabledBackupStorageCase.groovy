package org.zstack.test.integration.kvm.vm

import org.zstack.header.image.ImageConstant
import org.zstack.sdk.ApiException
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

class CreateVmImageDisabledBackupStorageCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    BackupStorageInventory bs

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            createVmDisabledBackupStorage()
            addCreateImageDisabledBackupStorage()
        }
    }

    void createVmDisabledBackupStorage() {
        vm = env.inventoryByName("vm")
        bs = env.inventoryByName("sftp")

        changeBackupStorageState {
            delegate.uuid = bs.uuid
            delegate.stateEvent = "disable"
        }

        createVmInstance {
            name = "test"
            instanceOfferingUuid =  vm.instanceOfferingUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            imageUuid = vm.imageUuid
        }

        changeImageState {
            delegate.uuid = vm.imageUuid
            delegate.stateEvent = "disable"
        }

        expect (AssertionError) {
            createVmInstance {
                name = "test"
                instanceOfferingUuid =  vm.instanceOfferingUuid
                l3NetworkUuids = [vm.defaultL3NetworkUuid]
                imageUuid = vm.imageUuid
            }
        }

        changeBackupStorageState {
            delegate.uuid = bs.uuid
            delegate.stateEvent = "enable"
        }

    }

    void addCreateImageDisabledBackupStorage() {
        changeBackupStorageState {
            delegate.uuid = bs.uuid
            delegate.stateEvent = "disable"
        }

        expect (AssertionError) {
            addImage {
                name = "image4"
                url = "/my-site/foo.qcow2"
                backupStorageUuids = [bs.uuid]
                format = ImageConstant.QCOW2_FORMAT_STRING
            }
        }

        expect (AssertionError) {
            createRootVolumeTemplateFromRootVolume {
                delegate.name = 'test'
                delegate.backupStorageUuids = [bs.uuid]
                delegate.rootVolumeUuid = vm.getRootVolumeUuid()
            }
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}

