package org.zstack.iam1.compute.accounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.AfterCreateAccountExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.iam1.entity.accounts.AccountGroupVO;
import org.zstack.iam1.message.accounts.AddAccountToGroupMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.iam1.header.accounts.AccountGroupSystemTags.GROUP_FOR_NEW_CREATE_ACCOUNT;
import static org.zstack.utils.CollectionDSL.list;

public class AccountGroupExtensions implements AfterCreateAccountExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AccountGroupExtensions.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void afterCreateAccount(AccountInventory account) {
        if (isNormalOrThirdPartyAccount(account)) {
            addAccountToInitGroup(account.getUuid(), new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("Add account[uuid:%s] to init groups", account.getUuid()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to add account[uuid:%s] to init groups, error:%s",
                            account.getUuid(), errorCode.getDetails()));
                }
            });
        }
    }

    private boolean isNormalOrThirdPartyAccount(AccountInventory account) {
        return account.getType().equals(AccountType.Normal.toString())
                || account.getType().equals(AccountType.ThirdParty.toString());
    }

    private void addAccountToInitGroup(String accountUuid, Completion completion) {
        List<String> groups = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.tag, GROUP_FOR_NEW_CREATE_ACCOUNT.getTagFormat())
                .eq(SystemTagVO_.resourceType, AccountGroupVO.class.getSimpleName())
                .select(SystemTagVO_.resourceUuid)
                .listValues();
        if (groups.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(groups).each((groupUuid, whileCompletion) -> {
            AddAccountToGroupMsg innerMsg = new AddAccountToGroupMsg();
            innerMsg.setAccountUuids(list(accountUuid));
            innerMsg.setGroupUuid(groupUuid);
            bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, groupUuid);
            bus.send(innerMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        whileCompletion.done();
                    } else {
                        whileCompletion.addError(reply.getError());
                        whileCompletion.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });
    }
}
