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
import org.zstack.mevoco.MevocoGlobalConfig
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
        logger.info("Test 001~005: VM test migration success, check reserved cache. kvm1 -> kvm2 success")

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

        logger.info("Test 006~010: VM test migration failed, check reserved cache. kvm2 -> kvm1 failed")
        def kvm1CapacityBefore6 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        def kvm2CapacityBefore6 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
        long targetMemory6 = vm1.memorySize.intdiv(targetMemoryRatio)
        boolean reachedAgent6 = false

        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp ->
            reachedAgent6 = true

            def actual = dbFindByUuid(vm1.uuid, VmInstanceVO)
            assert actual.state == VmInstanceState.Migrating : "VM state: expected=Migrating actual=${actual.state}"
            assert actual.hostUuid == kvm2.uuid : "In-flight source: expected=${kvm2.uuid} actual=${actual.hostUuid}"

            def list006 = bean(HostCapacityReserveManager).reservedByVmMigration(kvm1.uuid)
            assert list006.size() == 1
            assert list006[0].vmUuid == vm1.uuid

            assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

            new HostCapacityUpdater(kvm1.uuid).run({ HostCapacityVO cap ->
                cap.availableMemory = kvm1CapacityBefore6.availableMemory - targetMemory6 - 1
                cap.availableCpu = kvm1CapacityBefore6.availableCpu - vm1.cpuNum - 1
                return cap
            } as HostCapacityUpdaterRunnable)

            def recalcMsg6 = new RecalculateHostCapacityMsg()
            def done6 = new CountDownLatch(1)
            recalcMsg6.hostUuid = kvm1.uuid
            bean(CloudBus).makeLocalServiceId(recalcMsg6, HostAllocatorConstant.SERVICE_ID)
            bean(CloudBus).send(recalcMsg6, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    done6.countDown()
                }
            })
            assert done6.await(3, TimeUnit.SECONDS)
            def actual61 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
            assert actual61.availableMemory == kvm1CapacityBefore6.availableMemory - targetMemory6
            assert actual61.availableCpu == kvm1CapacityBefore6.availableCpu - vm1.cpuNum
            def actual62 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
            assert actual62.availableMemory == kvm2CapacityBefore6.availableMemory
            assert actual62.availableCpu == kvm2CapacityBefore6.availableCpu

            rsp.error = "on purpose"
            return rsp
        }

        expectApiFailure({
            migrateVm {
                delegate.vmInstanceUuid = vm1.uuid
                delegate.hostUuid = kvm1.uuid
            }
        }) {
            assert delegate.code == "HOST.1009"
        }

        assert reachedAgent6
        assert dbFindByUuid(vm1.uuid, VmInstanceVO).hostUuid == kvm2.uuid // not changed

        def recalcMsg9 = new RecalculateHostCapacityMsg()
        def done9 = new CountDownLatch(1)
        recalcMsg9.clusterUuid = kvm1.clusterUuid
        bean(CloudBus).makeLocalServiceId(recalcMsg9, HostAllocatorConstant.SERVICE_ID)
        bean(CloudBus).send(recalcMsg9, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done9.countDown()
            }
        })
        assert done9.await(3, TimeUnit.SECONDS)
        def actual91 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        assert actual91.availableMemory == kvm1CapacityBefore6.availableMemory
        assert actual91.availableCpu == kvm1CapacityBefore6.availableCpu
        def actual92 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
        assert actual92.availableMemory == kvm2CapacityBefore6.availableMemory
        assert actual92.availableCpu == kvm2CapacityBefore6.availableCpu

        assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

        env.cleanAfterSimulatorHandlers()

        logger.info("Test 011~015: VM test migration success, ratios = 3.5. kvm2 -> kvm1 success")
        double targetMemoryRatio11 = 3.5
        updateGlobalConfig {
            delegate.category = MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.category
            delegate.name = MevocoGlobalConfig.MEMORY_OVER_PROVISIONING_RATIO.name
            delegate.value = Double.toString(targetMemoryRatio11)
        }

        def recalcMsg10 = new RecalculateHostCapacityMsg()
        def done10 = new CountDownLatch(1)
        recalcMsg10.hostUuid = kvm2.uuid
        bean(CloudBus).makeLocalServiceId(recalcMsg10, HostAllocatorConstant.SERVICE_ID)
        bean(CloudBus).send(recalcMsg10, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done10.countDown()
            }
        })
        assert done10.await(3, TimeUnit.SECONDS)

        def kvm1CapacityBefore11 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        def kvm2CapacityBefore11 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
        long targetMemory11 = (long) ((vm1.memorySize as double) / targetMemoryRatio11)
        assert kvm2CapacityBefore11.availableMemory == kvm2CapacityBefore11.totalMemory - targetMemory11
        boolean reachedAgent11 = false

        env.afterSimulator(KVM_MIGRATE_VM_PATH) { rsp ->
            reachedAgent11 = true

            def actual = dbFindByUuid(vm1.uuid, VmInstanceVO)
            assert actual.state == VmInstanceState.Migrating : "VM state: expected=Migrating actual=${actual.state}"
            assert actual.hostUuid == kvm2.uuid : "In-flight source: expected=${kvm2.uuid} actual=${actual.hostUuid}"

            def list011 = bean(HostCapacityReserveManager).reservedByVmMigration(kvm1.uuid)
            assert list011.size() == 1
            assert list011[0].vmUuid == vm1.uuid

            assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

            new HostCapacityUpdater(kvm1.uuid).run({ HostCapacityVO cap ->
                cap.availableMemory = kvm1CapacityBefore11.availableMemory - targetMemory11 - 1
                cap.availableCpu = kvm1CapacityBefore11.availableCpu - vm1.cpuNum - 1
                return cap
            } as HostCapacityUpdaterRunnable)

            def recalcMsg11 = new RecalculateHostCapacityMsg()
            def done11 = new CountDownLatch(1)
            recalcMsg11.hostUuid = kvm1.uuid
            bean(CloudBus).makeLocalServiceId(recalcMsg11, HostAllocatorConstant.SERVICE_ID)
            bean(CloudBus).send(recalcMsg11, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    done11.countDown()
                }
            })
            assert done11.await(3, TimeUnit.SECONDS)
            def actual111 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
            assert actual111.availableMemory == kvm1CapacityBefore11.availableMemory - targetMemory11
            assert actual111.availableCpu == kvm1CapacityBefore11.availableCpu - vm1.cpuNum

            retryInSecs {
                def actual112 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
                assert actual112.availableMemory == kvm2CapacityBefore11.availableMemory
                assert actual112.availableCpu == kvm2CapacityBefore11.availableCpu
            }

            return rsp
        }

        migrateVm {
            delegate.vmInstanceUuid = vm1.uuid
            delegate.hostUuid = kvm1.uuid
        }

        assert reachedAgent11
        assert dbFindByUuid(vm1.uuid, VmInstanceVO).hostUuid == kvm1.uuid

        def recalcMsg14 = new RecalculateHostCapacityMsg()
        def done14 = new CountDownLatch(1)
        recalcMsg14.clusterUuid = kvm1.clusterUuid
        bean(CloudBus).makeLocalServiceId(recalcMsg14, HostAllocatorConstant.SERVICE_ID)
        bean(CloudBus).send(recalcMsg14, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done14.countDown()
            }
        })
        assert done14.await(3, TimeUnit.SECONDS)
        def actual141 = dbFindByUuid(kvm1.uuid, HostCapacityVO)
        assert actual141.availableMemory == kvm1CapacityBefore11.availableMemory - targetMemory11
        assert actual141.availableCpu == kvm1CapacityBefore11.availableCpu - vm1.cpuNum
        def actual142 = dbFindByUuid(kvm2.uuid, HostCapacityVO)
        assert actual142.availableMemory == kvm2CapacityBefore11.availableMemory + targetMemory11
        assert actual142.availableCpu == kvm2CapacityBefore11.availableCpu + vm1.cpuNum

        assert Q.New(VmSchedHistoryVO).eq(VmSchedHistoryVO_.vmInstanceUuid, vm1.uuid).count() == 0

        env.cleanAfterSimulatorHandlers()
    }

    @Override
    void clean() {
        env.delete()
    }
}
