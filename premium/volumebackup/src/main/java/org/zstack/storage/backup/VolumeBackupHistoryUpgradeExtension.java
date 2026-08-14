package org.zstack.storage.backup;

import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.TimerTask;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.Event;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.header.storage.backup.BackupMode;
import org.zstack.header.vo.ResourceVO;
import org.zstack.identity.Session;
import org.zstack.scheduler.SchedulerConstant;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.api.APIGetEventDataMsg;
import org.zstack.zwatch.api.APIGetEventDataReply;
import org.zstack.zwatch.datatype.EventData;
import org.quartz.*;
import org.zstack.zwatch.datatype.Label;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.storage.backup.VolumeBackupGlobalProperty.UPGRADE_VOLUME_BACKUP_HISTORY;

/**
 * Created by MaJin on 2019/5/7.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupHistoryUpgradeExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(VolumeBackupHistoryUpgradeExtension.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private SessionInventory session;

    private long nowTime = System.currentTimeMillis();

    @Override
    public boolean start() {
        if (UPGRADE_VOLUME_BACKUP_HISTORY) {
            thdf.submitTimerTask(this::upgrade, TimeUnit.MINUTES, 5);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private boolean upgrade() {
        session = Session.loginByAdmin();

        List<Tuple> ts = SQL.New("SELECT ref.schedulerJobGroupUuid, job.uuid, job.targetResourceUuid" +
                " FROM SchedulerJobVO job" +
                " JOIN SchedulerJobGroupJobRefVO ref on ref.schedulerJobUuid = job.uuid" +
                " WHERE job.jobClassName in (:classes)", Tuple.class)
                .param("classes", Arrays.asList(CreateVolumeBackupJob.class.getName(), CreateVmBackupJob.class.getName()))
                .list();

        Map<String, Map<String, String>> groupJobResourceUuidRef = new HashMap<>();
        for (Tuple t : ts) {
            groupJobResourceUuidRef.computeIfAbsent(t.get(0, String.class), it -> new HashMap<>())
                    .put(t.get(1, String.class), t.get(2, String.class));
        }

        logger.debug(String.format("start to upgrade scheduler group job history, group count: %d", ts.size()));
        upgradeGroupHistory(groupJobResourceUuidRef);
        return true;
    }

    private void upgradeGroupHistory(Map<String, Map<String, String>> groupJobResourceUuidRef) {
        Map<String, SchedulerJobGroupVO> groups = dbf.listByPrimaryKeys(groupJobResourceUuidRef.keySet(), SchedulerJobGroupVO.class)
                .stream().collect(Collectors.toMap(ResourceVO::getUuid, it -> it));
        groupJobResourceUuidRef.forEach((groupUuid, jobResourceUuids) -> {
            if (jobResourceUuids.isEmpty()) {
                return;
            }

            List<SchedulerTriggerVO> triggers = SQL.New("select trigger" +
                    " from SchedulerJobGroupSchedulerTriggerRefVO ref, SchedulerTriggerVO trigger" +
                    " where ref.schedulerJobGroupUuid = :groupUuid" +
                    " and trigger.uuid = ref.schedulerTriggerUuid", SchedulerTriggerVO.class)
                    .param("groupUuid", groupUuid)
                    .list();


            persistHistory(groups.get(groupUuid), triggers, jobResourceUuids);
        });
    }

    @ExceptionSafe
    private void persistHistory(SchedulerJobGroupVO group, List<SchedulerTriggerVO> triggers, Map<String, String> jobResourceUuids) {
        if (triggers.isEmpty()) {
            return;
        }

        List<EventData> datas = getEventData(jobResourceUuids.keySet(), triggers);
        if (datas.isEmpty()) {
            return;
        }

        logger.debug(String.format("start to migrate volume backup scheduler history[count: %d, groupUuid: %s] to mysql",
                datas.size(), group.getUuid()));

        datas.sort(Comparator.comparing(EventData::getTime));
        String fullTriggerUuid = JSONObjectUtil.toObject(group.getJobData(), CreateVmBackupJob.class).getFullBackupTriggerUuid();
        SchedulerTriggerVO fullTrigger = triggers.stream().filter(it -> it.getUuid().equals(fullTriggerUuid)).findFirst().orElse(null);
        if (fullTrigger != null) {
            List<EventData> fullBackupDatas = datas.stream()
                    .filter(it -> it.getTime() > fullTrigger.getLastOpDate().getTime() &&
                            it.getLabels().get("VMSchedulerExecutedResult").contains("full"))
                    .collect(Collectors.toList());
            List<Long> fullTriggerTimes = getTriggerTimes(fullTrigger);

            persistHistoryGroupByTriggerTime(group, fullTriggerTimes, fullBackupDatas, triggerTime -> fullTriggerUuid, jobResourceUuids, null);

            datas.removeAll(fullBackupDatas);
            triggers.remove(fullTrigger);
        }

        if (triggers.isEmpty()) {
            return;
        }

        Map<Long, String> dateTriggerRef = new HashMap<>();
        List<Long> otherTriggerTimes = new ArrayList<>();
        for (SchedulerTriggerVO trigger : triggers) {
            List<Long> triggerTimes = getTriggerTimes(trigger);
            triggerTimes.forEach(it -> dateTriggerRef.put(it, trigger.getUuid()));
            otherTriggerTimes.addAll(triggerTimes);
        }
        otherTriggerTimes.sort(Comparator.comparing(it -> it));
        persistHistoryGroupByTriggerTime(group, otherTriggerTimes, datas,
                time -> dateTriggerRef.getOrDefault(time, triggers.get(0).getUuid()),
                jobResourceUuids,
                eventData -> !eventData.getLabels().get("VMSchedulerExecutedResult").contains("full"));
    }

    private void persistHistoryGroupByTriggerTime(SchedulerJobGroupVO group, List<Long> triggerTimes, List<EventData> datas,
                                                  Function<Long, String> triggerUuidCalc, Map<String, String> jobResourceUuids,
                                                  Function<EventData, Boolean> filter) {
        Iterator<Long> triggerTimesIter = triggerTimes.iterator();
        int nowIndex = -1;
        int lastIndex = 0;
        long beginTime = 0;
        long endTime = triggerTimesIter.hasNext() ? triggerTimesIter.next() : Long.MAX_VALUE;

        // make sure all data will be processed
        EventData lastFakeData = new EventData();
        lastFakeData.setTime(Long.MAX_VALUE);
        datas.add(lastFakeData);
        for (EventData data : datas) {
            nowIndex += 1;
            if (data.getTime() >= endTime) {
                // persist previous data
                List<EventData> inThisTimeDatas = datas.subList(lastIndex, nowIndex);
                doPersistHistory(group, triggerUuidCalc.apply(beginTime), beginTime, inThisTimeDatas, jobResourceUuids, filter);
                lastIndex = nowIndex;

                // skip misfire times
                do {
                    beginTime = endTime;
                    endTime = triggerTimesIter.hasNext() ? triggerTimesIter.next() : Long.MAX_VALUE;
                } while (data.getTime() >= endTime && endTime != Long.MAX_VALUE);
            }
        }
    }

    @ExceptionSafe
    private void doPersistHistory(SchedulerJobGroupVO groupVO, String triggerUuid, long startTime,
                                  List<EventData> datas, Map<String, String> jobResourceUuids, Function<EventData, Boolean> filter) {
        if (datas.isEmpty()) {
            return;
        }

        List<SchedulerJobHistoryVO> histories = new ArrayList<>();
        for (EventData data : datas) {
            if (filter != null && !filter.apply(data)) {
                continue;
            }

            long finalStartTime = startTime;
            if (startTime == 0L) {
                finalStartTime = data.getTime() - data.getTime() % TimeUnit.MINUTES.toMillis(1);
            }

            SchedulerJobHistoryVO history = new SchedulerJobHistoryVO();
            history.setSuccess(Boolean.valueOf(data.getLabels().get("isSuccess")));
            history.setStartTime(new Timestamp(finalStartTime));
            history.setExecuteTime(TimeUnit.MILLISECONDS.toSeconds(data.getTime() - finalStartTime));
            history.setSchedulerJobGroupUuid(groupVO.getUuid());
            history.setSchedulerJobUuid(data.getResourceId());
            history.setTriggerUuid(triggerUuid);
            history.setTargetResourceUuid(jobResourceUuids.get(data.getResourceId()));
            history.setFireInstanceId(Platform.getUuidFromBytes((String.valueOf(finalStartTime) + triggerUuid).getBytes()));
            history.setJobType(groupVO.getJobType());
            String result = data.getLabels().get("VMSchedulerExecutedResult");
            boolean isFull = result.contains("full");
            String bsUuid = result.substring(result.length() - 32);
            List<String> bsUuids = StringDSL.isZStackUuid(bsUuid) ? Collections.singletonList(bsUuid) : null;

            APIEvent evt = new APIEvent();
            if (!history.isSuccess()) {
                evt.setError(operr(data.getError()));
            } else {
                evt.setSuccess(true);
            }

            VolumeBackupLongJobParams param = new VolumeBackupLongJobParams();
            param.setMode(isFull ? BackupMode.full : BackupMode.auto);
            param.setAlternativeBackupStorageUuids(bsUuids);

            history.setRequestDump(JSONObjectUtil.toJsonString(param));
            history.setResultDump(JSONObjectUtil.toJsonString(evt));
            histories.add(history);
        }

        dbf.updateCollection(histories);
    }

    private List<Long> getTriggerTimes(SchedulerTriggerVO triggerVO) {
        OperableTrigger trigger;
        switch (triggerVO.getSchedulerType()) {
            case SchedulerConstant.SIMPLE_TYPE_STRING:
                SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl();
                simpleTrigger.setStartTime(triggerVO.getStartTime());
                simpleTrigger.setRepeatCount(triggerVO.getRepeatCount() - 1);
                simpleTrigger.setRepeatInterval(TimeUnit.SECONDS.toMillis(triggerVO.getSchedulerInterval()));
                simpleTrigger.setEndTime(triggerVO.getStopTime());
                simpleTrigger.setNextFireTime(getFromDate(triggerVO));
                trigger = simpleTrigger;
                break;
            case SchedulerConstant.CRON_TYPE_STRING:
                CronTriggerImpl cronTriggerImpl = new CronTriggerImpl();
                cronTriggerImpl.setStartTime(triggerVO.getStartTime() != null ? triggerVO.getStartTime() : getFromDate(triggerVO));
                try {
                    cronTriggerImpl.setCronExpression(triggerVO.getCron());
                } catch (ParseException e) {
                    logger.error(String.format("cannot parse cron %s", triggerVO.getCron()), e);
                    return Collections.EMPTY_LIST;
                }
                trigger = cronTriggerImpl;
                break;
            default:
                // cannot be here
                logger.error("how can be here?");
                return Collections.EMPTY_LIST;
        }

        if (triggerTooThick(trigger)) {
            logger.warn(String.format("trigger[uuid: %s] is too thick, skip it", triggerVO.getUuid()));
            return Collections.EMPTY_LIST;
        } else {
            return TriggerUtils.computeFireTimesBetween(trigger, null, getFromDate(triggerVO), new Date(nowTime))
                    .stream().map(Date::getTime).collect(Collectors.toList());
        }
    }

    private List<EventData> getEventData(Collection<String> jobUuids, List<SchedulerTriggerVO> triggers) {
        triggers.sort(Comparator.comparing(SchedulerTriggerVO::getLastOpDate));
        long startTime = nowTime - TimeUnit.DAYS.toMillis(180);

        APIGetEventDataMsg gmsg = new APIGetEventDataMsg();
        gmsg.setSession(session);
        gmsg.setStartTime(startTime);
        gmsg.setLabelList(Collections.singletonList(new Label("resourceId=~^" + String.join("|", jobUuids) + "$")));
        gmsg.setLimit(jobUuids.size() * 100);
        bus.makeLocalServiceId(gmsg, ZWatchConstants.SERVICE_ID);
        MessageReply reply = bus.call(gmsg);
        if (!reply.isSuccess()) {
            logger.error("get volume backup history failed!");
            return Collections.EMPTY_LIST;
        }

        APIGetEventDataReply r = reply.castReply();
        return r.getEvents();
    }

    private Date getFromDate(SchedulerTriggerVO trigger) {
        return new Date(Long.max(nowTime - TimeUnit.DAYS.toMillis(180), trigger.getLastOpDate().getTime()));
    }

    private boolean triggerTooThick(OperableTrigger trigger) {
        int samplingNumber = 5;
        List<Date> dates = TriggerUtils.computeFireTimes(trigger, null, samplingNumber);
        return dates.size() == samplingNumber &&
                (dates.get(samplingNumber - 1).getTime() - dates.get(0).getTime()) / samplingNumber < TimeUnit.MINUTES.toMillis(10);
    }
}
