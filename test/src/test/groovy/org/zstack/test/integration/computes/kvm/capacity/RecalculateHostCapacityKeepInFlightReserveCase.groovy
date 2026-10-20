package org.zstack.test.integration.computes.kvm.capacity

import org.zstack.compute.allocator.HostCapacityReserveManager
import org.zstack.compute.allocator.HostCapacityUpdater
import org.zstack.compute.allocator.HostCapacityUpdaterRunnable
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.header.allocator.HostAllocatorConstant
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.host.RecalculateHostCapacityMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.header.vm.VmSchedHistoryVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.computes.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static org.zstack.kvm.KVMConstant.KVM_MIGRATE_VM_PATH

class RecalculateHostCapacityKeepInFlightReserveCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm1
    HostInventory kvm1, kvm2

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = KvmTest.cephPsCephBs2Host()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testMigrationCapacity()
        }
    }

    void prepare() {
        vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        kvm1 = env.inventoryByName("kvm1") as HostInventory
        kvm2 = env.inventoryByName("kvm2") as HostInventory
    }

    void testMigrationCapacity() {
        logger.info("Test 001: VM test migration success, check reserved cache")

        def vm001List = queryVmInstance {
            delegate.conditions = [
                "uuid=${vm1.uuid}"
            ]
        } as List<VmInstanceInventory>
        assert vm001List.size() == 1
        assert vm001List[0].hostUuid == kvm1.uuid

        def kvm1CapacityBefore = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        def kvm2CapacityBefore = dbFindByUuid(kvm2.uuid, HostCapacityVO)

        int sourceMemoryRatio = 1
        int targetMemoryRatio = 1
        long targetMemory = vm1.memorySize.intdiv(targetMemoryRatio)
        long sourceMemory = vm1.memorySize.intdiv(sourceMemoryRatio)
        boolean reachedAgent = false

        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp ->
            reachedAgent = true

            def actual = dbFindByUuid(vm1.uuid, VmInstanceVO)
            assert actual.state == VmInstanceState.Migrating : "VM state: expected=Migrating actual=${actual.state}"
            assert actual.hostUuid == kvm1.uuid : "In-flight source: expected=${kvm1.uuid} actual=${actual.hostUuid}"

            def list001 = bean(HostCapacityReserveManager).reservedByVmMigration(kvm2.uuid)
            assert list001.size() == 1
            assert list001[0].vmUuid == vm1.uuid

            assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

            new HostCapacityUpdater(kvm2.uuid).run({ HostCapacityVO cap ->
                cap.availableMemory = kvm2CapacityBefore.availableMemory - targetMemory - 1
                cap.availableCpu = kvm2CapacityBefore.availableCpu - vm1.cpuNum - 1
                return cap
            } as HostCapacityUpdaterRunnable)

            def recalcMsg1 = new RecalculateHostCapacityMsg()
            def done1 = new CountDownLatch(1)
            recalcMsg1.hostUuid = kvm2.uuid
            bean(CloudBus).makeLocalServiceId(recalcMsg1, HostAllocatorConstant.SERVICE_ID)
            bean(CloudBus).send(recalcMsg1, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    done1.countDown()
                }
            })
            assert done1.await(3, TimeUnit.SECONDS)
            def actual11 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
            assert actual11.availableMemory == kvm1CapacityBefore.availableMemory
            assert actual11.availableCpu == kvm1CapacityBefore.availableCpu
            def actual12 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
            assert actual12.availableMemory == kvm2CapacityBefore.availableMemory - targetMemory
            assert actual12.availableCpu == kvm2CapacityBefore.availableCpu - vm1.cpuNum

            return rsp
        }

        migrateVm {
            delegate.vmInstanceUuid = vm1.uuid
            delegate.hostUuid = kvm2.uuid
        }

        assert reachedAgent
        assert dbFindByUuid(vm1.uuid, VmInstanceVO).hostUuid == kvm2.uuid

        def recalcMsg5 = new RecalculateHostCapacityMsg()
        def done5 = new CountDownLatch(1)
        recalcMsg5.clusterUuid = kvm2.clusterUuid
        bean(CloudBus).makeLocalServiceId(recalcMsg5, HostAllocatorConstant.SERVICE_ID)
        bean(CloudBus).send(recalcMsg5, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done5.countDown()
            }
        })
        assert done5.await(3, TimeUnit.SECONDS)
        def actual51 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        assert actual51.availableMemory == kvm1CapacityBefore.availableMemory + sourceMemory
        assert actual51.availableCpu == kvm1CapacityBefore.availableCpu + vm1.cpuNum
        def actual52 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
        assert actual52.availableMemory == kvm2CapacityBefore.availableMemory - targetMemory
        assert actual52.availableCpu == kvm2CapacityBefore.availableCpu - vm1.cpuNum

        assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

        env.cleanAfterSimulatorHandlers()
    }

    @Override
    void clean() {
        env.delete()
    }
}
