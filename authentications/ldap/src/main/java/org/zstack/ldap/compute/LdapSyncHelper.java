package org.zstack.ldap.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.HardcodedFilter;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountSource;
import org.zstack.header.identity.AccountType;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.header.ImportAccountItem;
import org.zstack.identity.imports.header.ImportAccountResult;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.identity.imports.header.SyncTaskResult;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountsSpec;
import org.zstack.identity.imports.message.ImportThirdPartyAccountMsg;
import org.zstack.identity.imports.message.ImportThirdPartyAccountReply;
import org.zstack.identity.imports.message.UnbindThirdPartyAccountMsg;
import org.zstack.identity.imports.message.UnbindThirdPartyAccountReply;
import org.zstack.ldap.LdapConstant;
import org.zstack.ldap.driver.LdapSearchSpec;
import org.zstack.ldap.driver.LdapUtil;
import org.zstack.ldap.entity.LdapEntryAttributeInventory;
import org.zstack.ldap.entity.LdapEntryInventory;
import org.zstack.ldap.header.LdapSyncTaskSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.err;
import static org.zstack.ldap.LdapConstant.DEFAULT_PERSON_FILTER;
import static org.zstack.ldap.LdapErrors.LDAP_SYNC_ERROR;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LdapSyncHelper {
    private static final CLogger logger = Utils.getLogger(LdapSyncHelper.class);

    @Autowired
    private CloudBus bus;

    private LdapSyncTaskSpec taskSpec;
    private final ImportAccountSpec importSpec;
    private LdapUtil ldapUtil;
    private LdapSyncTaskResult progress;

    public LdapSyncHelper(LdapSyncTaskSpec spec) {
        this.taskSpec = Objects.requireNonNull(spec);

        importSpec = new ImportAccountSpec();
        importSpec.setSourceType(AccountSource.fromLdapServerTypeName(
                taskSpec.getServerType() == null ? null : taskSpec.getServerType().name()).name());
        importSpec.setSourceUuid(spec.sourceUuid);
        importSpec.setSyncCreateStrategy(taskSpec.getCreateAccountStrategy());

        ldapUtil = Platform.New(LdapUtil::new);

        progress = new LdapSyncTaskResult()
                .withLdapServer(importSpec.getSourceUuid())
                .withSourceType(importSpec.getSourceType())
                .withExistingRecordCount(Q.New(AccountThirdPartyAccountSourceRefVO.class)
                        .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, importSpec.getSourceUuid())
                        .count());
    }

    @SuppressWarnings({"rawtypes"})
    public void run(ReturnValueCompletion<SyncTaskResult> completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("sync-ldap-server-%s", importSpec.getSourceUuid()));
        chain.enableProgressReport();
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-uid";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String userFilter = buildFilter();
                logger.debug("user filter is " + userFilter);

                LdapSearchSpec searchSpec = new LdapSearchSpec();
                searchSpec.setLdapServerUuid(importSpec.getSourceUuid());
                searchSpec.setFilter(userFilter);
                searchSpec.setCount(taskSpec.getMaxAccountCount());
                searchSpec.setReturningAttributes(buildReturnAttribute());
                searchSpec.setSearchAllAttributes(false);

                List<LdapEntryInventory> results = ldapUtil.searchLdapEntry(searchSpec);
                for (LdapEntryInventory ldapEntry : results) {
                    try {
                        importSpec.accountList.add(generateAccountSpec(ldapEntry));
                    } catch (Exception e) {
                        logger.warn("failed to sync ldap entry[], ignore this account", e);
                    }
                }
                progress.withSearchRecordCount(importSpec.accountList.size());
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "import-accounts";

            @Override
            public boolean skip(Map data) {
                return taskSpec.getCreateAccountStrategy() == SyncCreatedAccountStrategy.NoAction ||
                        progress.getImportStage().getTotal() == 0;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                AtomicBoolean anySuccess = new AtomicBoolean(false);

                new While<>(splitMessageFromImportSpec())
                        .enableProgressReport("import-accounts-from-ldap-server")
                        .each((msg, whileCompletion) -> {
                    bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            recordImportReply(msg, reply);
                            if (reply.isSuccess()) {
                                anySuccess.set(true);
                            } else {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!anySuccess.get()) {
                            trigger.fail(err(LDAP_SYNC_ERROR,
                                    "all ldap account importing attempt is failed. ldapServerUuid=%s",
                                    importSpec.getSourceUuid())
                                    .withCause(errorCodeList));
                            return;
                        } else if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("ldap account importing occur some errors: " +
                                    errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }

            private List<ImportThirdPartyAccountMsg> splitMessageFromImportSpec() {
                int accountCount = importSpec.accountList.size();

                if (accountCount <= 100) {
                    ImportThirdPartyAccountMsg msg = new ImportThirdPartyAccountMsg();
                    msg.setSpec(importSpec);
                    return list(msg);
                }

                int count = 0;
                List<ImportThirdPartyAccountMsg> list = new ArrayList<>();
                while (count < accountCount) {
                    int toIndexExclude = Math.min(accountCount, count + 100);

                    ImportAccountSpec splitSpec = new ImportAccountSpec();
                    splitSpec.setSourceUuid(importSpec.getSourceUuid());
                    splitSpec.setSourceType(importSpec.getSourceType());
                    splitSpec.setAccountList(importSpec.getAccountList().subList(count, toIndexExclude));
                    splitSpec.setSyncCreateStrategy(importSpec.getSyncCreateStrategy());
                    splitSpec.setSyncUpdateStrategies(importSpec.getSyncUpdateStrategies());

                    ImportThirdPartyAccountMsg msg = new ImportThirdPartyAccountMsg();
                    msg.setSpec(splitSpec);
                    list.add(msg);

                    count += 100;
                }
                return list;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "clean-stale-ldap-entry";

            @Override
            public boolean skip(Map data) {
                return taskSpec.getDeleteAccountStrategy() == SyncDeletedAccountStrategy.NoAction ||
                        progress.getCleanStage().getTotal() == 0;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                long totalSize = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                        .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, importSpec.getSourceUuid())
                        .count();

                Map<String, String> credentialsAccountMap = new HashMap<>(((int) totalSize) << 1);

                SQL.New("select " +
                            "ref.credentials, ref.accountUuid " +
                        "from " +
                            "AccountThirdPartyAccountSourceRefVO ref " +
                        "where " +
                            "accountSourceUuid = (:ldapUuid)", Tuple.class)
                        .param("ldapUuid", importSpec.getSourceUuid())
                        .limit(100)
                        .paginate(totalSize, (List<Tuple> tuples) -> {
                            for (Tuple tuple : tuples) {
                                credentialsAccountMap.put(tuple.get(0, String.class), tuple.get(1, String.class));
                            }
                        });

                final Set<String> credentials =
                        transformToSet(importSpec.accountList, ImportAccountItem::getCredentials);
                credentials.forEach(credentialsAccountMap::remove);
                progress.appendSkipCountInCleanStage(credentials.size());

                if (credentialsAccountMap.isEmpty()) {
                    trigger.next();
                    return;
                }
                printUnbindingList(credentialsAccountMap);

                AtomicBoolean anySuccess = new AtomicBoolean(false);
                new While<>(splitMessages(credentialsAccountMap))
                        .enableProgressReport("unbind-third-party-accounts")
                        .each((msg, whileCompletion) -> {
                    bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            recordUnbindReply(msg, reply);
                            if (reply.isSuccess()) {
                                anySuccess.set(true);
                            } else {
                                whileCompletion.addError(reply.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!anySuccess.get()) {
                            trigger.fail(err(LDAP_SYNC_ERROR,
                                    "all ldap account unbinding attempt is failed. ldapServerUuid=%s",
                                    importSpec.getSourceUuid())
                                    .withCause(errorCodeList));
                            return;
                        } else if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn("ldap account unbinding occur some errors: " +
                                    errorCodeList.getCauses().get(0).getDetails());
                        }
                        trigger.next();
                    }
                });
            }

            private void printUnbindingList(Map<String, String> credentialsAccountMapNeedDelete) {
                StringBuilder builder = new StringBuilder(128 + credentialsAccountMapNeedDelete.size() << 7);
                builder.append(String.format(
                        "LDAP[uuid=%s, deleteStrategy=%s] unbinding account list below:%n",
                        importSpec.getSourceUuid(), taskSpec.getDeleteAccountStrategy()));
                for (Map.Entry<String, String> entry : credentialsAccountMapNeedDelete.entrySet()) {
                    builder.append(String.format("\taccountUuid=%s, credentials=%s%n", entry.getValue(), entry.getKey()));
                }
                logger.info(builder.toString());
            }

            private List<UnbindThirdPartyAccountMsg> splitMessages(Map<String, String> credentialsAccountMapNeedDelete) {
                int accountCount = credentialsAccountMapNeedDelete.size();

                if (accountCount <= 100) {
                    UnbindThirdPartyAccountsSpec spec = new UnbindThirdPartyAccountsSpec();
                    spec.setSourceUuid(importSpec.getSourceUuid());
                    spec.setSourceType(importSpec.getSourceType());
                    spec.setAccountUuidList(new ArrayList<>(credentialsAccountMapNeedDelete.values()));
                    spec.setSyncDeleteStrategy(taskSpec.deleteAccountStrategy);

                    UnbindThirdPartyAccountMsg msg = new UnbindThirdPartyAccountMsg();
                    msg.setSpec(spec);
                    return list(msg);
                }

                int count = 0;
                List<UnbindThirdPartyAccountMsg> list = new ArrayList<>();
                List<Map.Entry<String, String>> entries = new ArrayList<>(credentialsAccountMapNeedDelete.entrySet());

                while (count < accountCount) {
                    int toIndexExclude = Math.min(accountCount, count + 100);

                    UnbindThirdPartyAccountsSpec splitSpec = new UnbindThirdPartyAccountsSpec();
                    splitSpec.setSourceUuid(importSpec.getSourceUuid());
                    splitSpec.setSourceType(importSpec.getSourceType());
                    splitSpec.setSyncDeleteStrategy(taskSpec.deleteAccountStrategy);

                    Collection<String> accountUuids = transform(entries.subList(count, toIndexExclude), Map.Entry::getValue);
                    splitSpec.setAccountUuidList(new ArrayList<>(accountUuids));

                    UnbindThirdPartyAccountMsg msg = new UnbindThirdPartyAccountMsg();
                    msg.setSpec(splitSpec);
                    list.add(msg);

                    count += 100;
                }
                return list;
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(progress);
            }
        }).start();
    }

    private ImportAccountItem generateAccountSpec(LdapEntryInventory ldapEntry) {
        ImportAccountItem account = new ImportAccountItem();

        List<LdapEntryAttributeInventory> attributes = ldapEntry.getAttributes();
        String usernameProperty = taskSpec.getUsernameProperty();
        LdapEntryAttributeInventory usernameAttribute = findOneOrNull(attributes,
                attribute -> Objects.equals(attribute.getId(), usernameProperty));

        String dn = ldapEntry.getDn(); // entryDN
        final String username;
        if (usernameAttribute == null || usernameAttribute.getValues().isEmpty()) {
            username = dn;
        } else {
            Object value = usernameAttribute.getValues().get(0);
            username = value == null ? dn : value.toString();
        }

        account.setCredentials(dn);
        account.setAccountType(AccountType.Normal);
        account.setUsername(username);
        account.setEnable(ldapEntry.isEnable());
        return account;
    }

    private String buildFilter() {
        AndFilter filter = new AndFilter();
        if (taskSpec.getFilter() != null) {
            filter.and(new HardcodedFilter(taskSpec.getFilter()));
        }
        return filter.and(new HardcodedFilter(DEFAULT_PERSON_FILTER)).toString();
    }

    private String[] buildReturnAttribute() {
        Set<String> attributeSet = new HashSet<>(Arrays.asList(LdapConstant.QUERY_LDAP_ENTRY_MUST_RETURN_ATTRIBUTES));
        attributeSet.add(findGlobalUuidKey());
        if (StringUtils.isNotEmpty(taskSpec.getUsernameProperty())) {
            attributeSet.add(taskSpec.getUsernameProperty());
        }
        return attributeSet.toArray(new String[0]);
    }

    public String findGlobalUuidKey() {
        switch (taskSpec.getServerType()) {
        case OpenLdap:
            return LdapConstant.OpenLdap.GLOBAL_UUID_KEY;
        case WindowsAD: default:
            return LdapConstant.WindowsAD.GLOBAL_UUID_KEY;
        }
    }

    private void recordImportReply(ImportThirdPartyAccountMsg message, MessageReply rawReply) {
        int totalRecordExpect = message.getSpec().accountList.size();
        if (!rawReply.isSuccess() || !(rawReply instanceof ImportThirdPartyAccountReply)) {
            progress.appendFailCountInImportStage(totalRecordExpect);
            return;
        }

        ImportThirdPartyAccountReply reply = rawReply.castReply();
        if (CollectionUtils.isEmpty(reply.getResults())) {
            progress.appendFailCountInImportStage(totalRecordExpect);
            return;
        }

        final List<ImportAccountResult> successResults =
                filter(reply.getResults(), result -> result.getError() == null && result.getRef() != null);
        if (successResults.size() >= totalRecordExpect) {
            progress.appendSuccessCountInImportStage(totalRecordExpect);
        } else {
            progress.appendSuccessCountInImportStage(successResults.size());
            progress.appendFailCountInImportStage(totalRecordExpect - successResults.size());
        }
    }

    private void recordUnbindReply(UnbindThirdPartyAccountMsg message, MessageReply rawReply) {
        int totalRecordExpect = message.getSpec().getAccountUuidList().size();
        if (!rawReply.isSuccess() || !(rawReply instanceof UnbindThirdPartyAccountReply)) {
            progress.appendFailCountInCleanStage(totalRecordExpect);
            return;
        }

        UnbindThirdPartyAccountReply reply = rawReply.castReply();
        if (CollectionUtils.isEmpty(reply.getResults())) {
            progress.appendFailCountInCleanStage(totalRecordExpect);
            return;
        }

        final List<UnbindThirdPartyAccountResult> successResults =
                filter(reply.getResults(), result -> result.getError() == null);
        if (successResults.size() >= totalRecordExpect) {
            progress.appendSuccessCountInCleanStage(totalRecordExpect);
        } else {
            progress.appendSuccessCountInCleanStage(successResults.size());
            progress.appendFailCountInCleanStage(totalRecordExpect - successResults.size());
        }
    }
}