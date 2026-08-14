package org.zstack.test.integration.kvm.hostallocator

import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.GetCpuMemoryCapacityResult

class AllocateHostRollBackCase extends SubCase {
    EnvSpec env

    HostInventory host

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
        spring {
            include("HostAllocateExtension.xml")
        }
    }

    @Override
    void environment() {
        env = CephEnv.oneCephBSandtwoPs()
    }

    @Override
    void test() {
        env.create {
            testAllocateMetFailAtExtensionPointAndSuccessfullyRollback()
        }
    }

    void testAllocateMetFailAtExtensionPointAndSuccessfullyRollback() {
        host = env.inventoryByName("host6") as HostInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        HostAllocateExtension ext = bean(HostAllocateExtension.class)
        ext.setErrorOut(true)

        GetCpuMemoryCapacityResult beforeChangedCapacity = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
        } as GetCpuMemoryCapacityResult

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm1"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                cpuNum = 2
                memorySize = gb(4)
                hostUuid = host.uuid
            } as VmInstanceInventory
        }

        retryInSecs {
            GetCpuMemoryCapacityResult afterChangedCapacity = getCpuMemoryCapacity {
                hostUuids = [host.uuid]
            } as GetCpuMemoryCapacityResult

            assert afterChangedCapacity.availableCpu == beforeChangedCapacity.availableCpu
            assert afterChangedCapacity.availableMemory == beforeChangedCapacity.availableMemory
        }

        ext.setErrorOut(false)
    }
}

