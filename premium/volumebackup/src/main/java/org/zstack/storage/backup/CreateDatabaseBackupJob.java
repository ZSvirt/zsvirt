package org.zstack.storage.backup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.CascadeUpdate;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.scheduler.SchedulerCanonicalEvents;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.*;
import org.zstack.longjob.LongJobManager;
import org.zstack.mevoco.MevocoGlobalProperty;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateDatabaseBackupJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CreateDatabaseBackupJob.class);

    @Autowired
    private transient DatabaseFacade dbf;
    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient ThreadFacade thdf;
    @Autowired
    private transient EventFacade evtf;
    @Autowired
    private transient LongJobManager longJobMgr;
    @Autowired
    private transient PluginRegistry pluginRgty;

    private BackupRetentionPolicy retentionPolicy;
    private BackupRetentionPolicy remoteRetentionPolicy;
    @CascadeUpdate(resourceType = BackupStorageVO.class, disableWhenEmpty = true)
    private List<String> backupStorageUuids;
    @CascadeUpdate(resourceType = BackupStorageVO.class)
    private String remoteBackupStorageUuid;

    public CreateDatabaseBackupJob() {
        super();
    }

    @Override
    public DatabaseBackupLongJobParams buildRequest() {
        return buildParams();
    }

    @Override
    public void execute(Object request, ReturnValueCompletion completion) {
        backupStorageUuids = backupStorageUuids.stream().filter(bsUuid -> Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, bsUuid).isExists()).collect(Collectors.toList());
        if (backupStorageUuids.isEmpty()) {
            ErrorCode err = operr("No available backup storage found, skip this job");
            fireEvent(err);
            completion.fail(err);
            return;
        }

        DatabaseBackupLongJobParams params = (DatabaseBackupLongJobParams) request;
        SubmitLongJobMsg msg = buildMsg(params);
        longJobMgr.submitLongJob(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                }
            }
        }, event -> {
            if (event.isSuccess()) {
                APICreateDatabaseBackupEvent evt = (APICreateDatabaseBackupEvent) event;
                String bsUuid = evt.getInventory().getBackupStorageRefs().get(0).getBackupStorageUuid();
                logger.info(String.format("database backup succeeded, bsUuid = %s", bsUuid));
                doRetention(retentionPolicy, backupStorageUuids);
                doSyncToRemote(bsUuid, evt.getInventory().getUuid(), new Completion(null) {
                    @Override
                    public void success() {
                        if (needRemoteRetention()) {
                            doRetention(remoteRetentionPolicy, Collections.singletonList(remoteBackupStorageUuid));
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                    }
                });

            }

            fireEvent(event);
            completion.success(event);
        });
    }

    private boolean needRemoteRetention() {
        return remoteBackupStorageUuid != null && remoteRetentionPolicy != null;
    }

    private void deleteDatabaseBackups(List<String> backupUuids, List<String> backupStorageUuids) {
        List<DatabaseBackupDeletionMsg> dmsgs = backupUuids.stream()
                .map(backupUuid -> {
                    DatabaseBackupDeletionMsg msg = new DatabaseBackupDeletionMsg();
                    msg.setUuid(backupUuid);
                    msg.setBackupStorageUuids(backupStorageUuids);
                    bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, backupUuid);
                    return msg;
                }).collect(Collectors.toList());
        bus.send(dmsgs);
    }

    @ExceptionSafe
    private void cleanBackupByDays(long days, List<String> backupStorageUuids) {
        if (days < 1) {
            return;
        }

        LocalDateTime time = LocalDateTime.now().minusDays(days);

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> backupUuids = q(DatabaseBackupVO.class)
                        .eq(DatabaseBackupVO_.status, DatabaseBackupStatus.Ready)
                        .lt(DatabaseBackupVO_.createDate, Timestamp.valueOf(time))
                        .select(DatabaseBackupVO_.uuid)
                        .listValues();

                backupUuids.removeAll(DatabaseBackupSystemTag.MANUAL_CREATE_RESOURCE.filterResourceHasTag(backupUuids));
                if (!backupUuids.isEmpty()) {
                    deleteDatabaseBackups(backupUuids, backupStorageUuids);
                }
            }
        }.execute();
    }

    @ExceptionSafe
    private void cleanBackupByCount(long count, List<String> backupStorageUuids) {
        if (count < 1) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> backupUuids = q(DatabaseBackupVO.class)
                        .eq(DatabaseBackupVO_.status, DatabaseBackupStatus.Ready)
                        .orderBy(DatabaseBackupVO_.createDate, SimpleQuery.Od.ASC)
                        .select(DatabaseBackupVO_.uuid)
                        .listValues();

                backupUuids.removeAll(DatabaseBackupSystemTag.MANUAL_CREATE_RESOURCE.filterResourceHasTag(backupUuids));
                long offset = backupUuids.size() - count;
                if (offset > 0) {
                    deleteDatabaseBackups(backupUuids.subList(0, (int)offset), backupStorageUuids);
                }
            }
        }.execute();
    }

    private void doRetention(BackupRetentionPolicy backupRetentionPolicy, List<String> backupStorageUuids) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "retention-backup-" + getTargetResourceUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                switch (backupRetentionPolicy.getRetentionType()) {
                    case Days:
                        cleanBackupByDays(backupRetentionPolicy.getRetentionValue(), backupStorageUuids);
                        break;
                    case Count:
                        cleanBackupByCount(backupRetentionPolicy.getRetentionValue(), backupStorageUuids);
                        break;
                }

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @ExceptionSafe
    private void doSyncToRemote(final String bsUuid, final String backupUuid, Completion completion) {
        if (StringUtils.isEmpty(remoteBackupStorageUuid) || !dbf.isExist(remoteBackupStorageUuid, BackupStorageVO.class)) {
            return;
        }

        SyncDatabaseBackupFromImageStoreBackupStorageMsg msg = new SyncDatabaseBackupFromImageStoreBackupStorageMsg();
        msg.setUuid(backupUuid);
        msg.setSrcBackupStorageUuid(bsUuid);
        msg.setDstBackupStorageUuid(remoteBackupStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, bsUuid);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }


    private DatabaseBackupLongJobParams buildParams() {
        DatabaseBackupLongJobParams params = new DatabaseBackupLongJobParams();
        DatabaseBackupVersionExtensionPoint ext = pluginRgty.getExtensionFromMap(
                MevocoGlobalProperty.DEPLOY_MODE, DatabaseBackupVersionExtensionPoint.class);
        String version = ext == null ? dbf.getDbVersion() : ext.getVersion(dbf.getDbVersion());
        params.setName(DatabaseBackupConstant.buildDatabaseBackupName(version));
        params.setDescription(String.format("backup by scheduled job[uuid:%s]", getUuid()));
        params.setAlternativeBackupStorageUuids(backupStorageUuids);
        return params;
    }

    private SubmitLongJobMsg buildMsg(DatabaseBackupLongJobParams params) {
        SubmitLongJobMsg msg = new SubmitLongJobMsg();
        msg.setName(params.getName());
        msg.setDescription(String.format("submitted by database backup scheduler job[uuid:%s]", getUuid()));
        msg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        msg.setJobData(JSONObjectUtil.toJsonString(params));
        msg.setJobName(APICreateDatabaseBackupMsg.class.getSimpleName());
        return msg;
    }

    private void mergeFields(Map<String, String> parameters) {
        mergeBackupPolicy(parameters);
        validateRemoteRetentionPolicyParam(parameters);
        mergeRetentionPolicy(parameters);
        validateParam();
    }

    public CreateDatabaseBackupJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
        mergeFields(msg.getParameters());
    }

    @Override
    public String getType() {
        return SchedulerType.DATABASE_BACKUP;
    }

    @Override
    public void updateSchedulerJob(Map<String, String> parameters) {
        super.updateSchedulerJob(parameters);
        mergeFields(parameters);
    }

    @Override
    public boolean lastJobIsRunning() {
        LongJobState longJobState = Q.New(LongJobVO.class)
                .like(LongJobVO_.name, String.format("%s-%%-%%.gz", BackupConstant.DB_BACKUP_PREFIX))
                .orderBy(LongJobVO_.createDate, SimpleQuery.Od.DESC)
                .limit(1).select(LongJobVO_.state).findValue();
        return Objects.equals(longJobState, LongJobState.Running);
    }

    private void mergeRetentionPolicy(Map<String, String> params) {
        if (retentionPolicy == null) {
            retentionPolicy = new BackupRetentionPolicy();
        }

        Optional.ofNullable(params.get("retentionType")).ifPresent(it ->
                retentionPolicy.setRetentionType(StringUtils.isBlank(it) ? null : BackupRetentionType.valueOf(it)));
        Optional.ofNullable(params.get("retentionValue")).ifPresent(it ->
                retentionPolicy.setRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));

        if (remoteRetentionPolicy == null) {
            remoteRetentionPolicy = new BackupRetentionPolicy();
        }
        Optional.ofNullable(params.get("remoteRetentionType")).ifPresent(it ->
                remoteRetentionPolicy.setRetentionType(StringUtils.isBlank(it) ? null : BackupRetentionType.valueOf(it)));
        Optional.ofNullable(params.get("remoteRetentionValue")).ifPresent(it ->
                remoteRetentionPolicy.setRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));

        if (isUpdateRemoteRetentionPolicyPermanent(remoteRetentionPolicy) || remoteBackupStorageUuid == null) {
            remoteRetentionPolicy = null;
        }
    }

    private static boolean isUpdateRemoteRetentionPolicyPermanent(BackupRetentionPolicy remoteRetentionPolicy) {
        return remoteRetentionPolicy.getRetentionType() == null || remoteRetentionPolicy.getRetentionValue() == 0;
    }

    private void mergeBackupPolicy(Map<String, String> params) {
        Optional.ofNullable(params.get("remoteBackupStorageUuid")).ifPresent(it ->
                this.remoteBackupStorageUuid = StringUtils.isBlank(it) ? null : it);
        Optional.ofNullable(params.get("backupStorageUuids")).ifPresent(it ->
                this.backupStorageUuids = Arrays.asList(StringUtils.split(it.replace("\\s", ""),",")));
    }

    private void validateParam() {
        if (retentionPolicy.getRetentionType() == null) {
            throw new OperationFailureException(Platform.operr("missing 'retentionType' in job parameters"));
        }

        if (retentionPolicy.getRetentionValue() == 0) {
            throw new OperationFailureException(Platform.operr("missing 'retentionValue' in job parameters"));
        }

        if (backupStorageUuids == null) {
            throw new OperationFailureException(Platform.operr("missing 'backupStorageUuids' in job parameters"));
        }

        if (backupStorageUuids.isEmpty()) {
            throw new OperationFailureException(Platform.operr("job parameter 'backupStorageUuids' is empty"));
        }

        for (String bsUuid: backupStorageUuids) {
            if (!Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, bsUuid).isExists()) {
                throw new OperationFailureException(Platform.operr("unexpected backup storage uuid: %s", bsUuid));
            }
        }
    }

    private void validateRemoteRetentionPolicyParam(Map<String, String> params) {
        if (StringUtils.isBlank(params.get("remoteRetentionType")) && !StringUtils.isBlank(params.get("remoteRetentionValue"))) {
            throw new OperationFailureException(Platform.operr("missing 'remoteRetentionType' in job parameters"));
        } else if (StringUtils.isBlank(params.get("remoteRetentionValue")) && !StringUtils.isBlank(params.get("remoteRetentionType"))) {
            throw new OperationFailureException(Platform.operr("missing 'remoteRetentionValue' in job parameters"));
        }
    }

    private void fireEvent(ErrorCode errorCode) {
        APIEvent event = new APIEvent();
        event.setError(errorCode);
        fireEvent(event);
    }

    protected void fireEvent(APIEvent event) {
        SchedulerCanonicalEvents.SchedulerExecutedData data = new SchedulerCanonicalEvents.SchedulerExecutedData();
        data.setTargetResourceUuid(getTargetResourceUuid());
        data.setSchedulerName(getName());
        data.setJobUuid(getUuid());
        if (event.isSuccess()) {
            APICreateDatabaseBackupEvent evt = (APICreateDatabaseBackupEvent) event;
            String bsUuid = evt.getInventory().getBackupStorageRefs().get(0).getBackupStorageUuid();
            data.setResultMessage(String.format("database backup succeeded, bsUuid = %s", bsUuid));
        } else {
            data.setError(event.getError());
            data.setResultMessage(String.format("database backup failed, bsUuids: %s, error: %s",
                    backupStorageUuids, event.getError().getDetails()));
        }

        evtf.fire(SchedulerCanonicalEvents.DATABASE_BACKUP_SCHEDULER_PATH, data);
    }
}
