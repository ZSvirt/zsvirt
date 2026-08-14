package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.identity.Session;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.api.APIGetEventDataMsg;
import org.zstack.zwatch.api.APIGetEventDataReply;
import org.zstack.zwatch.datatype.EventData;
import org.zstack.zwatch.datatype.Label;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.storage.backup.DatabaseBackupGlobalProperty.UPGRADE_DATABASE_BACKUP_HISTORY;

/**
 * Created by MaJin on 2019/12/23.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DatabaseBackupHistoryUpgradeExtension implements Component {
    protected static final CLogger logger = Utils.getLogger(VolumeBackupHistoryUpgradeExtension.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private SessionInventory session;

    private long nowTime = System.currentTimeMillis();

    private final String missedTriggerUuid = "9b44d7b3ce36418685b53c236b901160";

    @Override
    public boolean start() {
        if (UPGRADE_DATABASE_BACKUP_HISTORY) {
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

        List<SchedulerJobVO> jobs = Q.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.jobClassName, CreateDatabaseBackupJob.class.getName())
                .list();

        logger.debug(String.format("start to upgrade database backup scheduler job history, job count: %d", jobs.size()));
        upgradeJobHistory(jobs);
        return true;
    }

    private void upgradeJobHistory(List<SchedulerJobVO> jobs) {
        for (SchedulerJobVO job : jobs) {
            String triggerUuid = job.getAddedTriggerRefs().stream()
                    .map(SchedulerJobSchedulerTriggerRefVO::getSchedulerTriggerUuid)
                    .findFirst().orElse(missedTriggerUuid);
            persistHistory(job, triggerUuid);
        }
    }

    @ExceptionSafe
    private void persistHistory(SchedulerJobVO job, String triggerUuid) {
        List<EventData> datas = getEventData(job.getUuid());
        if (datas.isEmpty()) {
            return;
        }

        logger.debug(String.format("start to migrate database backup scheduler history[count: %d, jobUuid: %s] to mysql",
                datas.size(), job.getUuid()));

        persistHistoryGroupByTriggerTime(datas, triggerUuid, job);
    }

    private void persistHistoryGroupByTriggerTime(List<EventData> datas, String triggerUuid, SchedulerJobVO job) {
        for (EventData data : datas) {
            doPersistHistory(triggerUuid, data, job.getTargetResourceUuid());
        }
    }

    @ExceptionSafe
    private void doPersistHistory(String triggerUuid, EventData data, String targetResourceUuid) {
        long finalStartTime = data.getTime() - data.getTime() % TimeUnit.MINUTES.toMillis(1);

        SchedulerJobHistoryVO history = new SchedulerJobHistoryVO();
        history.setSuccess(Boolean.valueOf(data.getLabels().get("isSuccess")));
        history.setStartTime(new Timestamp(finalStartTime));
        history.setExecuteTime(TimeUnit.MILLISECONDS.toSeconds(data.getTime() - finalStartTime));
        history.setSchedulerJobUuid(data.getResourceId());
        history.setTriggerUuid(triggerUuid);
        history.setTargetResourceUuid(targetResourceUuid);
        history.setFireInstanceId(Platform.getUuidFromBytes((history.getStartTime().toString() + triggerUuid).getBytes()));
        history.setJobType(SchedulerType.DATABASE_BACKUP);
        String result = data.getLabels().get("DatabaseSchedulerExecutedResult");
        String bsUuid = result.substring(result.length() - 32);
        List<String> bsUuids = StringDSL.isZStackUuid(bsUuid) ? Collections.singletonList(bsUuid) : null;

        APIEvent evt = new APIEvent();
        if (!history.isSuccess()) {
            evt.setError(operr(data.getError()));
        } else {
            evt.setSuccess(true);
        }

        DatabaseBackupLongJobParams param = new DatabaseBackupLongJobParams();
        param.setAlternativeBackupStorageUuids(bsUuids);

        history.setRequestDump(JSONObjectUtil.toJsonString(param));
        history.setResultDump(JSONObjectUtil.toJsonString(evt));

        dbf.persist(history);
    }

    private List<EventData> getEventData(String jobUuid) {
        long startTime = nowTime - TimeUnit.DAYS.toMillis(180);

        APIGetEventDataMsg gmsg = new APIGetEventDataMsg();
        gmsg.setSession(session);
        gmsg.setStartTime(startTime);
        gmsg.setLabelList(Collections.singletonList(new Label("resourceId=" + jobUuid)));
        gmsg.setLimit(100);
        bus.makeLocalServiceId(gmsg, ZWatchConstants.SERVICE_ID);
        MessageReply reply = bus.call(gmsg);
        if (!reply.isSuccess()) {
            logger.error("get database backup history failed!");
            return Collections.emptyList();
        }

        APIGetEventDataReply r = reply.castReply();
        return r.getEvents();
    }
}
