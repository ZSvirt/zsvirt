package org.zstack.test.integration.storage.primary.local.psmaintenance

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.ImageSpec 
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec
import org.zstack.compute.vm.VmGlobalConfig

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageMaintenancePrimaryStorageDetachIsoCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageDetachIsoWhenPrimaryStorageIsMaintained()
        }
    }


    void testLocalStorageDetachIsoWhenPrimaryStorageIsMaintained() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("test-vm")
        String vmUuid = vmSpec.inventory.uuid
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        def attach_iso_path_is_invoked = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            attach_iso_path_is_invoked = true
            return rsp
        }
        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid
            sessionId = currentEnvSpec.session.uuid
        }
        assert attach_iso_path_is_invoked


        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Maintenance


        def detach_iso_path_is_invoked = false
        env.afterSimulator(KVMConstant.KVM_DETACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            //cmd2 = json(e.body, LinkedHashMap.class)
            detach_iso_path_is_invoked = true
            return rsp
        }
        detachIsoFromVmInstance {
            vmInstanceUuid = vmUuid
            sessionId = currentEnvSpec.session.uuid
        }
        assert !detach_iso_path_is_invoked


        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
