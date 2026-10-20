package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.*;
import org.zstack.header.identity.IdentityCanonicalEvents.AccountDeletedData;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.header.identity.role.RolePolicyVO_;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountBase implements Account {
    private static final CLogger logger = Utils.getLogger(AccountBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private AccountVO self;

    public AccountBase(AccountVO self) {
        this.self = self;
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handle(APIUpdateAccountMsg msg) {
        APIUpdateAccountEvent evt = new APIUpdateAccountEvent(msg.getId());

        UpdateAccountMsg innerMsg = new UpdateAccountMsg();
        innerMsg.setUuid(msg.getUuid());
        innerMsg.setName(msg.getName());
        innerMsg.setPassword(msg.getPassword());
        innerMsg.setDescription(msg.getDescription());
        if (msg.getState() != null) {
            innerMsg.setState(AccountState.valueOf(msg.getState()));
        }

        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountConstant.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    evt.setInventory(((UpdateAccountReply) reply).getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(UpdateAccountMsg msg) {
        UpdateAccountReply reply = new UpdateAccountReply();
        AccountVO account = dbf.findByUuid(msg.getUuid(), AccountVO.class);

        if (msg.getPassword() != null) {
            for(PasswordUpdateExtensionPoint ext : pluginRgty.getExtensionList(PasswordUpdateExtensionPoint.class)) {
                ext.preUpdatePassword(account.getUuid(), account.getPassword(), msg.getPassword());
            }
        }

        if (msg.getName() != null) {
            account.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            account.setDescription(msg.getDescription());
        }
        if (msg.getState() != null) {
            account.setState(msg.getState());
        }

        boolean passwordUpdated = false;
        String oldPassword = null;
        if (msg.getPassword() != null && !msg.getPassword().equals(account.getPassword())) {
            oldPassword = account.getPassword();
            account.setPassword(msg.getPassword());
            passwordUpdated = true;

        }
        account = dbf.updateAndRefresh(account);

        if (passwordUpdated) {
            for(PasswordUpdateExtensionPoint ext : pluginRgty.getExtensionList(PasswordUpdateExtensionPoint.class)) {
                ext.afterUpdatePassword(account.getUuid(), oldPassword);
            }
        }

        // execute tf extension point
        final AccountInventory inventory = AccountInventory.valueOf(account);
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterUpdateAccountExtensionPoint.class),
                arg -> arg.afterUpdateAccount(inventory));

        reply.setInventory(AccountInventory.valueOf(account));
        bus.reply(msg, reply);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AccountDeletionMsg) {
            handle((AccountDeletionMsg) msg);
        } else if (msg instanceof UpdateAccountMsg) {
            handle((UpdateAccountMsg) msg);
        } else if (msg instanceof DeleteAccountMsg) {
            handle((DeleteAccountMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteAccountMsg msg) {
        DeleteAccountReply reply = new DeleteAccountReply();
        deleteAccount(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void deleteAccount(Completion completion) {
        final String issuer = AccountVO.class.getSimpleName();
        final List<AccountInventory> ctx = list(AccountInventory.valueOf(self));
        List<String> resourceUuids = Q.New(AccountResourceRefVO.class)
                                        .select(AccountResourceRefVO_.resourceUuid)
                                        .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                                        .listValues();
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-account-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "transfer-account-resource-to-admin";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        transferAllOwnedResourcesToAdmin();
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        dbf.remove(self);
                        acntMgr.adminAdoptAllOrphanedResource(resourceUuids, self.getUuid());

                        AccountDeletedData evtData = new AccountDeletedData();
                        evtData.setAccountUuid(self.getUuid());
                        evtData.setInventory(AccountInventory.valueOf(self));
                        evtf.fire(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, evtData);

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIDeleteAccountMsg msg) {
        final APIDeleteAccountEvent evt = new APIDeleteAccountEvent(msg.getId());

        deleteAccount(new Completion(msg) {
            @Override
            public void success() {
                // execute tf extension point
                final AccountInventory inventory = new AccountInventory();
                inventory.setUuid(msg.getUuid() );
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeDeleteAccountExtensionPoint.class),
                        arg -> arg.beforeDeleteAccount(inventory));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void transferAllOwnedResourcesToAdmin() {
        SQL.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .set(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .update();
    }


    private void deleteRelatedResources() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(QuotaVO.class)
                        .eq(QuotaVO_.identityType, AccountVO.class.getSimpleName())
                        .eq(QuotaVO_.identityUuid, self.getUuid())
                        .delete();

                sql(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                        .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                        .delete();

                List<String> resourceUuids = q(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.resourceUuid)
                        .eq(AccountResourceRefVO_.accountUuid, self.getUuid())
                        .eq(AccountResourceRefVO_.resourceType, RoleVO.class.getSimpleName())
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                        .listValues();

                if (!resourceUuids.isEmpty()) {
                    sql(RolePolicyVO.class)
                            .in(RolePolicyVO_.roleUuid, resourceUuids)
                            .delete();

                    sql(RoleVO.class)
                            .in(RoleVO_.uuid, resourceUuids)
                            .delete();
                }
            }
        }.execute();
    }

    private void handle(AccountDeletionMsg msg) {
        AccountDeletionReply reply = new AccountDeletionReply();
        deleteRelatedResources();
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateAccountMsg) {
            handle((APIUpdateAccountMsg) msg);
        } else if (msg instanceof APIUpdateQuotaMsg) {
            handle((APIUpdateQuotaMsg) msg);
        } else if (msg instanceof APIDeleteAccountMsg) {
            handle((APIDeleteAccountMsg) msg);
        } else if (msg instanceof APIGetAccountQuotaUsageMsg) {
            handle((APIGetAccountQuotaUsageMsg) msg);
        } else if (msg instanceof APIChangeAccountTypeMsg) {
            handle((APIChangeAccountTypeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeAccountTypeMsg msg) {
        APIChangeAccountTypeEvent evt = new APIChangeAccountTypeEvent(msg.getId());
        String accountUuid = msg.getUuid();
        AccountVO account = dbf.findByUuid(accountUuid, AccountVO.class);
        if (account == null) {
            evt.setError(argerr("Cannot find account[uuid:%s]", accountUuid));
            bus.publish(evt);
            return;
        }
        AccountType originalAccountType = account.getType();
        AccountType targetAccountType = AccountType.valueOf(msg.getType());
        List<AccountTypeChangedExtensionPoint> extensions = pluginRgty.getExtensionList(AccountTypeChangedExtensionPoint.class);
        for (AccountTypeChangedExtensionPoint extension : extensions) {
            ErrorCode errorCode = extension.preAccountTypeChange(accountUuid, originalAccountType, targetAccountType);
            if (errorCode != null) {
                evt.setError(errorCode);
                bus.publish(evt);
                return;
            }
        }
        CollectionUtils.forEach(extensions, ext -> ext.beforeAccountTypeChange(accountUuid, originalAccountType, targetAccountType));

        account.setType(targetAccountType);
        account = dbf.updateAndRefresh(account);
        CollectionUtils.forEach(extensions, ext -> ext.afterAccountTypeChange(accountUuid, targetAccountType));
        evt.setInventory(AccountInventory.valueOf(account));
        bus.publish(evt);
    }

    private void handle(APIGetAccountQuotaUsageMsg msg) {
        APIGetAccountQuotaUsageReply reply = new APIGetAccountQuotaUsageReply();

        List<Quota.QuotaUsage> usages = new ArrayList<>();
        for (QuotaDefinition q : acntMgr.getQuotasDefinitions().values()) {
            Long used = q.getQuotaUsage(msg.getAccountUuid());

            if (used == null) {
                continue;
            }

            Quota.QuotaUsage usage = new Quota.QuotaUsage();
            usage.setUsed(used);
            usage.setName(q.getName());
            usages.add(usage);
        }

        Map<String, Quota.QuotaUsage> umap = new HashMap<String, Quota.QuotaUsage>();
        for (Quota.QuotaUsage usage : usages) {
            umap.put(usage.getName(), usage);
        }

        List<QuotaVO> vos = Q.New(QuotaVO.class)
                .eq(QuotaVO_.identityUuid, msg.getAccountUuid())
                .eq(QuotaVO_.identityType, AccountVO.class.getSimpleName())
                .in(QuotaVO_.name, umap.keySet())
                .list();
        Map<String, QuotaVO> vmap = new HashMap<>();
        for (QuotaVO vo : vos) {
            vmap.put(vo.getName(), vo);
        }

        for (Map.Entry<String, Quota.QuotaUsage> e : umap.entrySet()) {
            Quota.QuotaUsage u = e.getValue();
            QuotaVO vo = vmap.get(u.getName());
            u.setTotal(vo == null ? 0 : vo.getValue());
        }

        reply.setUsages(new ArrayList<>(umap.values()));
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateQuotaMsg msg) {
        QuotaVO quota = msg.getQuotaVO();
        quota.setValue(msg.getValue());
        quota = dbf.updateAndRefresh(quota);

        APIUpdateQuotaEvent evt = new APIUpdateQuotaEvent(msg.getId());
        evt.setInventory(QuotaInventory.valueOf(quota));
        bus.publish(evt);
    }
}
