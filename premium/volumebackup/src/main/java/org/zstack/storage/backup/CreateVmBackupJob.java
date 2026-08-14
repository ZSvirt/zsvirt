package org.zstack.storage.backup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.AccountManager;
import org.zstack.longjob.LongJobManager;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerConstant;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVmBackupJob extends AbstractSchedulerJob<VmBackupLongJobParams, APIEvent> {
    private static final CLogger logger = Utils.getLogger(CreateVmBackupJob.class);

    @Autowired
    private transient AccountManager acntMgr;
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
    private transient BackupErrorHandler errorHandler;

    private BackupRetentionPolicy retentionPolicy;
    private BackupRetentionPolicy remoteRetentionPolicy;
    @CascadeUpdate(resourceType = BackupStorageVO.class, disableWhenEmpty = true)
    private List<String> backupStorageUuids;
    @CascadeUpdate(resourceType = BackupStorageVO.class)
    private String remoteBackupStorageUuid;
    private BackupQosStruct backupQosStruct;
    private String fullBackupTriggerUuid;

    private static List<Field> qosFields = FieldUtils.getAllFields(BackupQosStruct.class);

    @Override
    public String getType() {
        return SchedulerType.VM_BACKUP;
    }

    @Override
    public void updateSchedulerJob(Map<String, String> jobParameters) {
        super.updateSchedulerJob(jobParameters);
        mergeFields(jobParameters);
    }

    @Override
    public boolean lastJobIsRunning() {
        LongJobState longJobState = Q.New(LongJobVO.class)
                .eq(LongJobVO_.name, VmBackupTracker.TASK_NAME)
                .eq(LongJobVO_.targetResourceUuid, getTargetResourceUuid())
                .orderBy(LongJobVO_.createDate, SimpleQuery.Od.DESC)
                .limit(1).select(LongJobVO_.state).findValue();
        return Objects.equals(longJobState, LongJobState.Running);
    }

    @Override
    public String getZoneUuid() {
        String sql = "select vm.zoneUuid from VolumeVO vol, VmInstanceVO vm, SchedulerJobVO job " +
                "where vol.uuid = vm.rootVolumeUuid and vol.uuid = job.targetResourceUuid and job.uuid = :uuid";
        return SQL.New(sql, String.class).param("uuid", getUuid()).find();
    }

    private void mergeQosStruct(Map<String, String> params) {
        if (backupQosStruct == null) {
            backupQosStruct = new BackupQosStruct();
        }

        qosFields.forEach(field -> {
            String value = params.get(field.getName());
            if (value != null) {
                field.setAccessible(true);
                Long qos = StringUtils.isBlank(value) ? null : validateBandWidth(Long.valueOf(value));
                try {
                    field.set(backupQosStruct, qos);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });
    }

    private long validateBandWidth(long value) {
        if (value <= 0) {
            throw new OperationFailureException(argerr("bandWidth must be a positive number"));
        }

        if (value < 1024L) {
            logger.warn(String .format("bandWidth[%s] cannot be lower than 1024 Bps, set it to 1024", value));
            return 1024L;
        }
        return value;
    }

    private void mergeRetentionPolicy(Map<String, String> params) {
        if (retentionPolicy == null) {
            retentionPolicy = new BackupRetentionPolicy();
        }

        Optional.ofNullable(params.get("retentionType")).ifPresent(it ->
                retentionPolicy.setRetentionType(StringUtils.isBlank(it) ? null : BackupRetentionType.valueOf(it)));
        Optional.ofNullable(params.get("retentionValue")).ifPresent(it ->
                retentionPolicy.setRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));
        Optional.ofNullable(params.get("fullBackupRetentionValue")).ifPresent(it ->
                retentionPolicy.setFullBackupRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));

        if (remoteRetentionPolicy == null) {
            remoteRetentionPolicy = new BackupRetentionPolicy();
        }
        Optional.ofNullable(params.get("remoteRetentionType")).ifPresent(it ->
                remoteRetentionPolicy.setRetentionType(StringUtils.isBlank(it) ? null : BackupRetentionType.valueOf(it)));
        Optional.ofNullable(params.get("remoteRetentionValue")).ifPresent(it ->
                remoteRetentionPolicy.setRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));
        Optional.ofNullable(params.get("remoteFullBackupRetentionValue")).ifPresent(it ->
                remoteRetentionPolicy.setFullBackupRetentionValue(StringUtils.isBlank(it) ? 0 : Long.valueOf(it)));

        if (isUpdateRemoteRetentionPolicyPermanent(remoteRetentionPolicy) || remoteBackupStorageUuid == null) {
            remoteRetentionPolicy = null;
        }
    }

    private static boolean isUpdateRemoteRetentionPolicyPermanent(BackupRetentionPolicy remoteRetentionPolicy) {
        return remoteRetentionPolicy.getRetentionType() == null || remoteRetentionPolicy.getRetentionValue() == 0;
    }

    private void mergeBackupPolicy(Map<String, String> params) {
        Optional.ofNullable(params.get("fullBackupTriggerUuid")).ifPresent(it ->
                this.fullBackupTriggerUuid = StringUtils.isBlank(it) ? null : it);
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

    private void mergeFields(Map<String, String> parameters) {
        if (parameters == null) {
            throw new OperationFailureException(Platform.operr("missing job parameters"));
        }
        mergeBackupPolicy(parameters);
        validateRemoteRetentionPolicyParam(parameters);
        mergeRetentionPolicy(parameters);
        mergeQosStruct(parameters);
        validateParam();
    }

    public CreateVmBackupJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
        mergeFields(msg.getParameters());
    }

    public CreateVmBackupJob() {
        super();
    }

    public BackupRetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(BackupRetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }

    public BackupRetentionPolicy getRemoteRetentionPolicy() {
        return remoteRetentionPolicy;
    }

    public void setRemoteRetentionPolicy(BackupRetentionPolicy remoteRetentionPolicy) {
        this.remoteRetentionPolicy = remoteRetentionPolicy;
    }

    public BackupQosStruct getBackupQosStruct() {
        return backupQosStruct;
    }

    public void setBackupQosStruct(BackupQosStruct backupQosStruct) {
        this.backupQosStruct = backupQosStruct;
    }

    private VmBackupLongJobParams buildParams() {
        VmBackupLongJobParams params = new VmBackupLongJobParams();
        String vmName = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.rootVolumeUuid, getTargetResourceUuid()).
                select(VmInstanceVO_.name).findValue();
        params.setName(vmName);
        params.setDescription(String.format("created by VM backup scheduler job[uuid:%s]", getUuid()));
        params.setRootVolumeUuid(getTargetResourceUuid());
        params.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(getUuid()));
        params.setAlternativeBackupStorageUuids(backupStorageUuids);
        params.setBackupQosStruct(backupQosStruct);
        params.setMode(getBackupMode());
        return params;
    }

    private SubmitLongJobMsg buildMsg(VmBackupLongJobParams params) {
        SubmitLongJobMsg msg = new SubmitLongJobMsg();
        msg.setName(VmBackupTracker.TASK_NAME);
        msg.setDescription(String.format("submitted by VM backup scheduler job[uuid:%s]", getUuid()));
        msg.setJobData(JSONObjectUtil.toJsonString(params));
        msg.setAccountUuid(params.getAccountUuid());
        msg.setJobName(APICreateVmBackupMsg.class.getSimpleName());
        msg.setTargetResourceUuid(params.getRootVolumeUuid());
        return msg;
    }

    BackupMode getBackupMode() {
        if (getTriggerUuid() != null && getTriggerUuid().equals(fullBackupTriggerUuid)) {
            return BackupMode.full;
        }

        return BackupMode.auto;
    }

    private void deleteVolumeBackups(List<String> backupUuids, String volumeUuid, List<String> backupStorageUuids) {
        List<VolumeBackupDeletionMsg> dmsgs = backupUuids.stream()
                .map(backupUuid -> {
                    VolumeBackupDeletionMsg msg = new VolumeBackupDeletionMsg();
                    msg.setUuid(backupUuid);
                    msg.setBackupStorageUuids(backupStorageUuids);
                    msg.setHandleDependency(true);
                    bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, volumeUuid);
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
                List<String> backupUuids = q(VolumeBackupVO.class)
                        .eq(VolumeBackupVO_.volumeUuid, getTargetResourceUuid())
                        .eq(VolumeBackupVO_.status, VolumeBackupStatus.Ready)
                        .eq(VolumeBackupVO_.type, VolumeType.Root)
                        .notNull(VolumeBackupVO_.groupUuid)
                        .lt(VolumeBackupVO_.createDate, Timestamp.valueOf(time))
                        .select(VolumeBackupVO_.uuid)
                        .listValues();
                backupUuids.removeAll(VolumeBackupSystemTag.MANUAL_CREATE_RESOURCE.filterResourceHasTag(backupUuids));
                if (!backupUuids.isEmpty()) {
                    findSameGroupBackupByVolume(backupUuids).forEach((volUuid, backUuids) -> {
                        deleteVolumeBackups(backUuids, volUuid, backupStorageUuids);
                    });
                }
            }
        }.execute();
    }

    @ExceptionSafe
    private void cleanBackupByCount(long count, long fullCount, List<String> backupStorageUuids) {
        if (count < 0 || fullCount < 0) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<Tuple> backupTuples = q(VolumeBackupVO.class)
                        .eq(VolumeBackupVO_.volumeUuid, getTargetResourceUuid())
                        .eq(VolumeBackupVO_.type, VolumeType.Root)
                        .eq(VolumeBackupVO_.status, VolumeBackupStatus.Ready)
                        .notNull(VolumeBackupVO_.groupUuid)
                        .orderBy(VolumeBackupVO_.createDate, SimpleQuery.Od.ASC)
                        .select(VolumeBackupVO_.uuid, VolumeBackupVO_.mode)
                        .listTuple();

                List<String> allBackupUuids = backupTuples.stream()
                        .map(t -> t.get(0, String.class)).collect(Collectors.toList());

                List<String> incrementalBackupUuids = backupTuples.stream()
                        .filter(t -> t.get(1, BackupMode.class) != BackupMode.full)
                        .map(t -> t.get(0, String.class)).collect(Collectors.toList());

                List<String> fullBackupUuids = backupTuples.stream()
                        .filter(t -> t.get(1, BackupMode.class) == BackupMode.full)
                        .map(t -> t.get(0, String.class)).collect(Collectors.toList());

                List<String> ManualVolumeBackupUuids = VolumeBackupSystemTag
                        .MANUAL_CREATE_RESOURCE.filterResourceHasTag(allBackupUuids);
                allBackupUuids.removeAll(ManualVolumeBackupUuids);
                incrementalBackupUuids.removeAll(ManualVolumeBackupUuids);
                fullBackupUuids.removeAll(ManualVolumeBackupUuids);

                List<String> backupUuidsToDelete = new ArrayList<>();
                if (fullCount == 0) {
                    backupUuidsToDelete.addAll(getBackupUuidsToDelete(count, allBackupUuids));
                } else {
                    backupUuidsToDelete.addAll(getBackupUuidsToDelete(count, incrementalBackupUuids));
                    backupUuidsToDelete.addAll(getBackupUuidsToDelete(fullCount, fullBackupUuids));
                }
                doDeleteVolumeBackups(backupUuidsToDelete, backupStorageUuids);
            }
        }.execute();
    }

    private List<String> getBackupUuidsToDelete(long count, List<String> backupUuids) {
        long offset = backupUuids.size() - count;
        if (offset > 0) {
            return backupUuids.subList(0, (int) offset);
        }
        return new ArrayList<>();
    }

    private void doDeleteVolumeBackups(List<String> backupUuids, List<String> backupStorageUuids) {
        if (CollectionUtils.isEmpty(backupUuids)) {
            return;
        }

        findSameGroupBackupByVolume(backupUuids).forEach((volUuid, backUuids) ->
                deleteVolumeBackups(backUuids, volUuid, backupStorageUuids));
    }

    private Map<String, List<String>> findSameGroupBackupByVolume(List<String> backupUuids) {
        List<Tuple> ts = SQL.New("select vb.volumeUuid, vb.uuid from VolumeBackupVO vb" +
                " where vb.groupUuid in" +
                " (" +
                " select v.groupUuid from VolumeBackupVO v" +
                " where v.uuid in (:uuids)" +
                " )", Tuple.class).param("uuids", backupUuids)
                .list();
        return ts.stream().collect(Collectors.groupingBy(t -> ((Tuple) t).get(0, String.class),
                Collectors.mapping(t -> ((Tuple)t).get(1, String.class), Collectors.toList())));
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
                        cleanBackupByCount(backupRetentionPolicy.getRetentionValue(),
                                backupRetentionPolicy.getFullBackupRetentionValue(), backupStorageUuids);
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
    private void doSyncToRemote(final String bsUuid, final List<VolumeBackupInventory> backups, Completion completion) {
        if (StringUtils.isEmpty(remoteBackupStorageUuid) || !dbf.isExist(remoteBackupStorageUuid, BackupStorageVO.class)) {
            return;
        }

        List<SyncBackupFromImageStoreBackupStorageMsg> msgs = new ArrayList<>();
        for (VolumeBackupInventory inv: backups) {
            SyncBackupFromImageStoreBackupStorageMsg msg = new SyncBackupFromImageStoreBackupStorageMsg();
            msg.setUuid(inv.getUuid());
            msg.setSrcBackupStorageUuid(bsUuid);
            msg.setDstBackupStorageUuid(remoteBackupStorageUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, msg.getBackupStorageUuid());
            msgs.add(msg);
        }

        List<ErrorCode> errorCodes = new ArrayList<>();
        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync backup from imageStoreBackupStorage[%s], %s", bsUuid, reply.getError()));
                    errorCodes.add(reply.getError());
                }
                com.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodes.isEmpty()) {
                    completion.fail(errorCodes.get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    private ErrorCode check() {
        ErrorCode err;
        if ((err = checkVolumeVmState()) != null) {
            return err;
        } else if ((err = checkBackupStorage()) != null) {
            return err;
        }

        return null;
    }

    private ErrorCode checkVolumeVmState() {
        boolean notAllow = true;

        Tuple t = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.isShareable, VolumeVO_.type)
                .eq(VolumeVO_.uuid, getTargetResourceUuid())
                .findTuple();
        String vmUuid = (String) t.get(0);
        boolean isShareable = (boolean) t.get(1);
        VolumeType type = (VolumeType) t.get(2);

        StringBuilder errorStringBuilder = new StringBuilder();
        String wetherFullBackupInfo = getBackupMode() == BackupMode.full ? " full" : "";
        errorStringBuilder.append(String.format("Can not create volume%s backup for volume[uuid:%s], because ",
                wetherFullBackupInfo, getTargetResourceUuid()));

        if (isShareable) {
            errorStringBuilder.append("it is shareable volume");
        } else if (vmUuid == null) {
            errorStringBuilder.append("it is not attached to any vm");
        } else if (!Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused))
                .eq(VmInstanceVO_.uuid, vmUuid)
                .isExists()) {
            if (type.equals(VolumeType.Root)) {
                errorStringBuilder.append(String.format("vm is not in state[%s, %s]", VmInstanceState.Running, VmInstanceState.Paused));
            } else {
                errorStringBuilder.append(String.format("its attached vm is not in state[%s, %s]", VmInstanceState.Running, VmInstanceState.Paused));
            }
            return err(VolumeBackupErrors.VM_STOPPED, errorStringBuilder.toString());
        } else {
            notAllow = false;
        }

        if (notAllow) {
            return operr(errorStringBuilder.toString());
        } else {
            return null;
        }
    }

    private ErrorCode checkBackupStorage() {
        backupStorageUuids = Q.New(BackupStorageVO.class).select(BackupStorageVO_.uuid)
                .in(BackupStorageVO_.uuid, backupStorageUuids)
                .listValues();

        if (backupStorageUuids.isEmpty()) {
            return operr("No available backup storage found, skip this job");
        }
        return null;
    }

    @Override
    public VmBackupLongJobParams buildRequest() {
        return buildParams();
    }

    @Override
    public void execute(VmBackupLongJobParams request, ReturnValueCompletion<APIEvent> completion) {
        ErrorCode error = check();
        if (error != null) {
            fireEvent(error);
            errorHandler.handle(error, this);
            completion.fail(error);
            return;
        }

        SubmitLongJobMsg msg = buildMsg(request);
        VmBackupTracker tracker = new VmBackupTracker(getTargetResourceUuid());

        longJobMgr.submitLongJob(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    tracker.track(BackupProcess.starting);
                } else {
                    fireEvent(reply.getError());
                }
            }
        }, event -> {
            if (event.isSuccess()) {
                APICreateVmBackupEvent evt = (APICreateVmBackupEvent) event;
                String bsUuid = evt.getInventories().get(0).getBackupStorageRefs().get(0).getBackupStorageUuid();
                tracker.track(BackupProcess.success);
                doRetention(retentionPolicy, backupStorageUuids);
                doSyncToRemote(bsUuid, evt.getInventories(), new Completion(null) {
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
            } else {
                tracker.track(BackupProcess.failure);
                errorHandler.handle(event.getError(), this);
            }

            fireEvent(event);
            completion.success(event);
        });
    }

    private boolean needRemoteRetention() {
        return remoteBackupStorageUuid != null && remoteRetentionPolicy != null;
    }

    @Override
    public ErrorCode allowStateChange() {
        if (Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, getTargetResourceUuid())
                .eq(VolumeVO_.status, VolumeStatus.Deleted)
                .isExists()) {
            return operr("volume[uuid:%s] is deleted, state change is not allowed", getTargetResourceUuid());
        }

        return null;
    }

    private void fireEvent(ErrorCode err) {
        APIEvent evt = new APIEvent();
        evt.setError(err);
        fireEvent(evt);
    }

    @ExceptionSafe
    protected void fireEvent(APIEvent event) {
        String info;
        SchedulerCanonicalEvents.SchedulerExecutedData data = new SchedulerCanonicalEvents.SchedulerExecutedData();
        String vmUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid)
                .eq(VolumeVO_.uuid, getTargetResourceUuid())
                .findValue();

        if (event.isSuccess()) {
            APICreateVmBackupEvent evt = (APICreateVmBackupEvent) event;
            String wetherFullBackupInfo = evt.getInventories().get(0).getMode() == BackupMode.full ? " full" : "";
            String bsUuid = evt.getInventories().get(0).getBackupStorageRefs().get(0).getBackupStorageUuid();
            info = String.format("VM[uuid:%s]%s backup succeeded, bsUuid = %s",
                    vmUuid, wetherFullBackupInfo, bsUuid);
            logger.info(info);
        } else {
            String wetherFullBackupInfo = getBackupMode() == BackupMode.full ? " full" : "";
            info = String.format("VM[uuid:%s]%s backup failed, error: %s",
                    vmUuid, wetherFullBackupInfo, event.getError().getDetails());
            event.getError().setDetails(info);
            data.setError(event.getError());
            logger.error(info);
        }

        data.setTargetResourceUuid(getTargetResourceUuid());
        data.setSchedulerName(getName());
        data.setJobUuid(getUuid());
        data.setResultMessage(info);
        evtf.fire(SchedulerCanonicalEvents.VOLUME_BACKUP_SCHEDULER_PATH, data);
    }

    public String getFullBackupTriggerUuid() {
        return fullBackupTriggerUuid;
    }

    @Override
    public String getQueueName() {
        return SchedulerConstant.DISASTER_RECOVER_JOB_QUEUE;
    }

}
