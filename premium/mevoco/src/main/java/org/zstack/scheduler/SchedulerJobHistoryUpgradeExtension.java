package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import static org.zstack.core.Platform.operr;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import static org.zstack.header.scheduler.SchedulerGlobalProperty.UPGRADE_SCHEDULER_JOB_HISTORY;
import org.zstack.header.scheduler.SchedulerJobGroupVO;
import org.zstack.header.scheduler.SchedulerJobGroupVO_;
import org.zstack.header.scheduler.SchedulerJobHistoryVO;
import org.zstack.header.scheduler.SchedulerJobHistoryVO_;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerRefVO;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerJobVO_;
import org.zstack.header.storageDevice.StorageDeviceConstants;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.Session;
import org.zstack.scheduler.autoscalinggroup.AutoScalingGroupJob;
import org.zstack.scheduler.snapshot.CreateVolumeSnapshotJob;
import org.zstack.scheduler.vm.RebootVmInstanceJob;
import org.zstack.scheduler.vm.StartVmInstanceJob;
import org.zstack.scheduler.vm.StopVmInstanceJob;
import org.zstack.sdk.zwatch.api.GetEventDataAction;
import org.zstack.sdk.zwatch.datatype.EventData;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2020/4/3.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SchedulerJobHistoryUpgradeExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(SchedulerJobHistoryUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ThreadFacade thdf;

    private SessionInventory session;
    private Map<String, String> groupType = new HashMap<>();
    private long intervalTimeInMills = TimeUnit.SECONDS.toMillis(5);
    private long nowTime = System.currentTimeMillis();

    @Override
    public boolean start() {
        if (UPGRADE_SCHEDULER_JOB_HISTORY) {
            upgrade();
            thdf.submitTimerTask(this::upgradeVmSchedulerJob, TimeUnit.MINUTES, 5);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @ExceptionSafe
    private void upgrade() {
        logger.debug("start to upgrade scheduler job history");
        updateStorageDeviceJob();
        putSchedulerJobGroupType();
        updateSchedulerGroup();
    }

    @Transactional
    protected void updateStorageDeviceJob() {
        List<Long> ids = Q.New(SchedulerJobHistoryVO.class).select(SchedulerJobHistoryVO_.id)
                .eq(SchedulerJobHistoryVO_.schedulerJobUuid, StorageDeviceConstants.LOCAL_RAID_SELF_TEST_JOB_UUID)
                .listValues();

        for (Long id : ids) {
            SQL.New(SchedulerJobHistoryVO.class).eq(SchedulerJobHistoryVO_.id, id)
                    .set(SchedulerJobHistoryVO_.fireInstanceId, Platform.getUuid())
                    .set(SchedulerJobHistoryVO_.jobType, SchedulerType.LOCAL_RAID_SELF_TEST)
                    .update();
        }
    }

    private boolean upgradeVmSchedulerJob() {
        session = Session.loginByAdmin();

        List<SchedulerJobVO> jobs = Q.New(SchedulerJobVO.class)
                .in(SchedulerJobVO_.jobClassName, Arrays.asList(RebootVmInstanceJob.class.getName(),
                        StartVmInstanceJob.class.getName(),
                        StopVmInstanceJob.class.getName(),
                        CreateVolumeSnapshotJob.class.getName()))
                .list();

        logger.debug(String.format("start to vm and volume scheduler job history, job count: %d", jobs.size()));
        upgradeJobHistory(jobs);
        return true;
    }

    private void upgradeJobHistory(List<SchedulerJobVO> jobs) {
        for (SchedulerJobVO job : jobs) {
            String triggerUuid = job.getAddedTriggerRefs().stream()
                    .map(SchedulerJobSchedulerTriggerRefVO::getSchedulerTriggerUuid)
                    .findFirst().orElse(null);
            persistHistory(job, triggerUuid);
        }
    }

    private void persistHistory(SchedulerJobVO job, String triggerUuid) {
        List<EventData> datas = getEventData(job.getUuid());
        if (datas.isEmpty()) {
            return;
        }

        logger.debug(String.format("start to migrate vm scheduler history[count: %d, jobUuid: %s] to mysql",
                datas.size(), job.getUuid()));

        persistHistoryGroupByTriggerTime(datas, triggerUuid, job);
    }

    private void persistHistoryGroupByTriggerTime(List<EventData> datas, String triggerUuid, SchedulerJobVO job) {
        String name = job.getJobClassName().equals(CreateVolumeSnapshotJob.class.getName()) ? "volumeUuid" : "vmInstanceUuid";
        String jobType;
        if (job.getJobClassName().equals(RebootVmInstanceJob.class.getName())) {
            jobType = SchedulerType.REBOOT_VM;
        } else if (job.getJobClassName().equals(StartVmInstanceJob.class.getName())) {
            jobType = SchedulerType.START_VM;
        } else if (job.getJobClassName().equals(StopVmInstanceJob.class.getName())) {
            jobType = SchedulerType.STOP_VM;
        } else if (job.getJobClassName().equals(CreateVolumeSnapshotJob.class.getName())) {
            jobType = SchedulerType.VOLUME_SNAPSHOT;
        } else if (job.getJobClassName().equals(AutoScalingGroupJob.class.getName())) {
            jobType = SchedulerType.RUN_AUTO_SCALING_GROUP;
        } else {
            return;
        }
        for (EventData data : datas) {
            doPersistHistory(triggerUuid, data, job.getTargetResourceUuid(), name, jobType);
        }
    }

    @ExceptionSafe
    private void doPersistHistory(String triggerUuid, EventData data, String targetResourceUuid, String resourceFieldName, String jobType) {
        long finalStartTime = data.getTime() - data.getTime() % TimeUnit.MINUTES.toMillis(1);

        SchedulerJobHistoryVO history = new SchedulerJobHistoryVO();
        history.setSuccess(Boolean.valueOf(data.getLabels().get("isSuccess").toString()));
        history.setStartTime(new Timestamp(finalStartTime));
        history.setExecuteTime(TimeUnit.MILLISECONDS.toSeconds(data.getTime() - finalStartTime));
        history.setSchedulerJobUuid(data.getResourceId());
        history.setTriggerUuid(triggerUuid);
        history.setTargetResourceUuid(targetResourceUuid);
        history.setFireInstanceId(Platform.getUuidFromBytes((history.getStartTime().toString() + triggerUuid).getBytes()));
        history.setJobType(jobType);

        APIEvent evt = new APIEvent();
        if (!history.isSuccess()) {
            evt.setError(operr(data.getError()));
        } else {
            evt.setSuccess(true);
        }

        Map<String, String> param = Collections.singletonMap(resourceFieldName, targetResourceUuid);
        history.setRequestDump(JSONObjectUtil.toJsonString(param));
        history.setResultDump(JSONObjectUtil.toJsonString(evt));

        dbf.persist(history);
    }

    private List<EventData> getEventData(String jobUuid) {
        long startTime = nowTime - TimeUnit.DAYS.toMillis(180);

        GetEventDataAction a = new GetEventDataAction();
        a.sessionId = session.getUuid();
        a.startTime = startTime;
        a.conditions = Collections.singletonList("resourceId=" + jobUuid);
        a.limit = 100;
        GetEventDataAction.Result result = a.call();

        if (result.error != null) {
            logger.error(String.format("get vm[uuid:%s] scheduler history failed!", jobUuid));
            return Collections.emptyList();
        }

        return result.value.events;
    }

    private void putSchedulerJobGroupType() {
        List<Tuple> ts = Q.New(SchedulerJobGroupVO.class).select(SchedulerJobGroupVO_.uuid, SchedulerJobGroupVO_.jobType).listTuple();
        for (Tuple t : ts) {
            groupType.put(t.get(0, String.class), t.get(1, String.class));
        }
    }

    private void updateSchedulerGroup() {
        // some records from upgrade extension may have no triggerUuid, delete first.
        SQL.New(SchedulerJobHistoryVO.class).isNull(SchedulerJobHistoryVO_.triggerUuid).delete();

        long count = SQL.New("select count(distinct startTime) from SchedulerJobHistoryVO where schedulerJobGroupUuid is not null", Long.class).find();
        if (count == 0) {
            return;
        }

        List<Date> sameFireTimes = new ArrayList<>();
        final Date[] startTime = {new Date(0)};
        SQL.New("select distinct startTime from SchedulerJobHistoryVO where schedulerJobGroupUuid is not null order by startTime", Date.class).limit(1000).paginate(count, times -> {
            for (Object time : times) {
                if (!sameFire(startTime[0], (Date) time) && !sameFireTimes.isEmpty()) {
                    handleSameFire(sameFireTimes.get(0), sameFireTimes.get(sameFireTimes.size() - 1));
                    sameFireTimes.clear();
                }

                sameFireTimes.add((Date) time);
                startTime[0] = (Date) time;
            }

            handleSameFire(sameFireTimes.get(0), sameFireTimes.get(sameFireTimes.size() - 1));
        });
    }

    private boolean sameFire(Date t1, Date t2) {
        return (t2.getTime() - t1.getTime()) / intervalTimeInMills == 0;
    }

    private void handleSameFire(Date from, Date to) {
        List<SchedulerJobHistoryVO> histories = Q.New(SchedulerJobHistoryVO.class)
                .gte(SchedulerJobHistoryVO_.startTime, from)
                .lte(SchedulerJobHistoryVO_.startTime, to)
                .notNull(SchedulerJobHistoryVO_.schedulerJobGroupUuid)
                .list();

        for (List<SchedulerJobHistoryVO> sameGroupHistories : histories.stream()
                .collect(Collectors.groupingBy(SchedulerJobHistoryVO::getSchedulerJobGroupUuid)).values()) {
            handleSameGroupHistories(sameGroupHistories);
        }
    }

    private void handleSameGroupHistories(List<SchedulerJobHistoryVO> histories) {
        String jobType = getJobType(histories.get(0));
        for (List<SchedulerJobHistoryVO> sameTriggerHistories : histories.stream()
                .peek(it -> it.setJobType(jobType))
                .collect(Collectors.groupingBy(SchedulerJobHistoryVO::getTriggerUuid)).values()) {
            handleSameGroupSameTriggerHistories(sameTriggerHistories);
        }

        dbf.updateCollection(histories);
    }

    private void handleSameGroupSameTriggerHistories(List<SchedulerJobHistoryVO> histories) {
        String fireInstanceId = Platform
                .getUuidFromBytes((histories.get(0).getStartTime().toInstant() + histories.get(0).getTriggerUuid()).getBytes());

        histories.forEach(it -> it.setFireInstanceId(fireInstanceId));
    }

    private String getJobType(SchedulerJobHistoryVO history) {
        if (groupType.keySet().contains(history.getSchedulerJobGroupUuid())) {
            return groupType.get(history.getSchedulerJobGroupUuid());
        } else if (history.getRequestDump().startsWith("{\"rootVolumeUuid")) {
            groupType.put(history.getSchedulerJobGroupUuid(), SchedulerType.VM_BACKUP);
            return SchedulerType.VM_BACKUP;
        } else if (history.getRequestDump().startsWith("{\"volumeUuid")) {
            if (history.getResultDump().contains("{\"type\":\"Root\"")) {
                groupType.put(history.getSchedulerJobGroupUuid(), SchedulerType.ROOT_VOLUME_BACKUP);
                return SchedulerType.ROOT_VOLUME_BACKUP;
            } else if (history.getResultDump().contains("{\"type\":\"Data\"")) {
                groupType.put(history.getSchedulerJobGroupUuid(), SchedulerType.VOLUME_BACKUP);
                return SchedulerType.VOLUME_BACKUP;
            }

            VolumeType volumeType = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, history.getTargetResourceUuid())
                    .select(VolumeVO_.type).findValue();
            if (volumeType == VolumeType.Root) {
                groupType.put(history.getSchedulerJobGroupUuid(), SchedulerType.ROOT_VOLUME_BACKUP);
                return SchedulerType.ROOT_VOLUME_BACKUP;
            } else if (volumeType == VolumeType.Data){
                groupType.put(history.getSchedulerJobGroupUuid(), SchedulerType.VOLUME_BACKUP);
                return SchedulerType.VOLUME_BACKUP;
            }
        }

        return null;
    }
}
