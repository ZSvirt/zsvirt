package org.zstack.iam1.compute.accounts;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.identity.role.RoleAccountRefVO;
import org.zstack.header.identity.role.RoleAccountRefVO_;
import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.header.vo.ResourceTypeMetadata;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.api.accounts.*;
import org.zstack.iam1.compute.ensemble.ResourceEnsembleHelper;
import org.zstack.iam1.compute.rbac.IAM1RBACConstant;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO_;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;
import org.zstack.iam1.entity.accounts.AccountGroupResourceRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupResourceRefVO_;
import org.zstack.iam1.entity.accounts.AccountGroupResourceView;
import org.zstack.iam1.entity.accounts.AccountGroupRoleRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupRoleRefVO_;
import org.zstack.iam1.entity.accounts.AccountGroupRoleView;
import org.zstack.iam1.entity.accounts.AccountGroupVO;
import org.zstack.iam1.entity.accounts.AccountGroupVO_;
import org.zstack.iam1.entity.accounts.AccountGroupView;
import org.zstack.iam1.header.IAM1Errors;
import org.zstack.iam1.header.accounts.GroupNode;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;
import org.zstack.iam1.message.accounts.*;
import org.zstack.iam1.message.rbac.RevokeResourceSharingMsg;
import org.zstack.iam1.message.rbac.ShareResourceMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.iam1.compute.accounts.AccountGroupConstant.GROUP_FORM_UPDATE_TARGET;
import static org.zstack.iam1.header.accounts.GroupNode.*;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountGroupBase {
    private static final CLogger logger = Utils.getLogger(AccountGroupBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private ThreadFacade threadFacade;

    private AccountGroupVO self;

    public AccountGroupBase(AccountGroupVO self) {
        this.self = self;
    }

    protected AccountGroupVO refreshVO() {
        AccountGroupVO vo = self;
        refreshVOOrNull();
        if (self == null) {
            throw new OperationFailureException(operr(
                    "AccountGroup[uuid:%s, name:%s] has been deleted", vo.getUuid(), vo.getName()));
        }
        return self;
    }

    protected AccountGroupVO refreshVOOrNull() {
        return self = databaseFacade.findByUuid(self.getUuid(), AccountGroupVO.class);
    }

    public void handleMessage(Message message) {
        if (message instanceof APIMessage) {
            handleApiMessage((APIMessage) message);
        } else {
            handleLocalMessage(message);
        }
    }

    private void handleApiMessage(APIMessage message) {
        if (message instanceof APIGetAccountGroupTreeMsg) {
            handle((APIGetAccountGroupTreeMsg) message);
        } else if (message instanceof APIGetRolesForAccountGroupMsg) {
            handle((APIGetRolesForAccountGroupMsg) message);
        } else if (message instanceof APIGetResourceInAccountGroupMsg) {
            handle((APIGetResourceInAccountGroupMsg) message);
        } else if (message instanceof APIShareResourceToGroupMsg) {
            handle((APIShareResourceToGroupMsg) message);
        } else if (message instanceof APIRevokeResourceSharingToGroupMsg) {
            handle((APIRevokeResourceSharingToGroupMsg) message);
        } else if (message instanceof APIAddAccountToGroupMsg) {
            handle((APIAddAccountToGroupMsg) message);
        } else if (message instanceof APIAttachRoleToAccountGroupMsg) {
            handle((APIAttachRoleToAccountGroupMsg) message);
        } else if (message instanceof APIRemoveAccountFromGroupMsg) {
            handle((APIRemoveAccountFromGroupMsg) message);
        } else if (message instanceof APIDetachRoleFromAccountGroupMsg) {
            handle((APIDetachRoleFromAccountGroupMsg) message);
        } else if (message instanceof APIUpdateAccountGroupMsg) {
            handle((APIUpdateAccountGroupMsg) message);
        } else if (message instanceof APIMoveAccountGroupMsg) {
            handle((APIMoveAccountGroupMsg) message);
        } else if (message instanceof APIDeleteAccountGroupMsg) {
            handle((APIDeleteAccountGroupMsg) message);
        } else {
            bus.dealWithUnknownMessage(message);
        }
    }

    private void handleLocalMessage(Message message) {
        if (message instanceof AddAccountToGroupMsg) {
            handle((AddAccountToGroupMsg) message);
        } else if (message instanceof AttachRoleToAccountGroupMsg) {
            handle((AttachRoleToAccountGroupMsg) message);
        } else if (message instanceof RemoveAccountFromGroupMsg) {
            handle((RemoveAccountFromGroupMsg) message);
        } else if (message instanceof DetachRoleFromAccountGroupMsg) {
            handle((DetachRoleFromAccountGroupMsg) message);
        } else if (message instanceof MoveAccountGroupMsg) {
            handle((MoveAccountGroupMsg) message);
        } else if (message instanceof DeleteAccountGroupMsg) {
            handle((DeleteAccountGroupMsg) message);
        } else {
            bus.dealWithUnknownMessage(message);
        }
    }

    private void handle(APIGetAccountGroupTreeMsg message) {
        APIGetAccountGroupTreeReply reply = new APIGetAccountGroupTreeReply();
        reply.setInventory(buildAccountGroupView(message));
        bus.reply(message, reply);
    }

    private void handle(APIGetRolesForAccountGroupMsg message) {
        APIGetRolesForAccountGroupReply reply = new APIGetRolesForAccountGroupReply();
        reply.setCurrentGroup(buildAccountGroupRoleView());
        if (message.isIncludeInheritedRoles()) {
            reply.setParentGroups(buildInheritedAccountGroupRoleView());
        }
        bus.reply(message, reply);
    }

    private void handle(APIGetResourceInAccountGroupMsg message) {
        APIGetResourceInAccountGroupReply reply = new APIGetResourceInAccountGroupReply();
        reply.setCurrentGroup(buildAccountGroupResourceView());
        if (message.isIncludeInheritedResources()) {
            reply.setParentGroups(buildInheritedAccountGroupResourceView());
        }
        bus.reply(message, reply);
    }

    private void handle(APIShareResourceToGroupMsg message) {
        ShareResourceMsg innerMsg = ShareResourceMsg.valueOf(message);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, IAM1RBACConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIShareResourceToGroupEvent event = new APIShareResourceToGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIRevokeResourceSharingToGroupMsg message) {
        RevokeResourceSharingMsg innerMsg = RevokeResourceSharingMsg.valueOf(message);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, IAM1RBACConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIRevokeResourceSharingToGroupEvent event = new APIRevokeResourceSharingToGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIAddAccountToGroupMsg message) {
        AddAccountToGroupMsg innerMsg = new AddAccountToGroupMsg();
        innerMsg.setAccountUuids(message.getAccountUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIAddAccountToGroupEvent event = new APIAddAccountToGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIAttachRoleToAccountGroupMsg message) {
        AttachRoleToAccountGroupMsg innerMsg = new AttachRoleToAccountGroupMsg();
        innerMsg.setRoleUuids(message.getRoleUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIAttachRoleToAccountGroupEvent event = new APIAttachRoleToAccountGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveAccountFromGroupMsg message) {
        RemoveAccountFromGroupMsg innerMsg = new RemoveAccountFromGroupMsg();
        innerMsg.setAccountUuids(message.getAccountUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIRemoveAccountFromGroupEvent event = new APIRemoveAccountFromGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIDetachRoleFromAccountGroupMsg message) {
        DetachRoleFromAccountGroupMsg innerMsg = new DetachRoleFromAccountGroupMsg();
        innerMsg.setRoleUuids(message.getRoleUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIDetachRoleFromAccountGroupEvent event = new APIDetachRoleFromAccountGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdateAccountGroupMsg message) {
        final UpdateQuery sql = SQL.New(AccountGroupVO.class)
                .eq(AccountGroupVO_.uuid, message.getUuid());

        if (message.getName() != null) {
            sql.set(AccountGroupVO_.name, message.getName())
                    .set(AccountGroupVO_.resourceName, message.getName());
        }
        if (message.getDescription() != null) {
            sql.set(AccountGroupVO_.description, message.getDescription());
        }

        sql.update();
        APIUpdateAccountGroupEvent event = new APIUpdateAccountGroupEvent(message.getId());
        event.setInventory(AccountGroupInventory.valueOf(refreshVO()));
        bus.publish(event);
    }

    private void handle(APIMoveAccountGroupMsg message) {
        MoveAccountGroupMsg innerMsg = new MoveAccountGroupMsg();
        innerMsg.setUuid(message.getUuid());
        innerMsg.setParentUuid(message.getParentUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIMoveAccountGroupEvent event = new APIMoveAccountGroupEvent(message.getId());
                if (reply.isSuccess()) {
                    AccountGroupVO currentGroup = databaseFacade.findByUuid(message.getUuid(), AccountGroupVO.class);
                    event.setInventory(AccountGroupInventory.valueOf(currentGroup));
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIDeleteAccountGroupMsg message) {
        DeleteAccountGroupMsg innerMsg = new DeleteAccountGroupMsg();
        innerMsg.setUuid(message.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountGroupConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIDeleteAccountGroupEvent event = new APIDeleteAccountGroupEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(AddAccountToGroupMsg message) {
        AddAccountToGroupReply reply = new AddAccountToGroupReply();

        addAccounts(message.getAccountUuids(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    private void handle(AttachRoleToAccountGroupMsg message) {
        AttachRoleToAccountGroupReply reply = new AttachRoleToAccountGroupReply();

        addRoles(message.getRoleUuids(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    private void handle(RemoveAccountFromGroupMsg message) {
        RemoveAccountFromGroupReply reply = new RemoveAccountFromGroupReply();

        removeAccounts(message.getAccountUuids(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    private void handle(DetachRoleFromAccountGroupMsg message) {
        DetachRoleFromAccountGroupReply reply = new DetachRoleFromAccountGroupReply();

        removeRoles(message.getRoleUuids(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    private void handle(MoveAccountGroupMsg message) {
        MoveAccountGroupReply reply = new MoveAccountGroupReply();

        move(message.getParentUuid(), new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    private void handle(DeleteAccountGroupMsg message) {
        DeleteAccountGroupReply reply = new DeleteAccountGroupReply();

        delete(new Completion(message) {
            @Override
            public void success() {
                bus.reply(message, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(message, reply);
            }
        });
    }

    public AccountGroupView buildAccountGroupView(APIGetAccountGroupTreeMsg message) {
        GroupNode current = buildGroupTreeOrThrow();

        current.retainWithLevel(message.getLevel());
        final AccountGroupView view = AccountGroupView.valueOf(current);
        final List<AccountGroupView> views = view.flatten();

        final Set<String> groupUuids = current.selfAndAllChildrenUuidSet();
        if (message.isShowGroup()) {
            final List<AccountGroupVO> groups = Q.New(AccountGroupVO.class)
                    .in(AccountGroupVO_.uuid, groupUuids)
                    .list();
            Map<String, AccountGroupVO> uuidGroupMap = toMap(groups, AccountGroupVO::getUuid, Function.identity());

            for (AccountGroupView accountGroupView : views) {
                accountGroupView.setInventory(uuidGroupMap.get(accountGroupView.getGroupUuid()));
            }
        }

        if (message.isShowAccount()) {
            List<Tuple> accountGroupTuples = Q.New(AccountGroupAccountRefVO.class)
                    .in(AccountGroupAccountRefVO_.groupUuid, groupUuids)
                    .select(AccountGroupAccountRefVO_.accountUuid, AccountGroupAccountRefVO_.groupUuid)
                    .listTuple();
            final Set<String> accountUuidSet = transformToSet(accountGroupTuples, tuple -> tuple.get(0, String.class));

            Map<String, AccountVO> uuidAccountMap;
            if (accountUuidSet.isEmpty()) {
                uuidAccountMap = Collections.emptyMap();
            } else {
                List<AccountVO> accounts = Q.New(AccountVO.class)
                        .in(AccountVO_.uuid, accountUuidSet)
                        .list();
                uuidAccountMap = toMap(accounts, AccountVO::getUuid, Function.identity());
            }

            for (AccountGroupView accountGroupView : views) {
                final List<String> accountUuids = accountGroupTuples.stream()
                        .filter(tuple -> Objects.equals(tuple.get(1, String.class), accountGroupView.getGroupUuid()))
                        .map(tuple -> tuple.get(0, String.class))
                        .collect(Collectors.toList());
                accountGroupView.setAccounts(transform(accountUuids,
                        uuid -> AccountInventory.valueOf(uuidAccountMap.get(uuid))));
            }
        }

        return view;
    }

    public AccountGroupRoleView buildAccountGroupRoleView() {
        List<RoleVO> roleList = Q.New(AccountGroupRoleRefVO.class, RoleVO.class)
                .table0()
                    .eq(AccountGroupRoleRefVO_.groupUuid, self.getUuid())
                    .eq(AccountGroupRoleRefVO_.roleUuid).table1(RoleVO_.uuid)
                .table1()
                    .selectThisTable()
                .list();

        AccountGroupRoleView view = new AccountGroupRoleView();
        view.setGroupUuid(self.getUuid());
        view.setGroupName(self.getName());
        view.getRoles().addAll(RoleInventory.valueOf(roleList));
        return view;
    }

    public List<AccountGroupRoleView> buildInheritedAccountGroupRoleView() {
        GroupNode current = buildGroupTreeOrThrow();

        Set<String> parentsGroupUuidSet = current.ancestorUuidSet();
        if (parentsGroupUuidSet.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> tuples = Q.New(AccountGroupRoleRefVO.class, RoleVO.class, AccountGroupVO.class)
                .table0()
                    .in(AccountGroupRoleRefVO_.groupUuid, parentsGroupUuidSet)
                    .eq(AccountGroupRoleRefVO_.roleUuid).table1(RoleVO_.uuid)
                    .eq(AccountGroupRoleRefVO_.groupUuid).table2(AccountGroupVO_.uuid)
                .table1()
                    .selectThisTable()
                .table2()
                    .select(AccountGroupVO_.uuid, AccountGroupVO_.name)
                .listTuple();
        Map<String, List<Tuple>> uuidTupleMap = groupBy(tuples, tuple -> tuple.get(1, String.class));
        List<AccountGroupRoleView> views = new ArrayList<>(uuidTupleMap.size());

        for (Map.Entry<String, List<Tuple>> entry : uuidTupleMap.entrySet()) {
            AccountGroupRoleView view = new AccountGroupRoleView();
            view.setGroupUuid(entry.getKey());
            view.setGroupName(entry.getValue().get(0).get(2, String.class));
            view.getRoles().addAll(RoleInventory.valueOf(
                    transform(entry.getValue(), tuple -> tuple.get(0, RoleVO.class))));
            views.add(view);
        }

        return views;
    }

    public AccountGroupResourceView buildAccountGroupResourceView() {
        List<Tuple> tuples = Q.New(AccountGroupResourceRefVO.class, ResourceVO.class)
                .table0()
                    .eq(AccountGroupResourceRefVO_.groupUuid, self.getUuid())
                    .eq(AccountGroupResourceRefVO_.resourceUuid).table1(ResourceVO_.uuid)
                .table1()
                    .select(ResourceVO_.uuid, ResourceVO_.resourceName, ResourceVO_.resourceType)
                .listTuple();

        AccountGroupResourceView view = new AccountGroupResourceView();
        view.setGroupUuid(self.getUuid());
        view.setGroupName(self.getName());
        for (Tuple tuple : tuples) {
            final ResourceInventory resource = new ResourceInventory();
            resource.setUuid(tuple.get(0, String.class));
            resource.setResourceName(tuple.get(1, String.class));
            resource.setResourceType(tuple.get(2, String.class));
            view.getResources().add(resource);
        }

        return view;
    }

    public List<AccountGroupResourceView> buildInheritedAccountGroupResourceView() {
        GroupNode current = buildGroupTreeOrThrow();

        Set<String> parentsGroupUuidSet = current.ancestorUuidSet();
        if (parentsGroupUuidSet.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tuple> tuples = Q.New(AccountGroupResourceRefVO.class, ResourceVO.class, AccountGroupVO.class)
                .table0()
                    .in(AccountGroupResourceRefVO_.groupUuid, parentsGroupUuidSet)
                    .eq(AccountGroupResourceRefVO_.resourceUuid).table1(ResourceVO_.uuid)
                    .eq(AccountGroupResourceRefVO_.groupUuid).table2(AccountGroupVO_.uuid)
                .table1()
                    .select(ResourceVO_.uuid, ResourceVO_.resourceName, ResourceVO_.resourceType)
                .table2()
                    .select(AccountGroupVO_.uuid, AccountGroupVO_.name)
                .listTuple();
        Map<String, List<Tuple>> uuidTupleMap = groupBy(tuples, tuple -> tuple.get(3, String.class));
        List<AccountGroupResourceView> views = new ArrayList<>(uuidTupleMap.size());

        for (Map.Entry<String, List<Tuple>> entry : uuidTupleMap.entrySet()) {
            AccountGroupResourceView view = new AccountGroupResourceView();
            view.setGroupUuid(entry.getKey());
            view.setGroupName(entry.getValue().get(0).get(4, String.class));

            for (Tuple tuple : entry.getValue()) {
                final ResourceInventory resource = new ResourceInventory();
                resource.setUuid(tuple.get(0, String.class));
                resource.setResourceName(tuple.get(1, String.class));
                resource.setResourceType(tuple.get(2, String.class));
                view.getResources().add(resource);
            }
            views.add(view);
        }

        return views;
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void delete(Completion completion) {
        AccountGroupVO currentGroup = refreshVOOrNull();

        if (currentGroup == null) {
            completion.success();
            return;
        }

        GroupNode current = buildGroupTreeOrThrow();
        final Set<String> uuidSet = current.selfAndAllChildrenUuidSet();
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AccountGroupAccountRefVO.class)
                        .in(AccountGroupAccountRefVO_.groupUuid, uuidSet)
                        .delete();
                sql(AccountGroupRoleRefVO.class)
                        .in(AccountGroupRoleRefVO_.groupUuid, uuidSet)
                        .delete();
                sql(AccountGroupResourceRefVO.class)
                        .in(AccountGroupResourceRefVO_.groupUuid, uuidSet)
                        .delete();
                sql(AccountResourceRefVO.class)
                        .in(AccountResourceRefVO_.accountPermissionFrom, uuidSet)
                        .delete();
                sql(RoleAccountRefVO.class)
                        .in(RoleAccountRefVO_.accountPermissionFrom, uuidSet)
                        .delete();
                sql(AccountGroupVO.class)
                        .in(AccountGroupVO_.uuid, uuidSet)
                        .delete();
            }
        }.execute();
        completion.success();
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void move(String toUuid, Completion completion) {
        AccountGroupVO currentGroup = refreshVO();

        if (Objects.equals(currentGroup.getParentUuid(), toUuid)) {
            completion.success();
            return;
        }

        GroupNode current = buildGroupTreeOrThrow();
        if (Objects.equals(current.uuid, toUuid)) {
            completion.fail(err(IAM1Errors.GROUP_MOVE_TO_WRONG_PLACE,
                    "failed to move account group[uuid:%s] to it self", current.uuid));
            return;
        }

        if (current.hasChild(toUuid)) {
            completion.fail(err(IAM1Errors.GROUP_MOVE_TO_WRONG_PLACE,
                    "failed to move account group[uuid:%s] to its child group[uuid:%s]", current.uuid, toUuid));
            return;
        }

        GroupNode toNode;
        if (toUuid == null) {
            toNode = null;
        } else {
            GroupNode to = findSingleGroupNodeWithoutChildren(toUuid);
            if (to == null) {
                completion.fail(err(IAM1Errors.GROUP_MOVE_TO_WRONG_PLACE,
                        "invalid destination account group[uuid:%s]: not found", toUuid));
                return;
            }

            if (Objects.equals(to.rootGroupUuid, current.rootGroupUuid)) {
                toNode = current.root.find(to.uuid);
            } else {
                toNode = buildGroupTreeOrThrow(to.uuid, to.rootGroupUuid);
            }
        }

        String parentUuid = (toNode == null) ? null : toNode.uuid;
        String newRootUuid = (toNode == null) ? current.uuid : toNode.rootGroupUuid;

        Set<String> exitGroupUuids = current.ancestorUuidSet();
        Set<String> enterGroupUuids = (toNode == null) ? new HashSet<>() : toNode.selfAndAncestorUuidSet();

        Set<String> both = enterGroupUuids.stream()
                .filter(exitGroupUuids::contains)
                .collect(Collectors.toSet());
        exitGroupUuids.removeAll(both);
        enterGroupUuids.removeAll(both);

        Set<String> relatedGroupUuids = current.selfAndAllChildrenUuidSet();
        List<String> relatedAccountUuids = Q.New(AccountGroupAccountRefVO.class)
                .select(AccountGroupAccountRefVO_.accountUuid)
                .in(AccountGroupAccountRefVO_.groupUuid, relatedGroupUuids)
                .listValues();

        List<RoleAccountRefVO> roleRefs = roleRefsForAccountEnterGroups(relatedAccountUuids, enterGroupUuids);
        List<AccountResourceRefVO> resourceRefs = resourceRefsForAccountEnterGroups(relatedAccountUuids, enterGroupUuids);

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AccountGroupVO.class)
                        .eq(AccountGroupVO_.uuid, current.uuid)
                        .set(AccountGroupVO_.parentUuid, parentUuid)
                        .update();

                if (!Objects.equals(newRootUuid, current.rootGroupUuid)) {
                    sql(AccountGroupVO.class)
                            .in(AccountGroupVO_.uuid, relatedGroupUuids)
                            .set(AccountGroupVO_.rootGroupUuid, newRootUuid)
                            .update();
                }

                if (!exitGroupUuids.isEmpty() && !relatedAccountUuids.isEmpty()) {
                    sql(RoleAccountRefVO.class)
                            .in(RoleAccountRefVO_.accountUuid, relatedAccountUuids)
                            .in(RoleAccountRefVO_.accountPermissionFrom, exitGroupUuids)
                            .delete();
                    sql(AccountResourceRefVO.class)
                            .in(AccountResourceRefVO_.accountUuid, relatedAccountUuids)
                            .in(AccountResourceRefVO_.accountPermissionFrom, exitGroupUuids)
                            .delete();
                }

                if (!enterGroupUuids.isEmpty()) {
                    for (RoleAccountRefVO ref : roleRefs) {
                        persist(ref);
                    }
                    for (AccountResourceRefVO resourceRef : resourceRefs) {
                        persist(resourceRef);
                    }
                }
            }
        }.execute();

        completion.success();
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void addAccounts(List<String> accountUuids, Completion completion) {
        Set<String> alreadyInGroup = new HashSet<>(Q.New(AccountGroupAccountRefVO.class)
                .eq(AccountGroupAccountRefVO_.groupUuid, self.getUuid())
                .in(AccountGroupAccountRefVO_.accountUuid, accountUuids)
                .select(AccountGroupAccountRefVO_.accountUuid)
                .listValues());

        Set<String> needAdd = new HashSet<>(accountUuids);
        needAdd.removeAll(alreadyInGroup);

        if (needAdd.isEmpty()) {
            completion.success();
            return;
        }

        List<AccountGroupAccountRefVO> refs = new ArrayList<>(needAdd.size());
        for (String accountUuid : needAdd) {
            AccountGroupAccountRefVO ref = new AccountGroupAccountRefVO();
            ref.setAccountUuid(accountUuid);
            ref.setGroupUuid(self.getUuid());
            refs.add(ref);
        }

        GroupNode current = buildGroupTreeOrThrow();
        Set<String> relatedGroupUuids = current.selfAndAncestorUuidSet();

        List<RoleAccountRefVO> roleRefs = roleRefsForAccountEnterGroups(needAdd, relatedGroupUuids);
        List<AccountResourceRefVO> resourceRefs = resourceRefsForAccountEnterGroups(needAdd, relatedGroupUuids);

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (AccountGroupAccountRefVO ref : refs) {
                    persist(ref);
                }
                for (RoleAccountRefVO roleRef : roleRefs) {
                    persist(roleRef);
                }
                for (AccountResourceRefVO resourceRef : resourceRefs) {
                    persist(resourceRef);
                }
            }
        }.execute();
        completion.success();
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void removeAccounts(List<String> accountUuids, Completion completion) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AccountGroupAccountRefVO.class)
                        .eq(AccountGroupAccountRefVO_.groupUuid, self.getUuid())
                        .in(AccountGroupAccountRefVO_.accountUuid, accountUuids)
                        .delete();
                sql(RoleAccountRefVO.class)
                        .in(RoleAccountRefVO_.accountUuid, accountUuids)
                        .eq(RoleAccountRefVO_.accountPermissionFrom, self.getUuid())
                        .delete();
                sql(AccountResourceRefVO.class)
                        .in(RoleAccountRefVO_.accountUuid, accountUuids)
                        .eq(RoleAccountRefVO_.accountPermissionFrom, self.getUuid())
                        .delete();
            }
        }.execute();

        completion.success();
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void addRoles(List<String> roleUuids, Completion completion) {
        Set<String> alreadyInGroup = new HashSet<>(Q.New(AccountGroupRoleRefVO.class)
                .eq(AccountGroupRoleRefVO_.groupUuid, self.getUuid())
                .in(AccountGroupRoleRefVO_.roleUuid, roleUuids)
                .select(AccountGroupRoleRefVO_.roleUuid)
                .listValues());

        Set<String> needAdd = new HashSet<>(roleUuids);
        needAdd.removeAll(alreadyInGroup);

        if (needAdd.isEmpty()) {
            completion.success();
            return;
        }

        refreshVO();
        List<AccountGroupRoleRefVO> refs = new ArrayList<>(needAdd.size());
        List<RoleAccountRefVO> relatedRefs = new ArrayList<>();

        for (String roleUuid : needAdd) {
            AccountGroupRoleRefVO ref = new AccountGroupRoleRefVO();
            ref.setRoleUuid(roleUuid);
            ref.setGroupUuid(self.getUuid());
            refs.add(ref);
        }

        GroupNode current = buildGroupTreeOrThrow();
        Set<String> relatedGroupUuidSet = current.selfAndAllChildrenUuidSet();
        List<String> relatedAccountUuids = Q.New(AccountGroupAccountRefVO.class)
                .in(AccountGroupAccountRefVO_.groupUuid, relatedGroupUuidSet)
                .select(AccountGroupAccountRefVO_.accountUuid)
                .listValues();

        for (String accountUuid : relatedAccountUuids) {
            for (String roleUuid : needAdd) {
                RoleAccountRefVO ref = new RoleAccountRefVO();
                ref.setRoleUuid(roleUuid);
                ref.setAccountUuid(accountUuid);
                ref.setAccountPermissionFrom(self.getUuid());
                relatedRefs.add(ref);
            }
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                for (AccountGroupRoleRefVO ref : refs) {
                    persist(ref);
                }
                for (RoleAccountRefVO relatedRef : relatedRefs) {
                    persist(relatedRef);
                }
            }
        }.execute();

        completion.success();
    }

    @SyncThread(signature="account-group-organizational-form-update")
    public void removeRoles(List<String> roleUuids, Completion completion) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AccountGroupRoleRefVO.class)
                        .in(AccountGroupRoleRefVO_.roleUuid, roleUuids)
                        .eq(AccountGroupRoleRefVO_.groupUuid, self.getUuid())
                        .delete();
                sql(RoleAccountRefVO.class)
                        .in(RoleAccountRefVO_.roleUuid, roleUuids)
                        .eq(RoleAccountRefVO_.accountPermissionFrom, self.getUuid())
                        .delete();
            }
        }.execute();

        completion.success();
    }

    private GroupNode buildGroupTreeOrThrow() {
        return buildGroupTreeOrThrow(self.getUuid(), self.getRootGroupUuid());
    }

    public static GroupNode buildGroupTreeOrThrow(String currentGroupUuid) {
        String rootGroupUuid = Q.New(AccountGroupVO.class)
                .eq(AccountGroupVO_.uuid, currentGroupUuid)
                .select(AccountGroupVO_.rootGroupUuid)
                .findValue();
        return buildGroupTreeOrThrow(currentGroupUuid, rootGroupUuid);
    }

    public static GroupNode buildGroupTreeOrThrow(String currentGroupUuid, String rootGroupUuid) {
        GroupNode currentRoot = findGroupNodeAndChildren(rootGroupUuid);
        GroupNode current = currentRoot.find(currentGroupUuid);
        if (current == null) { // should not be here
            throw new CloudRuntimeException(
                    "broken account group form with rootGroupUuid: " + rootGroupUuid);
        }
        return current;
    }

    private List<RoleAccountRefVO> roleRefsForAccountEnterGroups(Collection<String> accountUuids, Collection<String> groupUuids) {
        if (isEmpty(groupUuids)) {
            return new ArrayList<>();
        }

        List<RoleAccountRefVO> list = new ArrayList<>();
        List<Tuple> groupRoleTuples = Q.New(AccountGroupRoleRefVO.class)
                .in(AccountGroupRoleRefVO_.groupUuid, groupUuids)
                .select(AccountGroupRoleRefVO_.groupUuid, AccountGroupRoleRefVO_.roleUuid)
                .listTuple();
        for (Tuple tuple : groupRoleTuples) {
            String groupUuid = tuple.get(0, String.class);
            String roleUuid = tuple.get(1, String.class);
            for (String accountUuid : accountUuids) {
                RoleAccountRefVO ref = new RoleAccountRefVO();
                ref.setAccountUuid(accountUuid);
                ref.setAccountPermissionFrom(groupUuid);
                ref.setRoleUuid(roleUuid);
                list.add(ref);
            }
        }

        return list;
    }

    private List<AccountResourceRefVO> resourceRefsForAccountEnterGroups(Collection<String> accountUuids, Collection<String> groupUuids) {
        if (isEmpty(groupUuids)) {
            return new ArrayList<>();
        }

        List<Tuple> groupResourceTuples = Q.New(AccountGroupResourceRefVO.class)
                .in(AccountGroupResourceRefVO_.groupUuid, groupUuids)
                .select(AccountGroupResourceRefVO_.groupUuid, AccountGroupResourceRefVO_.resourceUuid)
                .listTuple();
        if (isEmpty(groupResourceTuples)) {
            return new ArrayList<>();
        }

        Map<String, String> uuidTypeMap = toMap(
                Q.New(ResourceVO.class)
                        .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                        .in(ResourceVO_.uuid, transform(groupResourceTuples, tuple -> tuple.get(1, String.class)))
                        .listTuple(),
                tuple -> tuple.get(0, String.class),
                tuple -> tuple.get(1, String.class));

        List<AccountResourceRefVO> list = new ArrayList<>();
        for (Tuple tuple : groupResourceTuples) {
            String groupUuid = tuple.get(0, String.class);
            String masterResourceUuid = tuple.get(1, String.class);

            ResourceEnsembleInfo ensembleInfo = ResourceEnsembleHelper.findAllChildrenResources(
                    masterResourceUuid, ResourceTypeMetadata.resourceTypeForName(uuidTypeMap.get(masterResourceUuid)));
            if (ensembleInfo == null) {
                // find node with childUuid in tree(found by childUuid) must be not null.
                throw new CloudRuntimeException("should not be here");
            }

            for (ResourceEnsembleInfo resource : ensembleInfo.flatten()) {
                for (String accountUuid : accountUuids) {
                    AccountResourceRefVO ref = new AccountResourceRefVO();
                    ref.setAccountUuid(accountUuid);
                    ref.setAccountPermissionFrom(groupUuid);
                    ref.setResourceUuid(resource.uuid);
                    ref.setResourceType(resource.resourceType().getSimpleName());
                    ref.setResourcePermissionFrom(masterResourceUuid);
                    ref.setType(AccessLevel.Share);
                    list.add(ref);
                }
            }
        }

        return list;
    }
}
