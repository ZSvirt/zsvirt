package org.zstack.test.integration.storage.volume

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2018/9/28.*/
class ParallelDetachVolumeFromVmCase extends SubCase {
    EnvSpec env
    VolumeInventory volume
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testParallelDetachVolume(5)
        }
    }

    @Override
    void clean() {
        env.delete()
    }
    
    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        volume = queryVolume {
            conditions = ["type=Data"]
        }[0] as VolumeInventory
    }

    void testParallelDetachVolume(int times) {
        def threads = []

        def detached = 0

        env.afterSimulator(KVMConstant.KVM_DETACH_VOLUME) { rsp, HttpEntity<String> e ->
            detached ++
            return rsp
        }

        for (i in 1..times) {
            def t = Thread.start {
                detachDataVolumeFromVm {
                    uuid = volume.uuid
                    vmUuid = vm.uuid
                }
            }
            threads.add(t)
        }
        threads.each { it.join() }

        volume = queryVolume {
            conditions = ["type=Data"]
        }[0] as VolumeInventory

        assert volume.vmInstanceUuid == null
        assert volume.deviceId == null
        assert detached == 1
    }
}
