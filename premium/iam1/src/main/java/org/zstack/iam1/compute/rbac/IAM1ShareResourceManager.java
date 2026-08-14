package org.zstack.iam1.compute.rbac;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.Message;
import org.zstack.iam1.compute.accounts.AccountGroupBase;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO_;
import org.zstack.iam1.entity.accounts.AccountGroupResourceRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupResourceRefVO_;
import org.zstack.iam1.header.accounts.GroupNode;
import org.zstack.iam1.message.rbac.RevokeResourceSharingMsg;
import org.zstack.iam1.message.rbac.RevokeResourceSharingReply;
import org.zstack.iam1.message.rbac.ShareResourceMsg;
import org.zstack.iam1.message.rbac.ShareResourceReply;
import org.zstack.identity.header.ShareResourceContext;
import org.zstack.identity.rbac.ShareResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformToSet;

/**
 * Created by Wenhao.Zhang on 2024/09/05
 */
public class IAM1ShareResourceManager extends ShareResourceHelper implements Service {
    private static final CLogger logger = Utils.getLogger(IAM1ShareResourceManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threadFacade;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ShareResourceMsg) {
            handle((ShareResourceMsg) msg);
        } else if (msg instanceof RevokeResourceSharingMsg) {
            handle((RevokeResourceSharingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(IAM1RBACConstant.SERVICE_ID);
    }

    @Override
    public int getSyncLevel() {
        return 0;
    }

    @Override
    public List<String> getAliasIds() {
        return null;
    }

    @SyncThread(signature="for-resource-sharing")
    private void handle(ShareResourceMsg message) {
        ShareResourceContext context = ShareResourceContext.fromResources(message.getResourceUuids());

        if (message.isToPublic()) {
            shareToPublic(context);
            bus.reply(message, new ShareResourceReply());
            return;
        }

        if (message.getGroupUuid() == null) {
            shareToAccounts(context, message.getAccountUuids());
            bus.reply(message, new ShareResourceReply());
            return;
        }

        sinkAccountGroupChains(() -> shareToAccountGroup(context, message.getGroupUuid(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, new ShareResourceReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ShareResourceReply reply = new ShareResourceReply();
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        }));
    }

    @SyncThread(signature="for-resource-sharing")
    private void handle(RevokeResourceSharingMsg message) {
        ShareResourceContext context = ShareResourceContext.fromResources(message.getResourceUuids());

        if (message.isAll()) {
            revokeSharingAll(context);
            bus.reply(message, new RevokeResourceSharingReply());
            return;
        }

        if (message.isToPublic()) {
            revokeSharingToPublic(context);
            bus.reply(message, new RevokeResourceSharingReply());
            return;
        }

        if (message.getGroupUuid() == null) {
            revokeSharingToAccounts(context, message.getAccountUuids());
            bus.reply(message, new RevokeResourceSharingReply());
            return;
        }

        sinkAccountGroupChains(() -> revokeSharingToAccountGroup(context, message.getGroupUuid(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, new RevokeResourceSharingReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RevokeResourceSharingReply reply = new RevokeResourceSharingReply();
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        }));
    }

    private void sinkAccountGroupChains(Runnable handler) {
        threadFacade.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                handler.run();
                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getSyncSignature() {
                return "account-group-organizational-form-update";
            }
        });
    }

    /**
     * The resources shared to account group must in resource ensemble.
     */
    public void shareToAccountGroup(ShareResourceContext context, String groupUuid, Completion completion) {
        emitBeforeSharingExtensions(context);

        List<String> relatedAccountUuids = findRelatedAccountUuids(groupUuid);
        final Set<String> allMasterResources = context.findAllMasterResources();

        List<AccountResourceRefVO> accountRefNeedPersists = new ArrayList<>();
        List<AccountGroupResourceRefVO> groupRefNeedPersists = new ArrayList<>();

        Set<String> excludeResources = new HashSet<>(Q.New(AccountGroupResourceRefVO.class)
                .eq(AccountGroupResourceRefVO_.groupUuid, groupUuid)
                .in(AccountGroupResourceRefVO_.resourceUuid, allMasterResources)
                .select(AccountGroupResourceRefVO_.resourceUuid)
                .listValues());

        for (String masterResource : allMasterResources) {
            if (excludeResources.contains(masterResource)) {
                continue;
            }

            AccountGroupResourceRefVO groupRef = new AccountGroupResourceRefVO();
            groupRef.setGroupUuid(groupUuid);
            groupRef.setResourceUuid(masterResource);
            groupRefNeedPersists.add(groupRef);

            if (relatedAccountUuids.isEmpty()) {
                continue;
            }

            List<AccountResourceRefVO> refs = context.buildShareAccountRecords(masterResource, relatedAccountUuids);
            refs.forEach(ref -> ref.setAccountPermissionFrom(groupUuid));

            List<Tuple> existsTuples = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                    .in(AccountResourceRefVO_.resourceUuid, transform(refs, AccountResourceRefVO::getResourceUuid))
                    .in(AccountResourceRefVO_.accountUuid, relatedAccountUuids)
                    .eq(AccountResourceRefVO_.resourcePermissionFrom, masterResource)
                    .eq(AccountResourceRefVO_.accountPermissionFrom, groupUuid)
                    .select(
                            AccountResourceRefVO_.resourceUuid,
                            AccountResourceRefVO_.accountUuid,
                            AccountResourceRefVO_.resourcePermissionFrom
                    )
                    .listTuple();
            Set<String> existsRecords = transformToSet(existsTuples,
                    tuple -> tuple.get(0, String.class) + "," + tuple.get(1, String.class) + "," + tuple.get(2, String.class));
            refs.removeIf(ref -> existsRecords.contains(String.format("%s,%s,%s",
                    ref.getResourceUuid(), ref.getAccountUuid(), ref.getResourcePermissionFrom())));
            accountRefNeedPersists.addAll(refs);
        }

        if (accountRefNeedPersists.isEmpty() && groupRefNeedPersists.isEmpty()) {
            completion.success();
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (AccountResourceRefVO ref : accountRefNeedPersists) {
                    persist(ref);
                }
                for (AccountGroupResourceRefVO ref : groupRefNeedPersists) {
                    persist(ref);
                }
            }
        }.execute();

        String texts = StringUtils.join(transform(accountRefNeedPersists,
                shared -> String.format("\tuuid:%s type:%s", shared.getResourceUuid(), shared.getResourceType())), "\n");
        logger.debug(String.format("Shared below resources to accountGroup[uuid:%s]: %n%s", groupUuid, texts));
        completion.success();
    }

    public void revokeSharingToAccountGroup(ShareResourceContext context, String groupUuid, Completion completion) {
        emitBeforeSharingExtensions(context);

        final Set<String> masterResourceUuidSet = context.findAllMasterResources();
        if (masterResourceUuidSet.isEmpty()) {
            completion.success();
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AccountResourceRefVO.class)
                        .in(AccountResourceRefVO_.resourcePermissionFrom, masterResourceUuidSet)
                        .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                        .eq(AccountResourceRefVO_.accountPermissionFrom, groupUuid)
                        .delete();
                sql(AccountGroupResourceRefVO.class)
                        .in(AccountGroupResourceRefVO_.resourceUuid, masterResourceUuidSet)
                        .eq(AccountGroupResourceRefVO_.groupUuid, groupUuid)
                        .delete();
            }
        }.execute();

        logger.debug(String.format("Revoke shared resource for type(Share to AccountGroup[uuid:%s]): %n%s",
                groupUuid,
                StringUtils.join(transform(masterResourceUuidSet, uuid -> String.format("\tuuid:%s", uuid)), "\n")));
        completion.success();
    }

    private List<String> findRelatedAccountUuids(String groupUuid) {
        GroupNode groupNode = AccountGroupBase.buildGroupTreeOrThrow(groupUuid);
        Set<String> relatedGroupUuids = groupNode.selfAndAllChildrenUuidSet();
        return Q.New(AccountGroupAccountRefVO.class)
                .select(AccountGroupAccountRefVO_.accountUuid)
                .in(AccountGroupAccountRefVO_.groupUuid, relatedGroupUuids)
                .listValues();
    }
}
