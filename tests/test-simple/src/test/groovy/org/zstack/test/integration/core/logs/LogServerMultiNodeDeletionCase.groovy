package org.zstack.test.integration.core.logs

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.zstack.core.cascade.CascadeAction
import org.zstack.core.cascade.CascadeConstant
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountInventory
import org.zstack.header.identity.AccountVO
import org.zstack.log.server.LogServerCascadeExtension
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Message
import org.zstack.log.LogConfigurationConstant
import org.zstack.log.LogConfigurationManagerImpl
import org.zstack.log.server.ApplyLogServerConfigurationMsg
import org.zstack.log.server.DeleteLogServerConfigurationMsg
import org.zstack.log.server.DeleteLogServerConfigurationReply
import org.zstack.log.server.LogServerVO
import org.zstack.sdk.logserver.LogServerInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.ConcurrentHashMap

class LogServerMultiNodeDeletionCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    CloudBus bus
    String remoteUuid = Platform.uuid
    Set<String> remoteConfigurations = ConcurrentHashMap.newKeySet()
    Set<String> deletionTargets = ConcurrentHashMap.newKeySet()
    volatile boolean failRemoteDeletion
    volatile boolean interceptMessages

    @Override
    void setup() {
        def spec = makeSpring()
        spec.include("LogConfiguration.xml")
        spec.include("Log4j2Configuration.xml")
        useSpring(spec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)
        env.create {
            interceptMessages = true
            bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
                @Override
                void beforeSendMessage(Message msg) {
                    if (interceptMessages) {
                        msg.putHeaderEntry("log-server-test-target", msg.serviceId)
                    }
                }
            }, ApplyLogServerConfigurationMsg.class, DeleteLogServerConfigurationMsg.class)

            env.message(DeleteLogServerConfigurationMsg.class) { DeleteLogServerConfigurationMsg msg ->
                String target = msg.getHeaderEntry("log-server-test-target")
                deletionTargets.add(target)
                LogServerVO vo = dbf.findByUuid(msg.uuid, LogServerVO.class)
                assert vo != null : "log server must remain available until all nodes finish deleting configuration"
                if (target == remoteServiceId()) {
                    if (failRemoteDeletion) {
                        bus.replyErrorByMessageType(msg, "remote configuration deletion failed")
                    } else {
                        remoteConfigurations.remove(msg.uuid)
                        bus.reply(msg, new DeleteLogServerConfigurationReply())
                    }
                } else {
                    bean(LogConfigurationManagerImpl.class).handleMessage(msg)
                }
            }

            testDeleteOnEveryNode()
            testDeleteOnEveryNode(ManagementNodeState.JOINING)
            testMissingFactoryReturnsExplicitError()
            testFailedDeletionCanBeRetried()
            testCreationRollbackKeepsResourceUntilCleanup()
            testAccountCascadePreservesSharedLogServer()
            testAccountCascadeDeletesOnEveryNode()
            testForceDeletionContinuesAfterRemoteFailure()
        }
    }

    private String remoteServiceId() {
        DeleteLogServerConfigurationMsg msg = new DeleteLogServerConfigurationMsg()
        bus.makeServiceIdByManagementNodeId(msg, LogConfigurationConstant.SERVICE_ID, remoteUuid)
        return msg.serviceId
    }

    private void addRemoteNode(ManagementNodeState state = ManagementNodeState.RUNNING) {
        dbf.persist(new ManagementNodeVO(uuid: remoteUuid, hostName: "127.0.0.10", port: 8989,
                state: state,
                heartBeat: new java.sql.Timestamp(System.currentTimeMillis() + 3600000)))
    }

    private LogServerInventory addServer() {
        return addLogServer {
            name = "multi-node-log"
            category = "ManagementNodeLog"
            type = "Log4j2"
            level = "ALL"
            configuration = JSONObjectUtil.toJsonString([type: "Syslog", configuration:
                    JSONObjectUtil.toJsonString([facility: "LOCAL5", protocol: "UDP",
                            hostname: "127.0.0.1", port: 514, immediateFlush: "true"])])
        } as LogServerInventory
    }

    private boolean hasLocalAppender(String uuid) {
        return ((LoggerContext) LogManager.getContext(false)).configuration.rootLogger.appenders.containsKey(uuid)
    }

    private void assertDeletedEverywhere(String uuid) {
        assert deletionTargets == [bus.makeLocalServiceId(LogConfigurationConstant.SERVICE_ID), remoteServiceId()] as Set
        assert !remoteConfigurations.contains(uuid)
        assert !hasLocalAppender(uuid)
        assert dbf.findByUuid(uuid, LogServerVO.class) == null
    }

    void testDeleteOnEveryNode(ManagementNodeState state = ManagementNodeState.RUNNING) {
        LogServerInventory server = addServer()
        addRemoteNode(state)
        remoteConfigurations.add(server.uuid)
        deletionTargets.clear()
        try {
            deleteLogServer { uuid = server.uuid }
            assertDeletedEverywhere(server.uuid)
        } finally {
            dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
        }
    }

    void testFailedDeletionCanBeRetried() {
        LogServerInventory server = addServer()
        addRemoteNode()
        remoteConfigurations.add(server.uuid)
        failRemoteDeletion = true
        try {
            expectError {
                deleteLogServer { uuid = server.uuid }
            }
            assert dbf.findByUuid(server.uuid, LogServerVO.class) != null
            assert remoteConfigurations.contains(server.uuid)

            failRemoteDeletion = false
            deletionTargets.clear()
            deleteLogServer { uuid = server.uuid }
            assertDeletedEverywhere(server.uuid)
        } finally {
            failRemoteDeletion = false
            dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
        }
    }

    void testMissingFactoryReturnsExplicitError() {
        LogServerInventory server = addServer()
        def factories = bean(LogConfigurationManagerImpl.class).logConfigurationFactories
        def factory = factories.remove('log4j2')
        try {
            DeleteLogServerConfigurationMsg msg = new DeleteLogServerConfigurationMsg(uuid: server.uuid)
            bus.makeLocalServiceId(msg, LogConfigurationConstant.SERVICE_ID)
            def reply = bus.call(msg)
            assert !reply.success
            assert reply.error.code == 'SYS.1006'
            assert reply.error.details == 'No factory found for type:log4j2'
            assert hasLocalAppender(server.uuid)
            assert dbf.findByUuid(server.uuid, LogServerVO.class) != null
        } finally {
            factories.put('log4j2', factory)
            deleteLogServer { uuid = server.uuid }
        }
    }

    void testCreationRollbackKeepsResourceUntilCleanup() {
        addRemoteNode()
        Set<String> createdUuids = ConcurrentHashMap.newKeySet()
        Set<String> creationTargets = ConcurrentHashMap.newKeySet()
        deletionTargets.clear()
        env.message(ApplyLogServerConfigurationMsg.class) { ApplyLogServerConfigurationMsg msg ->
            createdUuids.add(msg.struct.uuid)
            creationTargets.add(msg.getHeaderEntry("log-server-test-target"))
            if (msg.getHeaderEntry("log-server-test-target") == remoteServiceId()) {
                remoteConfigurations.add(msg.struct.uuid)
                bus.replyErrorByMessageType(msg, "remote configuration creation failed")
            } else {
                bean(LogConfigurationManagerImpl.class).handleMessage(msg)
            }
        }
        try {
            expectError { addServer() }
            assert createdUuids.size() == 1
            String uuid = createdUuids.first()
            assert deletionTargets == creationTargets
            assert !remoteConfigurations.contains(uuid)
            assert !hasLocalAppender(uuid)
            assert dbf.findByUuid(uuid, LogServerVO.class) == null
        } finally {
            env.revokeMessage(ApplyLogServerConfigurationMsg.class, null)
            dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
        }
    }

    private void runCascade(CascadeAction action) {
        CountDownLatch done = new CountDownLatch(1)
        AtomicReference<ErrorCode> failure = new AtomicReference<>()
        bean(LogServerCascadeExtension.class).asyncCascade(action, new Completion(null) {
            @Override
            void success() { done.countDown() }

            @Override
            void fail(ErrorCode errorCode) {
                failure.set(errorCode)
                done.countDown()
            }
        })
        assert done.await(30, TimeUnit.SECONDS)
        assert failure.get() == null
    }

    void testAccountCascadePreservesSharedLogServer() {
        LogServerInventory server = addServer()
        def recipient = createAccount {
            name = "log-server-share-recipient"
            password = "password"
        }
        AccountResourceRefVO shareRef
        addRemoteNode()
        remoteConfigurations.add(server.uuid)
        deletionTargets.clear()
        try {
            LogServerVO vo = dbf.findByUuid(server.uuid, LogServerVO.class)
            assert vo.accountUuid != null
            assert vo.accountUuid != recipient.uuid
            shareRef = dbf.persist(new AccountResourceRefVO(accountUuid: recipient.uuid,
                    resourceUuid: server.uuid, resourceType: LogServerVO.simpleName,
                    type: AccessLevel.Share))

            runCascade(new CascadeAction(actionCode: CascadeConstant.DELETION_DELETE_CODE,
                    parentIssuer: AccountVO.simpleName,
                    parentIssuerContext: [new AccountInventory(uuid: recipient.uuid)]))

            assert deletionTargets.isEmpty()
            assert dbf.findByUuid(server.uuid, LogServerVO.class) != null
            assert hasLocalAppender(server.uuid)
            assert remoteConfigurations.contains(server.uuid)
        } finally {
            if (shareRef != null) {
                dbf.removeByPrimaryKey(shareRef.id, AccountResourceRefVO.class)
            }
            try {
                if (dbf.findByUuid(server.uuid, LogServerVO.class) != null) {
                    deleteLogServer { uuid = server.uuid }
                }
            } finally {
                remoteConfigurations.remove(server.uuid)
                dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
                deleteAccount { uuid = recipient.uuid }
            }
        }
    }

    void testAccountCascadeDeletesOnEveryNode() {
        LogServerInventory server = addServer()
        LogServerVO vo = dbf.findByUuid(server.uuid, LogServerVO.class)
        addRemoteNode()
        remoteConfigurations.add(server.uuid)
        deletionTargets.clear()
        try {
            runCascade(new CascadeAction(actionCode: CascadeConstant.DELETION_DELETE_CODE,
                    parentIssuer: AccountVO.simpleName,
                    parentIssuerContext: [new AccountInventory(uuid: vo.accountUuid)]))
            assertDeletedEverywhere(server.uuid)
        } finally {
            dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
        }
    }

    void testForceDeletionContinuesAfterRemoteFailure() {
        LogServerInventory server = addServer()
        LogServerVO vo = dbf.findByUuid(server.uuid, LogServerVO.class)
        addRemoteNode()
        remoteConfigurations.add(server.uuid)
        deletionTargets.clear()
        failRemoteDeletion = true
        try {
            runCascade(new CascadeAction(actionCode: CascadeConstant.DELETION_FORCE_DELETE_CODE,
                    parentIssuer: LogServerVO.simpleName,
                    parentIssuerContext: [org.zstack.log.server.LogServerInventory.valueOf(vo)]))
            assert deletionTargets == [bus.makeLocalServiceId(LogConfigurationConstant.SERVICE_ID), remoteServiceId()] as Set
            assert dbf.findByUuid(server.uuid, LogServerVO.class) == null
            assert !hasLocalAppender(server.uuid)
            assert remoteConfigurations.contains(server.uuid)
        } finally {
            failRemoteDeletion = false
            remoteConfigurations.remove(server.uuid)
            dbf.removeByPrimaryKey(remoteUuid, ManagementNodeVO.class)
        }
    }

    @Override
    void clean() {
        interceptMessages = false
        env.delete()
    }
}
