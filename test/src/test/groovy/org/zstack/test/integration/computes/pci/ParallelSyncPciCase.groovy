package org.zstack.test.integration.computes.pci

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.Constants
import org.zstack.header.message.MessageReply
import org.zstack.pciDevice.KvmPciDeviceBackend.PciDeviceKvmBackend
import org.zstack.pciDevice.header.PciDeviceConstants
import org.zstack.pciDevice.message.SyncPciDeviceInDbMsg
import org.zstack.pciDevice.message.SyncPciDeviceMsg
import org.zstack.sdk.KVMHostInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ParallelSyncPciCase extends SubCase {
    EnvSpec env

    KVMHostInventory host1
    KVMHostInventory host2

    @Override
    void setup() {
        useSpring(PciTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.11"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.12"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testDifferentHostsSyncInParallel()
            testSameHostSyncSerially()
        }
    }

    void prepare() {
        host1 = env.inventoryByName("kvm1") as KVMHostInventory
        host2 = env.inventoryByName("kvm2") as KVMHostInventory
    }

    void testDifferentHostsSyncInParallel() {
        logger.info("Test 001: testDifferentHostsSyncInParallel")

        def entered = new CountDownLatch(2)
        def release = new CountDownLatch(1)
        def completed = new CountDownLatch(2)
        def replies = Collections.synchronizedList([]) as List<MessageReply>
        def syncMsgs = Collections.synchronizedList([]) as List<SyncPciDeviceInDbMsg>

        // mock slow response
        env.simulator(PciDeviceKvmBackend.GET_PCI_DEVICES) { HttpEntity<String> entity, EnvSpec spec ->
            String hostUuid = entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            assert [host1.uuid, host2.uuid].contains(hostUuid) :
                    "unexpected GET_PCI_DEVICES host: expected=${[host1.uuid, host2.uuid]} actual=${hostUuid}"

            entered.countDown()
            release.await(10, TimeUnit.SECONDS)
            return PciTest.m60Response()
        }

        def cleaner = notifyWhenReceivedMessage(SyncPciDeviceInDbMsg) { SyncPciDeviceInDbMsg msg ->
            syncMsgs.add(msg)
        }

        def bus = bean(CloudBus)
        [host1, host2].each { host ->
            def msg = new SyncPciDeviceMsg()
            msg.hostUuid = host.uuid
            msg.hostIommuState = "Enabled"
            bus.makeTargetServiceIdByResourceUuid(msg, PciDeviceConstants.SERVICE_ID, msg.hostUuid)
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    replies.add(reply)
                    completed.countDown()
                }
            })
        }

        try {
            assert entered.await(5, TimeUnit.SECONDS) :
                        "different hosts must collect PCI data concurrently: expected=2 actual=${2 - entered.count}"

            release.countDown()
            assert completed.await(30, TimeUnit.SECONDS) :
                        "different-host PCI sync did not complete: expected=2 actual=${replies.size()}"

            assert replies.size() == 2
            assert replies.every { it.success } :
                    "different-host PCI sync failed: errors=${replies.findAll { !it.success }*.error}"
        } finally {
            release.countDown()
            cleaner.call()
        }

        assert syncMsgs.size() == 2 :
                    "each host snapshot must send one DB sync message: expected=2 actual=${syncMsgs.size()}"
    }

    void testSameHostSyncSerially() {
        logger.info("Test 011: testSameHostSyncSerially")

        def firstEntered = new CountDownLatch(1)
        def secondEntered = new CountDownLatch(1)
        def releaseFirst = new CountDownLatch(1)
        def completed = new CountDownLatch(2)
        def calls = new AtomicInteger()
        def replies = Collections.synchronizedList([]) as List<MessageReply>

        env.simulator(PciDeviceKvmBackend.GET_PCI_DEVICES) { HttpEntity<String> entity, EnvSpec spec ->
            assert entity.headers.getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID) == host1.uuid :
                    "same-host test called an unexpected host"

            int call = calls.incrementAndGet()
            if (call == 1) {
                firstEntered.countDown()
                releaseFirst.await(10, TimeUnit.SECONDS)
            } else {
                secondEntered.countDown()
            }
            return PciTest.m60Response()
        }

        def bus = bean(CloudBus)
        try {
            def msg1 = new SyncPciDeviceMsg()
            msg1.hostUuid = host1.uuid
            msg1.hostIommuState = "Enabled"
            bus.makeTargetServiceIdByResourceUuid(msg1, PciDeviceConstants.SERVICE_ID, msg1.hostUuid)
            bus.send(msg1, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    replies.add(reply)
                    completed.countDown()
                }
            })

            assert firstEntered.await(5, TimeUnit.SECONDS) :
                    "first same-host PCI sync did not reach GET_PCI_DEVICES"

            def msg2 = new SyncPciDeviceMsg()
            msg2.hostUuid = host1.uuid // still host1
            msg2.hostIommuState = "Enabled"
            bus.makeTargetServiceIdByResourceUuid(msg2, PciDeviceConstants.SERVICE_ID, msg2.hostUuid)
            bus.send(msg2, new CloudBusCallBack(null) {
                @Override
                void run(MessageReply reply) {
                    replies.add(reply)
                    completed.countDown()
                }
            })

            assert !secondEntered.await(1, TimeUnit.SECONDS) :
                    "same-host PCI sync overlapped while the first request was blocked"
        } finally {
            releaseFirst.countDown()
        }

        assert completed.await(30, TimeUnit.SECONDS) :
                "same-host PCI sync replies did not complete: expected=2 actual=${replies.size()}"
        assert secondEntered.await(5, TimeUnit.SECONDS) :
                "second same-host PCI sync never entered GET_PCI_DEVICES"
        assert calls.get() == 2 :
                "same-host PCI sync did not execute both requests: expected=2 actual=${calls.get()}"
        assert replies.every { it.success } :
                "same-host PCI sync failed: errors=${replies.findAll { !it.success }*.error}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
