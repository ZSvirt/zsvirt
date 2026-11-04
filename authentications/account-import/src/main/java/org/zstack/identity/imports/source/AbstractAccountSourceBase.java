package org.zstack.identity.imports.source;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountSource;
import org.zstack.header.identity.AccountState;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.UpdateAccountMsg;
import org.zstack.header.identity.UpdateAccountReply;
import org.zstack.header.message.Message;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.entity.SyncUpdateAccountStrategy;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.header.ImportAccountItem;
import org.zstack.identity.imports.header.ImportAccountResult;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.identity.imports.header.SyncTaskResult;
import org.zstack.identity.imports.header.SyncTaskSpec;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountsSpec;
import org.zstack.identity.imports.message.BindThirdPartyAccountMsg;
import org.zstack.identity.imports.message.BindThirdPartyAccountReply;
import org.zstack.identity.imports.message.DestroyThirdPartyAccountSourceMsg;
import org.zstack.identity.imports.message.DestroyThirdPartyAccountSourceReply;
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply;
import org.zstack.identity.imports.message.SyncThirdPartyAccountMsg;
import org.zstack.identity.imports.message.SyncThirdPartyAccountReply;
import org.zstack.identity.imports.message.UnbindThirdPartyAccountMsg;
import org.zstack.identity.imports.message.UnbindThirdPartyAccountReply;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.CreateAccountMsg;
import org.zstack.header.identity.CreateAccountReply;
import org.zstack.header.identity.DeleteAccountMsg;
import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.identity.AccountState.*;
import static org.zstack.identity.imports.AccountImportsGlobalConfig.AUTO_SYNC_ENABLE;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceSyncTaskSignature;
import static org.zstack.identity.imports.entity.SyncUpdateAccountStrategy.*;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractAccountSourceBase {
    private static final CLogger logger = Utils.getLogger(AbstractAccountSourceBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade databaseFacade;
    @Autowired
    protected ThreadFacade threadFacade;
    @Autowired
    protected ResourceConfigFacade resourceConfigFacade;

    protected AbstractAccountSourceBase(ThirdPartyAccountSourceVO self) {
        this.self = Objects.requireNonNull(self);
    }

    protected ThirdPartyAccountSourceVO self;

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ImportThirdPartyAccountMsg) {
            handle(((ImportThirdPartyAccountMsg) msg));
        } else if (msg instanceof BindThirdPartyAccountMsg) {
            handle(((BindThirdPartyAccountMsg) msg));
        } else if (msg instanceof UnbindThirdPartyAccountMsg) {
            handle(((UnbindThirdPartyAccountMsg) msg));
        } else if (msg instanceof SyncThirdPartyAccountMsg) {
            handle(((SyncThirdPartyAccountMsg) msg));
        } else if (msg instanceof DestroyThirdPartyAccountSourceMsg) {
            handle(((DestroyThirdPartyAccountSourceMsg) msg));
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    public void handle(ImportThirdPartyAccountMsg message) {
        ImportThirdPartyAccountReply reply = new ImportThirdPartyAccountReply();
        final String sourceUuid = message.getSourceUuid();

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                importAccounts(message.getSpec(), new ReturnValueCompletion<List<ImportAccountResult>>(chain) {
                    @Override
                    public void success(List<ImportAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "import-accounts-from-source-" + sourceUuid;
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public final void importAccounts(ImportAccountSpec spec, ReturnValueCompletion<List<ImportAccountResult>> completion) {
        List<ImportThirdPartyAccountContext> contexts = new ArrayList<>(spec.getAccountList().size());
        List<ImportThirdPartyAccountContext> validContexts = new ArrayList<>(spec.getAccountList().size());

        SyncAccountStateHelper stateMachine = new SyncAccountStateHelper();
        stateMachine.setSyncCreateStrategy(spec.getSyncCreateStrategy());

        // from spec.getSyncUpdateStrategies() > "VO.UpdateAccountStrategies" > from "CreateAccountStrategy"
        List<SyncUpdateAccountStrategy> defaultUpdateStrategies = SyncUpdateAccountStrategy.effectiveStrategies(
                SyncUpdateAccountStrategy.valueOfStrategies(self.getUpdateAccountStrategies()),
                SyncUpdateAccountStrategy.from(spec.getSyncCreateStrategy()));
        final List<SyncUpdateAccountStrategy> effectiveUpdateStrategies = SyncUpdateAccountStrategy.effectiveStrategies(
                spec.getSyncUpdateStrategies(),
                defaultUpdateStrategies);
        logger.debug("effective update strategies: " + effectiveUpdateStrategies);
        stateMachine.setSyncUpdateStrategies(effectiveUpdateStrategies);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("chain-with-importing-accounts-from-source-%s", spec.getSourceUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "validate-spec";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                buildContextsFromSpec();
                fillBindingIfExists();
                fillAccountIfAccountUuidMatched();
                fillAccountIfNameMatched();
                validateIfAccountNotPresent();
                validateIfAccountBindOtherSource();
                trigger.next();
            }

            private void buildContextsFromSpec() {
                for (ImportAccountItem accountSpec : spec.getAccountList()) {
                    final ImportThirdPartyAccountContext context = new ImportThirdPartyAccountContext();
                    context.spec = accountSpec;
                    contexts.add(context);
                    validContexts.add(context);
                }
            }

            private void fillBindingIfExists() {
                Map<String, ImportThirdPartyAccountContext> credentialsContextMap = toMap(
                        contexts, context -> context.spec.getCredentials(), Function.identity());
                int totalSize = credentialsContextMap.size();

                Map<String, AccountThirdPartyAccountSourceRefVO> credentialsRefMap = new HashMap<>();
                SQL.New("select ref " +
                        "from AccountThirdPartyAccountSourceRefVO ref " +
                        "where " +
                                "credentials in (:credentialsSet) and " +
                                "accountSourceUuid = :sourceUuid",
                                AccountThirdPartyAccountSourceRefVO.class)
                        .param("credentialsSet", credentialsContextMap.keySet())
                        .param("sourceUuid", self.getUuid())
                        .limit(25)
                        .paginate(totalSize, (List<AccountThirdPartyAccountSourceRefVO> refs) -> {
                            for (AccountThirdPartyAccountSourceRefVO ref : refs) {
                                credentialsRefMap.put(ref.getCredentials(), ref);
                            }
                        });

                credentialsRefMap.forEach((credentials, ref) -> {
                    ImportThirdPartyAccountContext context = credentialsContextMap.get(credentials);
                    if (context.spec.getAccountUuid() != null &&
                            !context.spec.getAccountUuid().equals(ref.getAccountUuid())) {
                        context.errorForValidation = operr(
                                "third party user[credentials=%s] has already binding to other account", credentials);
                        validContexts.remove(context);
                        return;
                    }

                    context.ref = ref;
                    context.bindingExisting = true;
                    context.spec.setAccountUuid(ref.getAccountUuid());
                    logger.debug(String.format(
                            "account[uuid=%s] has already binding third party source[uuid=%s], skip binding execution",
                            ref.getAccountUuid(), self.getUuid()));
                });
            }

            private void fillAccountIfAccountUuidMatched() {
                final Set<String> accountUuidSet =
                        transformToSetAndRemoveNull(validContexts, context -> context.spec.getAccountUuid());

                final Map<String, AccountVO> uuidExistingAccountMap = new HashMap<>();
                if (!accountUuidSet.isEmpty()) {
                    int totalSize = accountUuidSet.size();

                    SQL.New("select account from AccountVO account where uuid in (:uuidSet)", AccountVO.class)
                            .param("uuidSet", accountUuidSet)
                            .limit(25)
                            .paginate(totalSize, (List<AccountVO> accounts) -> {
                                for (AccountVO account : accounts) {
                                    uuidExistingAccountMap.put(account.getUuid(), account);
                                }
                            });
                }

                for (ImportThirdPartyAccountContext context : contexts) {
                    String accountUuid = context.spec.getAccountUuid();
                    if (accountUuid == null) {
                        continue;
                    }

                    AccountVO account = uuidExistingAccountMap.get(accountUuid);
                    if (account != null) {
                        context.account = AccountInventory.valueOf(account);
                        context.accountExisting = true;
                    }
                }
            }

            private void fillAccountIfNameMatched() {
                if (effectiveUpdateStrategies.contains(BindingNone)
                        || (!effectiveUpdateStrategies.contains(BindingNormalAccount)
                        && !effectiveUpdateStrategies.contains(BindingSystemAdmin))) {
                    return;
                }

                final Set<String> nameSet = validContexts.stream()
                        .filter(context -> context.account == null)
                        .map(context -> context.spec.getUsername())
                        .collect(Collectors.toSet());
                if (nameSet.isEmpty()) {
                    return;
                }

                Set<AccountType> accountTypes = new HashSet<>();
                accountTypes.add(AccountType.Normal);
                if (effectiveUpdateStrategies.contains(BindingSystemAdmin)) {
                    accountTypes.add(AccountType.SystemAdmin);
                }

                final Map<String, AccountVO> nameAccountMap = new HashMap<>();
                SQL.New("select account from AccountVO account where name in (:nameSet) and type in (:typeSet)", AccountVO.class)
                        .param("nameSet", nameSet)
                        .param("typeSet", accountTypes)
                        .limit(25)
                        .paginate(nameSet.size(), (List<AccountVO> accounts) -> {
                            for (AccountVO account : accounts) {
                                nameAccountMap.put(account.getName(), account);
                            }
                        });

                // find account:
                // 1. name matched
                // 2. not binding to this account source
                // 3. not equals to validContexts[x].account
                Set<String> validAccounts = transformToSetAndRemoveNull(validContexts,
                        context -> context.account == null ? null : context.account.getUuid());
                Set<String> accountsForNameMatched = new HashSet<>();

                for (ImportThirdPartyAccountContext context : validContexts) {
                    if (context.account != null) {
                        continue;
                    }
                    AccountVO account = nameAccountMap.get(context.spec.getUsername());
                    if (account == null) {
                        continue;
                    }

                    if (accountsForNameMatched.contains(account.getUuid()) || validAccounts.contains(account.getUuid())) {
                        context.errorForValidation = operr(
                                "account name[%s] is already exists and ready to binding other account source user",
                                context.spec.getUsername());
                        continue;
                    }
                    accountsForNameMatched.add(account.getUuid());
                }
                validContexts.removeIf(context -> context.errorForValidation != null);

                if (accountsForNameMatched.isEmpty()) {
                    return;
                }

                List<String> alreadyBindingAccount = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                        .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, self.getUuid())
                        .in(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountsForNameMatched)
                        .select(AccountThirdPartyAccountSourceRefVO_.accountUuid)
                        .listValues();

                for (ImportThirdPartyAccountContext context : validContexts) {
                    if (context.account != null) {
                        continue;
                    }
                    AccountVO account = nameAccountMap.get(context.spec.getUsername());
                    if (account == null) {
                        continue;
                    }

                    if (alreadyBindingAccount.contains(account.getUuid())) {
                        context.errorForValidation = operr(
                                "account name[%s] is already binding to other account source user",
                                context.spec.getUsername());
                        continue;
                    }

                    context.account = AccountInventory.valueOf(account);
                    context.accountExisting = true;
                }
                validContexts.removeIf(context -> context.errorForValidation != null);
            }

            private void validateIfAccountNotPresent() {
                for (Iterator<ImportThirdPartyAccountContext> it = validContexts.iterator(); it.hasNext();) {
                    ImportThirdPartyAccountContext context = it.next();
                    final ImportAccountItem accountSpec = context.spec;

                    if (spec.isCreateIfNotExist() || context.account != null) {
                        continue;
                    }

                    if (accountSpec.getAccountUuid() == null) {
                        context.errorForValidation = operr("invalid account spec: accountUuid is null");
                        it.remove();
                        continue;
                    }

                    context.errorForValidation = operr("invalid account spec: failed to find account[uuid=%s]",
                            accountSpec.getAccountUuid());
                    it.remove();
                }
            }

            private void validateIfAccountBindOtherSource() {
                final List<ImportThirdPartyAccountContext> filteredContexts = validContexts.stream()
                        .filter(context -> context.accountExisting)
                        .filter(context -> context.ref == null)
                        .collect(Collectors.toList());
                if (filteredContexts.isEmpty()) {
                    return;
                }

                final List<String> accountUuidList = transform(filteredContexts, context -> context.account.getUuid());
                final List<String> accountsBindingToOtherSource = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                        .in(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountUuidList)
                        .notEq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, self.getUuid())
                        .select(AccountThirdPartyAccountSourceRefVO_.accountUuid)
                        .listValues();
                for (String accountUuid : accountsBindingToOtherSource) {
                    ImportThirdPartyAccountContext context = findOneOrNull(contexts, c -> {
                        String contextAccount = c.account == null ? c.spec.getAccountUuid() : c.account.getUuid();
                        return Objects.equals(contextAccount, accountUuid);
                    });
                    if (context == null) {
                        continue;
                    }
                    context.errorForValidation =
                            operr("account[uuid=%s] has already binding other third party source",
                            accountUuid);
                    validContexts.remove(context);
                }
            }
        }).then(new Flow() {
            String __name__ = "generate-accounts";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(new ArrayList<>(validContexts)).step((context, whileCompletion) -> {
                    if (context.accountExisting) {
                        updateAccountIfNeeded(context, whileCompletion);
                    } else {
                        createAccount(context, whileCompletion);
                    }
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to generate accounts when imports accounts but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }

            private void createAccount(ImportThirdPartyAccountContext context, WhileCompletion whileCompletion) {
                String accountUuid = context.spec.getAccountUuid() == null ?
                        Platform.getUuid() : context.spec.getAccountUuid();
                AccountState stateInAccountSource = context.spec.isEnable() ? Enabled : Disabled;
                AccountState stateUpdateTo = stateMachine.transformForNewCreateAccount(stateInAccountSource);

                logger.info(String.format(
                        "account[uuid=%s] newlyCreate stateInAccountSource=%s stateUpdateTo=%s %s",
                        accountUuid, stateInAccountSource, stateUpdateTo, stateMachine));

                AccountType accountType = context.spec.getAccountType() != null ?
                        context.spec.getAccountType() : AccountType.Normal;
                if (accountType == AccountType.ThirdParty) {
                    accountType = AccountType.Normal;
                }

                CreateAccountMsg message = new CreateAccountMsg();
                message.setUuid(accountUuid);
                message.setName(context.spec.getUsername());
                message.setPassword(generatePassword());
                message.setType(accountType.toString());
                message.setSource(AccountSource.valueOf(self.getType()).toString());
                message.setState(stateUpdateTo);

                bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, accountUuid);
                bus.send(message, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            context.account = ((CreateAccountReply) reply).getInventory();
                        } else {
                            context.errorForAccountExecution = reply.getError();
                            validContexts.remove(context);
                        }
                        whileCompletion.done();
                    }
                });
            }

            private void updateAccountIfNeeded(ImportThirdPartyAccountContext context, WhileCompletion whileCompletion) {
                final AccountInventory account = context.account;
                boolean updated = false;
                UpdateAccountMsg message = new UpdateAccountMsg();

                AccountState originalState = AccountState.valueOf(account.getState());
                AccountState stateInAccountSource = context.spec.isEnable() ? Enabled : Disabled;
                AccountState stateUpdateTo = stateMachine.transform(originalState, stateInAccountSource);
                logger.info(String.format(
                        "account[uuid=%s] originalState=%s stateInAccountSource=%s stateUpdateTo=%s %s",
                        account.getUuid(), originalState, stateInAccountSource, stateUpdateTo, stateMachine));

                if (originalState != stateUpdateTo) {
                    message.setState(stateUpdateTo);
                    updated = true;
                }

                if (!updated) {
                    whileCompletion.done();
                    return;
                }

                context.readyToUpdateAccount = true;
                message.setUuid(account.getUuid());
                bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, account.getUuid());
                bus.send(message, new CloudBusCallBack(whileCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            context.account = ((UpdateAccountReply) reply).getInventory();
                        } else {
                            context.errorForAccountExecution = reply.getError();
                            validContexts.remove(context);
                        }
                        whileCompletion.done();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                new While<>(contexts).each((context, whileCompletion) -> {
                    if (context.account != null || !context.readyToCreateAccount) {
                        whileCompletion.done();
                        return;
                    }

                    DeleteAccountMsg message = new DeleteAccountMsg();
                    message.setUuid(context.account.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, context.account.getUuid());
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("destroy account[uuid=%s, name=%s] failed",
                                        context.account.getUuid(), context.account.getName()));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to rollback imports accounts: " + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.rollback();
                    }
                });
            }

            private String generatePassword() {
                return Platform.getUuid() + Platform.getUuid();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "bind-accounts-with-import-source";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ImportThirdPartyAccountContext> filteredContexts =
                        filter(validContexts, context -> !context.bindingExisting);

                if (filteredContexts.isEmpty()) {
                    trigger.next();
                    return;
                }

                for (ImportThirdPartyAccountContext context : filteredContexts) {
                    AccountThirdPartyAccountSourceRefVO ref = new AccountThirdPartyAccountSourceRefVO();
                    ref.setCredentials(context.spec.getCredentials());
                    ref.setAccountSourceUuid(spec.getSourceUuid());
                    ref.setAccountUuid(context.account.getUuid());
                    context.ref = ref;
                }

                databaseFacade.persistCollection(transform(filteredContexts, result -> result.ref));
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(transform(contexts, ImportThirdPartyAccountContext::makeResult));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(operr("failed to import account from source[uuid=%s, type=%s]",
                        spec.getSourceUuid(), spec.getSourceType())
                                .withCause(errCode));
            }
        }).start();
    }

    private void handle(BindThirdPartyAccountMsg message) {
        BindThirdPartyAccountReply reply = new BindThirdPartyAccountReply();
        final String sourceUuid = message.getSourceUuid();

        ImportAccountSpec batch = new ImportAccountSpec();
        batch.setSourceUuid(sourceUuid);
        batch.setSourceType(self.getType());

        ImportAccountItem spec = new ImportAccountItem();
        spec.setAccountUuid(message.getAccountUuid());
        spec.setCredentials(Objects.requireNonNull(message.getCredentials()));
        batch.getAccountList().add(spec);

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                importAccounts(batch, new ReturnValueCompletion<List<ImportAccountResult>>(chain) {
                    @Override
                    public void success(List<ImportAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "bind-account-from-source-" + sourceUuid;
            }
        });
    }

    private void handle(UnbindThirdPartyAccountMsg message) {
        UnbindThirdPartyAccountReply reply = new UnbindThirdPartyAccountReply();
        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                unbindingAccount(message.getSpec(), new ReturnValueCompletion<List<UnbindThirdPartyAccountResult>>(chain) {
                    @Override
                    public void success(List<UnbindThirdPartyAccountResult> results) {
                        chain.next();
                        reply.setResults(results);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(self.getUuid());
            }

            @Override
            public String getName() {
                return "unbinding-account-from-source-" + self.getUuid();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void unbindingAccount(UnbindThirdPartyAccountsSpec spec, ReturnValueCompletion<List<UnbindThirdPartyAccountResult>> completion) {
        if (spec.getAccountUuidList().isEmpty()) {
            completion.success(new ArrayList<>());
            return;
        }

        List<UnbindThirdPartyAccountsContext> contexts = new ArrayList<>();
        for (String accountUuid : spec.getAccountUuidList()) {
            UnbindThirdPartyAccountsContext context = new UnbindThirdPartyAccountsContext();
            context.accountUuid = accountUuid;
            context.sourceUuid = self.getUuid();
            contexts.add(context);
        }

        SyncAccountStateHelper stateMachine = new SyncAccountStateHelper();
        stateMachine.setSyncDeleteStrategy(spec.getSyncDeleteStrategy());
        AccountState stateUpdateTo = stateMachine.transformForDeletedAccount(Enabled);

        logger.info(String.format(
                "account[uuid=%s] deletedInAccountSource stateUpdateTo=%s %s",
                spec.getAccountUuidList(), stateUpdateTo, stateMachine));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("chain-with-unbinding-accounts-from-source-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "unbinding-account";

            @Override
            public boolean skip(Map data) {
                return !spec.isRemoveBindingOnly();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<String> accountUuidList = spec.getAccountUuidList();

                SQL.New(AccountThirdPartyAccountSourceRefVO.class)
                        .in(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountUuidList)
                        .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, self.getUuid())
                        .delete();

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-account-if-needed";

            @Override
            public boolean skip(Map data) {
                return spec.isRemoveBindingOnly() || stateUpdateTo != null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(contexts).step((context, whileCompletion) -> {
                    final String accountUuid = context.accountUuid;

                    DeleteAccountMsg message = new DeleteAccountMsg();
                    message.setUuid(accountUuid);
                    bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, accountUuid);
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("destroy account[uuid=%s] failed, still continue", accountUuid));
                                context.errorForAccountExecution = reply.getError();
                            }
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to delete accounts but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-account-state-if-needed";

            @Override
            public boolean skip(Map data) {
                return spec.isRemoveBindingOnly() || stateUpdateTo == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(contexts).each((context, whileCompletion) -> {
                    final String accountUuid = context.accountUuid;

                    UpdateAccountMsg message = new UpdateAccountMsg();
                    message.setUuid(accountUuid);
                    message.setState(stateUpdateTo);
                    bus.makeTargetServiceIdByResourceUuid(message, AccountConstant.SERVICE_ID, accountUuid);
                    bus.send(message, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("update account[uuid=%s] state failed, still continue", accountUuid));
                                context.errorForAccountExecution = reply.getError();
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to update account states but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(transform(contexts, UnbindThirdPartyAccountsContext::makeResult));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(operr("failed to unbinding accounts from source[uuid=%s, type=%s]",
                        self.getUuid(), self.getType())
                                .withCause(errCode));
            }
        }).start();
    }

    private void handle(SyncThirdPartyAccountMsg message) {
        SyncThirdPartyAccountReply reply = new SyncThirdPartyAccountReply();

        SyncCreatedAccountStrategy createStrategy = message.getCreateAccountStrategy() == null ?
                self.getCreateAccountStrategy() : message.getCreateAccountStrategy();
        SyncDeletedAccountStrategy deleteStrategy = message.getDeleteAccountStrategy() == null ?
                self.getDeleteAccountStrategy() : message.getDeleteAccountStrategy();

        SyncTaskSpec spec = new SyncTaskSpec();
        spec.setSourceUuid(self.getUuid());
        spec.setSourceType(self.getType());
        spec.setCreateAccountStrategy(createStrategy);
        spec.setDeleteAccountStrategy(deleteStrategy);

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                syncAccountsFromSource(spec, new ReturnValueCompletion<SyncTaskResult>(chain) {
                    @Override
                    public void success(SyncTaskResult result) {
                        chain.next();
                        reply.setResult(result);
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceSyncTaskSignature();
            }

            @Override
            public String getName() {
                return "sync-accounts-from-source-" + self.getUuid();
            }
        });
    }

    protected abstract void syncAccountsFromSource(SyncTaskSpec spec, ReturnValueCompletion<SyncTaskResult> completion);

    private void handle(DestroyThirdPartyAccountSourceMsg message) {
        DestroyThirdPartyAccountSourceReply reply = new DestroyThirdPartyAccountSourceReply();
        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                destroySourceAndRemoveThirdPartyAccounts(new Completion(chain) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(message, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(message, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(self.getUuid());
            }

            @Override
            public String getName() {
                return "destroy-source-" + self.getUuid();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void destroySourceAndRemoveThirdPartyAccounts(Completion completion) {
        List<String> accountUuidList = new ArrayList<>();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("chain-with-deleting-source-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "disable-syncing-for-deleted-account-source";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(AUTO_SYNC_ENABLE.getIdentity());
                resourceConfig.updateValue(self.getUuid(), Boolean.FALSE.toString());
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "gather-related-accounts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                gatherAccountsForDestroySource(new ReturnValueCompletion<List<String>>(trigger) {
                    @Override
                    public void success(List<String> returnValue) {
                        accountUuidList.addAll(returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "destroy-source";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                destroySource(new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            String __name__ = "destroy-related-accounts";

            @Override
            public boolean skip(Map data) {
                return accountUuidList.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(splitAccountLists())
                        .enableProgressReport("cleaning-stale-accounts-for-deleting-account-source")
                        .each(this::cleanStaleAccounts)
                        .run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("failed to delete related account (for destroy account source) but still continue: "
                                    + errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }

            private void cleanStaleAccounts(List<String> accounts, WhileCompletion whileCompletion) {
                UnbindThirdPartyAccountsSpec spec = new UnbindThirdPartyAccountsSpec();
                spec.setSourceUuid(self.getUuid());
                spec.setSourceType(self.getType());
                spec.setAccountUuidList(accounts);
                spec.setSyncDeleteStrategy(SyncDeletedAccountStrategy.DeleteAccount);
                spec.setRemoveBindingOnly(false);

                unbindingAccount(spec, new ReturnValueCompletion<List<UnbindThirdPartyAccountResult>>(whileCompletion) {
                    @Override
                    public void success(List<UnbindThirdPartyAccountResult> returnValue) {
                        whileCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        whileCompletion.addError(errorCode);
                        whileCompletion.done();
                    }
                });
            }

            private List<List<String>> splitAccountLists() {
                List<List<String>> results = new ArrayList<>();
                int offset = 0;
                int totalSize = accountUuidList.size();

                while (offset <= totalSize) {
                    results.add(accountUuidList.subList(offset, Math.min(offset + 50, accountUuidList.size())));
                    offset += 50;
                }
                return results;
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(operr("failed to delete source[uuid=%s, type=%s]", self.getUuid(), self.getType())
                                .withCause(errCode));
            }
        }).start();
    }

    protected abstract void destroySource(Completion completion);

    protected void gatherAccountsForDestroySource(ReturnValueCompletion<List<String>> completion) {
        long totalSize = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, self.getUuid())
                .count();

        List<String> results = new ArrayList<>((int) totalSize);
        // Imported accounts (non-local source) bound to this source will be destroyed
        SQL.New("select " +
                        "account.uuid " +
                "from " +
                        "AccountThirdPartyAccountSourceRefVO ref, " +
                        "AccountVO account " +
                "where " +
                        "ref.accountSourceUuid = :sourceUuid and " +
                        "ref.accountUuid = account.uuid and " +
                        "account.source != :localSource",
                        String.class)
                .param("sourceUuid", self.getUuid())
                .param("localSource", AccountSource.Local)
                .limit(300)
                .paginate(totalSize, (List<String> accountUuids) -> results.addAll(accountUuids));
        completion.success(results);
    }
}
