package org.zstack.scheduler;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.quartz.TriggerKey.triggerKey;
import static org.zstack.core.Platform.*;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 8/3/16.
 */

public abstract class AbstractSchedulerJob<T, K> implements SchedulerJob {
    private static final transient CLogger logger = Utils.getLogger(AbstractSchedulerJob.class);

    @Autowired
    private transient SchedulerFacadeImpl schedulerFacade;
    @Autowired
    private transient ResourceDestinationMaker destMaker;
    @Autowired
    private transient DatabaseFacade dbf;
    @Autowired
    private transient CloudBus bus;

    private final static String mnStoppedResultDump;

    private String uuid;
    private String triggerUuid;
    private String name;
    private String description;
    private String targetResourceUuid;
    private Timestamp createDate;
    private Timestamp startTime;
    private String accountUuid;
    private String fireInstanceId;
    private SchedulerJobHistoryVO history;

    static {
        APIEvent recordEvent = new APIEvent();
        recordEvent.setError(err(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR,
                "management stopped, scheduler job may have failed"));
        mnStoppedResultDump = JSONObjectUtil.toJsonString(recordEvent);
    }

    public AbstractSchedulerJob(CreateSchedulerJobDescMsg msg) {
        Date date = new Date();
        createDate = new Timestamp(date.getTime());
        name = msg.getName();
        this.accountUuid = msg.getAccountUuid();

        if (msg.getDescription() != null && ! msg.getDescription().isEmpty()) {
            description = msg.getDescription();
        }
    }


    public AbstractSchedulerJob() {

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {

        this.createDate = createDate;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getTriggerUuid() {
        return triggerUuid;
    }

    @Override
    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    private void resourceCheck() {
        String error = new SQLBatchWithReturn<String>() {
            @Override
            @Transactional(readOnly = true)
            protected String scripts() {
                boolean inGroup = Q.New(SchedulerJobGroupJobRefVO.class)
                        .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, getUuid()).isExists();
                if (inGroup) {
                    return null;
                }

                String mnId = Q.New(SchedulerJobVO.class).select(SchedulerJobVO_.managementNodeUuid)
                        .eq(SchedulerJobVO_.uuid, getUuid())
                        .findValue();
                String expectMnId = destMaker.makeDestination(getUuid());
                if (!expectMnId.equals(mnId)) {
                    logger.error(String.format("job should belong to MN[id:%s], but run on MN[id:%s], " +
                            "Please report us ASAP!", expectMnId, mnId));
                }

                if (!getManagementServerId().equals(mnId)) {
                    return String.format("scheduler job[uuid:%s] not run on MN[id:%s] on record, unschedule self",
                            getUuid(), mnId);
                }

                boolean jobTriggerRefExists = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, getUuid())
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, getTriggerUuid())
                        .isExists();
                if (jobTriggerRefExists) {
                    return null;
                }
                return String.format("scheduler job[uuid:%s] has been removed from trigger[uuid:%s], unschedule self",
                        getUuid(), getTriggerUuid());
            }
        }.scripts();

        if (error == null) {
            return;
        }

        try {
            schedulerFacade.getScheduler().unscheduleJob(triggerKey(getTriggerUuid(), getTriggerUuid() + "." + getUuid()));
        } catch (SchedulerException e) {
            logger.debug("failed to unschedule scheduler job");
        }

        throw new OperationFailureException(inerr(error));
    }

    @Override
    public void run(NopeCompletion completion) {
        prepareJob();
        resourceCheck();
        checkLastJob();
        startTime = Timestamp.valueOf(LocalDateTime.now());
        T request = buildRequest();
        if (request == null) {
            return;
        }

        recordRequest(request);
        execute(request, new ReturnValueCompletion<K>(null) {
            @Override
            public void success(K returnValue) {
                if (returnValue instanceof MessageReply) {
                    recordResult((MessageReply) returnValue);
                } else if (returnValue instanceof APIEvent) {
                    recordResult((APIEvent) returnValue);
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APIEvent event = new APIEvent();
                event.setError(errorCode);
                recordResult(event);
                completion.success();
            }
        });
    }

    void checkLastJob() {
        if (!SchedulerGlobalConfig.LAST_JOB_UNCOMPLETED_SKIP_NEXT_JOB.value(Boolean.class)) {
            return;
        }
        if (lastJobIsRunning()) {
            throw new OperationFailureException(operr("the last job is not completed. skip this job"));
        }
    }

    @Transactional
    void prepareJob() {
        this.targetResourceUuid = Q.New(SchedulerJobVO.class).eq(SchedulerJobVO_.uuid, this.uuid)
                .select(SchedulerJobVO_.targetResourceUuid).findValue();
        this.accountUuid = ResourceHelper.findResourceOwner(this.uuid);
    }

    public abstract T buildRequest();

    public abstract void execute(T request, ReturnValueCompletion<K> completion);

    public ErrorCode allowStateChange() {
        return null;
    }


    private void recordRequest(Object request) {
        history = new SchedulerJobHistoryVO();
        history.setSchedulerJobUuid(uuid);
        history.setTriggerUuid(triggerUuid);
        history.setTargetResourceUuid(targetResourceUuid);
        history.setSchedulerJobGroupUuid(Q.New(SchedulerJobGroupJobRefVO.class).select(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, uuid).findValue());
        history.setJobType(getType());
        history.setStartTime(startTime);
        history.setExecuteTime(-1);
        history.setFireInstanceId(fireInstanceId);
        history.setRequestDump(JSONObjectUtil.toJsonString(request));
        history.setResultDump("Running");
        history = dbf.persistAndRefresh(history);
    }

    private void recordResult(APIEvent evt) {
        doRecordResult(evt, evt.isSuccess());
    }

    private void recordResult(MessageReply reply) {
        doRecordResult(reply, reply.isSuccess());
    }

    @ExceptionSafe
    private void doRecordResult(Message response, boolean success) {
        long executeTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime.getTime());

        DebugUtils.Assert(history != null, "must call recordRequest first");
        history.setExecuteTime(executeTime);
        history.setResultDump(JSONObjectUtil.toJsonString(response));
        history.setSuccess(success);
        history = dbf.updateAndRefresh(history);

        fireEventIfJobGroupOver();
    }

    static void cleanStaleResult(Collection<String> jobUuids) {
        SQL.New(SchedulerJobHistoryVO.class)
                .eq(SchedulerJobHistoryVO_.executeTime, -1L)
                .in(SchedulerJobHistoryVO_.schedulerJobUuid, jobUuids)
                .set(SchedulerJobHistoryVO_.executeTime, 0L)
                .set(SchedulerJobHistoryVO_.resultDump, mnStoppedResultDump)
                .update();
    }

    static void cleanStaleResult() {
        logger.debug("clean scheduler job history which has been deleted.");
        SQL.New("update SchedulerJobHistoryVO history" +
                " set history.resultDump = :result ," +
                " history.executeTime = 0" +
                " where history.executeTime = -1" +
                " and history.schedulerJobUuid not in (" +
                " select job.uuid from SchedulerJobVO job" +
                " )")
                .param("result", mnStoppedResultDump)
                .execute();
    }

    @Transactional(readOnly = true)
    private void fireEventIfJobGroupOver() {
        if (history.getSchedulerJobGroupUuid() == null) {
            return;
        }

        long count = Q.New(SchedulerJobGroupJobRefVO.class).eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, history.getSchedulerJobGroupUuid())
                .lte(SchedulerJobGroupJobRefVO_.createDate, startTime)
                .count();


        List<Long> ids = Q.New(SchedulerJobHistoryVO.class).select(SchedulerJobHistoryVO_.id)
                .eq(SchedulerJobHistoryVO_.schedulerJobGroupUuid, history.getSchedulerJobGroupUuid())
                .eq(SchedulerJobHistoryVO_.fireInstanceId, history.getFireInstanceId())
                .gte(SchedulerJobHistoryVO_.executeTime, 0)
                .listValues();

        if (ids.size() < count) {
            return;
        }

        List<String> failureResults = Q.New(SchedulerJobHistoryVO.class).select(SchedulerJobHistoryVO_.resultDump)
                .in(SchedulerJobHistoryVO_.id, ids)
                .eq(SchedulerJobHistoryVO_.success, false)
                .listValues();

        if (failureResults.isEmpty()) {
            return;
        }

        String name = Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, history.getSchedulerJobGroupUuid())
                .select(SchedulerJobGroupVO_.name)
                .findValue();

        SchedulerCanonicalEvents.SchedulerGroupExecutedData data = new SchedulerCanonicalEvents.SchedulerGroupExecutedData();
        data.setFailedCount(failureResults.size());
        data.setTotalCount(count);
        data.setJobGroupUuid(history.getSchedulerJobGroupUuid());
        data.setSchedulerGroupName(name);
        data.setErrors(failureResults.stream().map(rsp -> {
            String err = JSONObjectUtil.toObject(rsp, MessageReply.class).getError().getReadableDetails();
            return err.length() > 200 ? err.substring(0, 200) + "..." : err;
        }).collect(Collectors.toList()));

        FireSchedulerGroupResultMsg msg = new FireSchedulerGroupResultMsg();
        msg.setSchedulerJobGroupUuid(history.getSchedulerJobGroupUuid());
        msg.setFireInstanceId(history.getFireInstanceId());
        msg.setData(data);
        bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, msg.getSchedulerJobGroupUuid());
        bus.send(msg);
    }

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    @Override
    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }

    @Override
    public String getQueueName() {
        return null;
    }
}
