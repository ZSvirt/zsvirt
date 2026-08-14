package org.zstack.test.integration.core.cloudbus

import org.zstack.core.Platform
import org.zstack.core.cloudbus.*
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.AbstractService
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.progress.ChainInfo
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.message.APIEvent
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.vm.APIQueryVmInstanceMsg
import org.zstack.header.vm.APIStartVmInstanceMsg
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class CloudBus3Case extends SubCase {
    AbstractService service
    CloudBus bus

    @Override
    void clean() {
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    void testStepSend() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testStepSend"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                bus.reply(msg, new MessageReply())
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(SERVICE_ID)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        List<StartVmInstanceMsg> msgs = []
        for (int i=0; i<3; i++) {
            StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
            bus.makeLocalServiceId(msg, SERVICE_ID)
            msgs.add(msg)
        }

        int count = 0
        CountDownLatch latch = new CountDownLatch(3)
        bus.send(msgs, 2, new CloudBusSteppingCallback(null) {
            @Override
            synchronized void run(NeedReplyMessage msg, MessageReply reply) {
                count ++
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assert count == 3

        bus.unregisterService(service)
    }

    void testManagementNodeGone() {

        CloudBus bus = bean(CloudBus.class)
        CloudBus3ManagementNodeLifeCycleTracker tracker = bean(CloudBus3ManagementNodeLifeCycleTracker.class)
        String SERVICE_ID = "nodeGone"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(SERVICE_ID)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        MessageReply errorReply
        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                errorReply = reply
            }
        })

        tracker.nodeLeft(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorReply
            assert !errorReply.isSuccess()
            assert errorReply.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        APIEvent errorEvent
        APIStartVmInstanceMsg amsg = new APIStartVmInstanceMsg()
        bus.makeLocalServiceId(amsg, SERVICE_ID)
        bus.send(amsg, new Consumer<APIEvent>() {
            @Override
            void accept(APIEvent apiEvent) {
                errorEvent = apiEvent
            }
        })

        tracker.nodeLeft(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorEvent
            assert !errorEvent.isSuccess()
            assert errorEvent.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        errorReply = null
        APIQueryVmInstanceMsg qmsg = new APIQueryVmInstanceMsg()
        bus.makeLocalServiceId(qmsg, SERVICE_ID)
        bus.send(qmsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                errorReply = reply
            }
        })

        tracker.iAmDead(new ManagementNodeInventory(uuid: Platform.getManagementServerId()))

        retryInSecs {
            assert errorReply
            assert !errorReply.isSuccess()
            assert errorReply.getError().getCode() == SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR.toString()
        }

        int cleanupInterval = CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL
        CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL = 1
        tracker.startCleanupTimer()

        qmsg = new APIQueryVmInstanceMsg()
        qmsg.setTimeout(2L)
        bus.makeLocalServiceId(qmsg, SERVICE_ID)
        bus.send(qmsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
            }
        })

        retryInSecs {
            assert tracker.messageTrackers.values().find { it.message.id == qmsg.id } == null
        }

        bus.unregisterService(service)
        CloudBusGlobalProperty.CLOUDBUS3_MESSAGE_TRACKER_CLEANUP_INTERVAL = cleanupInterval
    }

    void testSendToMissingNode() {
        CloudBus bus = bean(CloudBus.class)
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        String SERVICE_ID = "testMissingNode"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                bus.reply(msg, new MessageReply())
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(SERVICE_ID)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        def qmsg = new APIQueryVmInstanceMsg()
        qmsg.setTimeout(1L)
        bus.makeServiceIdByManagementNodeId(qmsg, SERVICE_ID, "some-fake-uuid")

        MessageReply r = bus.call(qmsg)
        assert r.error.isError(SysErrors.TIMEOUT)

        qmsg = new APIQueryVmInstanceMsg()
        bus.makeServiceIdByManagementNodeId(qmsg, SERVICE_ID, "some-fake-uuid2")

        expect(OperationFailureException.class) {
            bus.call(qmsg)
        }

        bus.unregisterService(service)
    }

    void testFutureCompletionSend() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSend"

        if (service == null) {
            service = new AbstractService() {
                @Override
                void handleMessage(Message msg) {
                    bus.reply(msg, new MessageReply())
                }

                @Override
                String getId() {
                    return bus.makeLocalServiceId(SERVICE_ID)
                }

                @Override
                boolean start() {
                    return true
                }

                @Override
                boolean stop() {
                    return true
                }
            }

            bus.registerService(service)
        }

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()
        assert !completion.isTimeout()

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(nrMsg, SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        assert !completion2.isTimeout()
        retryInSecs {
            assert replyHolder[0] != null
            assert replyHolder[0].isSuccess()
        }
    }

    void testFutureCompletionSendToNonExistService() {
        CloudBus bus = bean(CloudBus.class)
        String NON_EXIST_SERVICE_ID = "NonExistService" + Platform.uuid

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(msg, NON_EXIST_SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()


        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        bus.makeLocalServiceId(nrMsg, NON_EXIST_SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
        }
    }

    void testFutureCompletionSendTimeout() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSendTimeout"

        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                // Do not reply to simulate timeout
                try {
                    Thread.sleep(3000)
                } catch (InterruptedException e) {
                    e.printStackTrace()
                }
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(SERVICE_ID)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        // test timeout (Message msg)
        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        msg.setTimeout(1)
        bus.makeLocalServiceId(msg, SERVICE_ID)
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert completion.isSuccess()

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        nrMsg.setTimeout(1)
        bus.makeLocalServiceId(nrMsg, SERVICE_ID)
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert completion2.isSuccess()
        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
            assert replyHolder[0].error.isError(SysErrors.TIMEOUT)
        }

        bus.unregisterService(service)
    }

    void testFutureCompletionSendToMissingNode() {
        CloudBus bus = bean(CloudBus.class)
        String SERVICE_ID = "testFutureCompletionSendToMissingNode"

        StartVmInstanceMsg msg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        msg.setTimeout(2)
        bus.makeServiceIdByManagementNodeId(msg, SERVICE_ID, "fake-management-node-uuid")
        FutureCompletion completion = bus.send(msg)
        completion.await(5000L)
        assert !completion.isSuccess()
        assert completion.getErrorCode() != null

        MessageReply[] replyHolder = new MessageReply[1]
        NeedReplyMessage nrMsg = new StartVmInstanceMsg(vmInstanceUuid: Platform.uuid)
        nrMsg.setTimeout(2)
        bus.makeServiceIdByManagementNodeId(nrMsg, SERVICE_ID, "fake-management-node-uuid")
        FutureCompletion completion2 = bus.send(nrMsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                replyHolder[0] = reply
            }
        })
        completion2.await(5000L)
        assert !completion2.isSuccess()
        assert completion2.getErrorCode() != null

        retryInSecs {
            assert replyHolder[0] != null
            assert !replyHolder[0].isSuccess()
            // TODO
            // assert replyHolder[0].error.isError(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR)
        }
    }

    void testSendQueue() {
        def threads = []
        1.upto(50) { i ->
            Thread t = Thread.start {
                new MessageSender().sendMsg()
            }
            threads << t
        }

        ThreadFacade thdf = bean(ThreadFacade.class)

        ChainInfo info = thdf.getChainTaskInfo("http-send-in-queue")
        assert info.runningTask.size() <= 1
        sleep(100)
        info = thdf.getChainTaskInfo("http-send-in-queue")
        assert info.runningTask.size() <= 1

        threads.each { it.join() }
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)

        testStepSend()
        testManagementNodeGone()
        testSendToMissingNode()
        testFutureCompletionSend()
        testFutureCompletionSendToNonExistService()
        testFutureCompletionSendTimeout()
        testFutureCompletionSendToMissingNode()
        CloudBusGlobalProperty.HTTP_ALWAYS = true

        testFutureCompletionSend()
        testFutureCompletionSendToNonExistService()
        testFutureCompletionSendTimeout()
        testFutureCompletionSendToMissingNode()

        testSendQueue()
        bus.unregisterService(service)
        CloudBusGlobalProperty.HTTP_ALWAYS = false
    }
}
