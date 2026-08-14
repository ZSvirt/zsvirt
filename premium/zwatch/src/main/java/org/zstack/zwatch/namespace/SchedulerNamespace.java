package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.SchedulerNamespaceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kayo on 2018/4/4.
 */
public class SchedulerNamespace extends AbstractNamespace {
    public static final String NAME = "Scheduler";
    private static final List<EventFamily> events = new ArrayList<>();

    public SchedulerNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        SchedulerJobUuid,
        SchedulerJobGroupUuid
    }

    public enum EventLabelNames {
        isSuccess,
        TargetResourceUuid,
        VMSchedulerName,
        VMSchedulerExecutedResult,
        DatabaseSchedulerName,
        DatabaseSchedulerExecutedResult,
        SchedulerJobGroupName,
        TotalExecutionJobCount,
        FailedExecutionJobCount,
        SchedulerJobGroupErrors
    }

    public static final EventFamily VolumeSnapshotScheduler = new EventFamily("VolumeSnapshotScheduler", events,
            EventLabelNames.VMSchedulerName,
            EventLabelNames.VMSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily VolumeBackupScheduler = new EventFamily("VolumeBackupScheduler", events,
            EventLabelNames.VMSchedulerName,
            EventLabelNames.VMSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily VMStartScheduler = new EventFamily("VMStartScheduler", events,
            VmNamespace.EventLabelNames.VMSchedulerName,
            EventLabelNames.VMSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily VMStopScheduler = new EventFamily("VMStopScheduler", events,
            VmNamespace.EventLabelNames.VMSchedulerName,
            EventLabelNames.VMSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily VMRebootScheduler = new EventFamily("VMRebootScheduler", events,
            VmNamespace.EventLabelNames.VMSchedulerName,
            EventLabelNames.VMSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily DatabaseBackupScheduler = new EventFamily("DatabaseBackupScheduler", events,
            EventLabelNames.DatabaseSchedulerName,
            EventLabelNames.DatabaseSchedulerExecutedResult, EventLabelNames.isSuccess
    );
    public static final EventFamily SchedulerJobGroupExecutedResult = new EventFamily("JobGroupFailure", events,
            EventLabelNames.SchedulerJobGroupName,
            EventLabelNames.TotalExecutionJobCount, EventLabelNames.FailedExecutionJobCount,
            EventLabelNames.SchedulerJobGroupErrors
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    @StaticInit
    static void staticInit() {
        new SchedulerNamespaceEvent();
    }

    @Override
    public List<Metric> getMetrics() {
        return null;
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return SchedulerJobVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.SchedulerJobUuid.toString();
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }
}
