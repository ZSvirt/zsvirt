package org.zstack.test.integration.kvm.status

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceEO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeEO
import org.zstack.sdk.ChangePrimaryStorageStateAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.*
/**
 * Created by david on 3/7/17.
 */
class DBOnlyCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
            include("vyos.xml")
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testDeleteCreatedVm()
            testDeletePrimaryStorage()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testDeleteCreatedVm() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        def image = env.specByName("image1") as ImageSpec
        def offering = env.specByName("instanceOffering") as InstanceOfferingSpec
        def l3network = env.specByName("pubL3") as L3NetworkSpec

        def vm = createVmInstance {
            name = "VM-2"
            instanceOfferingUuid = offering.inventory.uuid
            imageUuid = image.inventory.uuid
            l3NetworkUuids = [l3network.inventory.uuid]
            strategy = VmCreationStrategy.JustCreate
        } as VmInstanceInventory

        def vmvo = dbf.findByUuid(vm.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Created

        destroyVmInstance {
            uuid = vm.uuid
        }

        assert !dbf.isExist(vm.uuid, VmInstanceVO.class)
    }

    void testDeletePrimaryStorage() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        def ps = env.specByName("local") as PrimaryStorageSpec
        ChangePrimaryStorageStateAction action = new ChangePrimaryStorageStateAction()
        action.stateEvent = PrimaryStorageStateEvent.maintain
        action.uuid = ps.inventory.uuid
        action.sessionId = env.session.uuid

        ChangePrimaryStorageStateAction.Result res = action.call()
        assert res.error == null

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.inventory.uuid
            clusterUuid = ps.inventory.attachedClusterUuids[0]
        }

        deletePrimaryStorage {
            uuid = ps.inventory.uuid
        }

        assert dbf.listAll(VmInstanceEO.class).isEmpty()
        assert dbf.listAll(VolumeEO.class).isEmpty()
    }
}
