package org.zstack.test.integration.core.cloudbus

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusGlobalConfig
import org.zstack.core.cloudbus.DeadMessageManagerImpl
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.AbstractService
import org.zstack.header.core.FutureCompletion
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.message.Message
import org.zstack.header.vm.StartVmInstanceMsg
import org.zstack.header.vm.StartVmInstanceReply
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig
import org.zstack.portal.managementnode.ManagementNodeManagerImpl
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class ManagementNodeNotFoundHandlerCase extends SubCase {
    DeadMessageManagerImpl mgr

    @Override
    void clean() {
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        mgr = bean(DeadMessageManagerImpl.class)
        mgr.managementNodeNotFoundHandlers.invalidateAll()
        mgr.managementNodeNotFoundHandlers.cleanUp()

        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.updateValue(1)
        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT.updateValue(180)
        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_NUM.updateValue(500)
    }

    Closure mockAManagementNode(String mgmtUuid) {
        ManagementNodeVO vo = new ManagementNodeVO(
                hostName: "127.0.0.10",
                // mock a future heartbeat
                heartBeat: new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5)).toTimestamp(),
                uuid: mgmtUuid,
                port: 8989,
                state: ManagementNodeState.RUNNING
        )

        DatabaseFacade dbf = bean(DatabaseFacade.class)
        dbf.persist(vo)

        // directly call the nodeJoin interface, mocking a canonical event will not work because the listener will check if the
        // event is from current node
        bean(ManagementNodeManagerImpl.class).nodeLifeCycle.nodeJoin(ManagementNodeInventory.valueOf(vo))

        return {
            dbf.removeByPrimaryKey(vo.getUuid(), ManagementNodeVO.class)
            bean(ManagementNodeManagerImpl.class).nodeLifeCycle.nodeLeft(ManagementNodeInventory.valueOf(vo))
        }
    }

    void testHanlderWorksNotWorkIfManagementNodeNotJoinedBefore() {
        CloudBus bus = bean(CloudBus.class)

        String secondNodeUuid = Platform.getUuid()
        String secondServiceName = "secondService"
        Message message = null
        def secondNodeService = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                logger.debug("received message[${msg.id}] on service[${getId()}]")
                message = msg
            }

            @Override
            String getId() {
                return bus.makeServiceIdByManagementNodeId(secondServiceName, secondNodeUuid)
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

        StartVmInstanceReply reply = new StartVmInstanceReply()
        bus.makeServiceIdByManagementNodeId(reply, secondServiceName, secondNodeUuid)
        FutureCompletion c = bus.send(reply)

        assert message == null

        bus.registerService(secondNodeService)

        c.await()
        assert !mgr.managementNodeNotFoundHandlers.asMap().values().message.contains(reply)
        Closure cleanup = mockAManagementNode(secondNodeUuid)

        sleep(1000)
        assert message == null

        cleanup()
        bus.unregisterService(secondNodeService)
    }

    void testHanlderWorksNotWorkIfMessageIsNotAReply() {
        CloudBus bus = bean(CloudBus.class)

        String secondNodeUuid = Platform.getUuid()
        String secondServiceName = "secondService"
        Message message = null
        def secondNodeService = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                logger.debug("received message[${msg.id}] on service[${getId()}]")
                message = msg
            }

            @Override
            String getId() {
                return bus.makeServiceIdByManagementNodeId(secondServiceName, secondNodeUuid)
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

        StartVmInstanceMsg msg = new StartVmInstanceMsg()
        bus.makeServiceIdByManagementNodeId(msg, secondServiceName, secondNodeUuid)
        FutureCompletion c = bus.send(msg)

        assert message == null

        bus.registerService(secondNodeService)
        c.await()
        assert !mgr.managementNodeNotFoundHandlers.asMap().values().message.contains(msg)
        Closure cleanup = mockAManagementNode(secondNodeUuid)

        sleep(1000)
        assert message == null

        cleanup()
        bus.unregisterService(secondNodeService)
    }

    void testHanlderWorks() {
        CloudBus bus = bean(CloudBus.class)

        String secondNodeUuid = Platform.getUuid()
        String secondServiceName = "secondService"
        Message message = null
        def secondNodeService = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                logger.debug("received message[${msg.id}] on service[${getId()}]")
                message = msg
            }

            @Override
            String getId() {
                return bus.makeServiceIdByManagementNodeId(secondServiceName, secondNodeUuid)
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

        DeadMessageManagerImpl mgr = bean(DeadMessageManagerImpl.class)
        mgr.previousOfflinedManagementNodes.put(secondNodeUuid, new ManagementNodeInventory(uuid: secondNodeUuid))

        StartVmInstanceReply reply = new StartVmInstanceReply()
        bus.makeServiceIdByManagementNodeId(reply, secondServiceName, secondNodeUuid)
        FutureCompletion c = bus.send(reply)

        assert message == null

        bus.registerService(secondNodeService)

        // send is a async operation, so we need to wait a bit
        c.await()
        assert mgr.managementNodeNotFoundHandlers.asMap().values().message.contains(reply)
        Closure cleanup = mockAManagementNode(secondNodeUuid)

        retryInSecs {
            logger.debug("waiting for reply sent to mgmt node[uuid: ${secondNodeUuid}]")
            assert message != null
            assert message.id == reply.id
        }

        cleanup()
        bus.unregisterService(secondNodeService)
    }

    void testHandlerDropsByTimeout() {
        CloudBus bus = bean(CloudBus.class)

        String secondNodeUuid = Platform.uuid
        DeadMessageManagerImpl mgr = bean(DeadMessageManagerImpl.class)
        mgr.previousOfflinedManagementNodes.put(secondNodeUuid, new ManagementNodeInventory(uuid: secondNodeUuid))

        StartVmInstanceReply reply = new StartVmInstanceReply()
        bus.makeServiceIdByManagementNodeId(reply, "fake service", secondNodeUuid)
        bus.send(reply)

        retryInSecs {
            assert mgr.managementNodeNotFoundHandlers.size() > 0
        }

        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT.updateValue(1)

        retryInSecs {
            // the expired items are not removed immediately but by a periodical routine decided by the cache implementation
            // we use cleanup() to trigger the routine in case of test
            mgr.managementNodeNotFoundHandlers.cleanUp()
            assert mgr.managementNodeNotFoundHandlers.size() == 0
        }
    }

    void testHandlerDropsByMaxItems() {
        CloudBus bus = bean(CloudBus.class)

        String secondNodeUuid = Platform.uuid
        DeadMessageManagerImpl mgr = bean(DeadMessageManagerImpl.class)
        mgr.previousOfflinedManagementNodes.put(secondNodeUuid, new ManagementNodeInventory(uuid: secondNodeUuid))

        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_TIMEOUT.updateValue(180)
        CloudBusGlobalConfig.MAX_MANAGEMENTNODE_NOTFOUND_ERROR_HANDLER_NUM.updateValue(5)

        for (int i=0; i<10; i++) {
            StartVmInstanceReply reply = new StartVmInstanceReply()
            bus.makeServiceIdByManagementNodeId(reply, "fake service", secondNodeUuid)
            bus.send(reply)
        }

        retryInSecs {
            assert mgr.managementNodeNotFoundHandlers.size() == 5
        }
    }

    @Override
    void test() {
        testHanlderWorks()
        testHanlderWorksNotWorkIfManagementNodeNotJoinedBefore()
        testHanlderWorksNotWorkIfMessageIsNotAReply()
        testHandlerDropsByTimeout()
        testHandlerDropsByMaxItems()
    }
}
