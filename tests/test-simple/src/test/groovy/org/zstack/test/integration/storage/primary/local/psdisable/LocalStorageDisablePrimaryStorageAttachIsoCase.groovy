package org.zstack.test.integration.storage.primary.local.psdisable

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
class LocalStorageDisablePrimaryStorageAttachIsoCase extends SubCase{
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
            testLocalStorageAttachIsoWhenPrimaryStorageIsDisabled()
        }
    }


    void testLocalStorageAttachIsoWhenPrimaryStorageIsDisabled() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec = env.specByName("test-vm")
        String vmUuid = vmSpec.inventory.uuid
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        Map cmd1 = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            cmd1 = json(e.body, LinkedHashMap.class)
            return rsp
        }
        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid
            sessionId = currentEnvSpec.session.uuid
        }
        assert cmd1 != null

        Map cmd2 = null
        env.afterSimulator(KVMConstant.KVM_DETACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            cmd2 = json(e.body, LinkedHashMap.class)
            return rsp
        }
        detachIsoFromVmInstance {
            vmInstanceUuid = vmUuid
            sessionId = currentEnvSpec.session.uuid
        }
        assert cmd2 != null

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled


        Map cmd3 = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            cmd3 = json(e.body, LinkedHashMap.class)
            return rsp
        }

        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid
            sessionId = currentEnvSpec.session.uuid
        }

        assert cmd3 != null

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
