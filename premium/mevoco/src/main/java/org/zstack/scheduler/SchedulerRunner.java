package org.zstack.scheduler;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.scheduler.*;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.quartz.TriggerKey.triggerKey;
import static org.zstack.core.Platform.getManagementServerId;
import static org.zstack.core.Platform.inerr;


/**
 * Created by Mei Lei on 7/14/16.
 */
public class SchedulerRunner implements Job {
    private static final CLogger logger = Utils.getLogger(SchedulerRunner.class);

    @Autowired
    private transient SchedulerFacadeImpl schedulerFacade;
    @Autowired
    private transient ResourceDestinationMaker destMaker;

    private  String jobData;
    private String jobClassName;
    private String fireInstanceId;
    private SchedulerJob runnerJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (MevocoGlobalConfig.PAUSE_THE_WORLD.value(Boolean.class)) {
            logger.warn("world has been paused, skip scheduler job.");
            return;
        }

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        jobData = dataMap.getString("jobData");
        jobClassName = dataMap.getString("jobClassName");
        fireInstanceId = context.getTrigger().getJobDataMap().getString(SchedulerConstant.FIRE_INSTANCE_ID);
        try {
            runnerJob = (SchedulerJob) JSONObjectUtil.toObject(jobData, Class.forName(jobClassName));
            if (fireInstanceId == null) {
                fireInstanceId = Platform.getUuidFromBytes((context.getScheduledFireTime().toString() + runnerJob.getTriggerUuid()).getBytes());
            }

            runnerJob.setFireInstanceId(fireInstanceId);

            SchedulerJobGroupVO group = Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, runnerJob.getUuid()).find();
            if (group == null) {
                runnerJob.run(new NopeCompletion());
                return;
            }

            checkGroup(group.getUuid(), runnerJob.getTriggerUuid());

            String sql = "select job.uuid,ref.priority from SchedulerJobVO job, SchedulerJobGroupJobRefVO ref " +
                    "where job.uuid = ref.schedulerJobUuid " +
                    "and ref.schedulerJobGroupUuid = :groupUuid " +
                    "and job.state = :state";
            List<Tuple> tuples = SQL.New(sql, Tuple.class).param("groupUuid", group.getUuid())
                    .param("state", SchedulerJobGroupState.Enabled.toString()).list();
            if (tuples.isEmpty()) {
                logger.warn(String.format("no enabled job found in group[%s]", group.getUuid()));
                return;
            }

            Map<Integer, List<String>> schedulerJobByPriority = new HashMap<>();
            buildSchedulerJobTuple(context, tuples)
                    .forEach(tuple -> schedulerJobByPriority.computeIfAbsent(tuple.get(1, Integer.class),
                            k -> new ArrayList<>()).add(tuple.get(0, String.class)));

            List<List<String>> priorities = schedulerJobByPriority.entrySet()
                    .stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

            new While<>(priorities).each((schedulerJobUuids, com) -> {
                new While<>(schedulerJobUuids).all((jobUuid, innerCom) -> {
                    Class<? extends AbstractSchedulerJob> clz = SchedulerFacadeImpl.getJobClasses().get(group.getJobClassName());
                    AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(group.getJobData(), clz);
                    schedulerJob.setUuid(jobUuid);
                    schedulerJob.setFireInstanceId(fireInstanceId);
                    schedulerJob.setTriggerUuid(runnerJob.getTriggerUuid());
                    schedulerJob.run(new NopeCompletion() {
                        @Override
                        public void success() {
                            innerCom.done();
                        }
                    });
                }).run(new WhileDoneCompletion(com) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        com.done();
                    }
                });
            }).run(new WhileDoneCompletion(null) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                }
            });
        } catch (Exception e) {
            logger.warn(String.format("Class %s Not found error", jobClassName));
            logger.warn(String.format("Class %s Not found error", runnerJob.getClass().getName()));
            throw new RuntimeException(e);
        }
    }

    private List<Tuple> buildSchedulerJobTuple(JobExecutionContext context, List<Tuple> schedulerJobTuples) {
        String jobsToSchedulerStr = context.getTrigger().getJobDataMap().getString(SchedulerConstant.GROUP_JOBS_TO_SCHEDULER);
        if (jobsToSchedulerStr == null) {
            return schedulerJobTuples;
        }

        List<String> jobsToScheduler = Arrays.asList(jobsToSchedulerStr.split(","));
        List<Tuple> newSchedulerJobTuples = new ArrayList<>();

        if (!jobsToScheduler.isEmpty()) {
            schedulerJobTuples.forEach(tuple -> {
                if (jobsToScheduler.contains(tuple.get(0, String.class))) {
                    newSchedulerJobTuples.add(tuple);
                }
            });
        }

        return newSchedulerJobTuples;
    }

    private void checkGroup(String groupUuid, String triggerUuid) {
        String error = new SQLBatchWithReturn<String>() {
            @Override
            @Transactional(readOnly = true)
            protected String scripts() {
                String mnId = Q.New(SchedulerJobGroupVO.class).select(SchedulerJobGroupVO_.managementNodeUuid)
                        .eq(SchedulerJobGroupVO_.uuid, groupUuid)
                        .findValue();
                if (!getManagementServerId().equals(mnId)) {
                    return String.format("scheduler job group[uuid:%s] not run on MN[id:%s] on record, " +
                            "unschedule self", groupUuid, mnId);
                }

                boolean groupTriggerRefExists = Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, groupUuid)
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid, triggerUuid)
                        .isExists();
                if (groupTriggerRefExists) {
                    return null;
                }
                return String.format("scheduler job group[uuid:%s] has been removed from trigger[uuid:%s], " +
                        "unschedule self", groupUuid, triggerUuid);
            }
        }.scripts();

        if (error == null) {
            return;
        }

        try {
            schedulerFacade.getScheduler().unscheduleJob(triggerKey(triggerUuid, triggerUuid + "." + groupUuid));
        } catch (SchedulerException e) {
            logger.error("failed to unschedule scheduler job group");
        }

        throw new OperationFailureException(inerr(String.format("scheduler job group[uuid:%s] has been removed from " +
                "trigger[uuid:%s], unschedule self", groupUuid, triggerUuid)));
    }

        public String getJobClassName() {
            return jobClassName;
        }

        public void setJobClassName(String jobClassName) {
            this.jobClassName = jobClassName;
        }

        public String getJobData() {
            return jobData;
        }

        public void setJobData(String jobData) {
            this.jobData = jobData;
        }
}
