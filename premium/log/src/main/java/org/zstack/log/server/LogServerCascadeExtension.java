package org.zstack.log.server;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.log.LogConfigurationConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LogServerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(LogServerCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE) ||
                action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleCleanDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleCleanDeletion(CascadeAction action, Completion completion) {
        List<LogServerVO> logServers = new ArrayList<>();
        List<String> accountUuids = accountFromAction(action);
        if (CollectionUtils.isNotEmpty(accountUuids)) {
            logServers = SQL.New("select vo from LogServerVO vo, AccountResourceRefVO ref " +
                            "where vo.uuid = ref.resourceUuid and ref.accountUuid in (:accountUuids) " +
                            "and ref.type = :refType", LogServerVO.class)
                    .param("accountUuids", accountUuids)
                    .param("refType", AccessLevel.Own).list();
        } else if (LogServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<LogServerInventory> inventories = action.getParentIssuerContext();
            logServers = Q.New(LogServerVO.class).in(LogServerVO_.uuid,
                    inventories.stream().map(LogServerInventory::getUuid).collect(Collectors.toList())).list();
        }

        if (logServers.isEmpty()) {
            completion.success();
            return;
        }
        List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();
        boolean forceDelete = action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE);
        new While<>(logServers).each((server, whileCompletion) -> {
            deleteLogServer(server, mnUuids, forceDelete, new Completion(whileCompletion) {
                @Override
                public void success() {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    private void deleteLogServer(LogServerVO server, List<String> mnUuids, boolean forceDelete, Completion completion) {
        List<DeleteLogServerConfigurationMsg> msgs = mnUuids.stream().map(mnUuid -> {
            DeleteLogServerConfigurationMsg msg = new DeleteLogServerConfigurationMsg();
            msg.setUuid(server.getUuid());
            bus.makeServiceIdByManagementNodeId(msg, LogConfigurationConstant.SERVICE_ID, mnUuid);
            return msg;
        }).collect(Collectors.toList());

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    if (!reply.isSuccess()) {
                        if (!forceDelete) {
                            completion.fail(reply.getError());
                            return;
                        }
                        logger.warn(String.format("failed to delete configuration for log server[uuid:%s]: %s",
                                server.getUuid(), reply.getError()));
                    }
                }
                dbf.remove(server);
                completion.success();
            }
        });
    }

    private List<String> accountFromAction(CascadeAction action) {
        List<String> accountUuids = new ArrayList<>();
        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());
        }
        return accountUuids;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return LogServerVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
