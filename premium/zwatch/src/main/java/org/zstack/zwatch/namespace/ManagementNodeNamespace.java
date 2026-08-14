package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.ManagementNodeNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by kayo on 2018/8/3.
 */
public class ManagementNodeNamespace extends AbstractNamespace {
    public static final String NAME = "MN";
    private static final List<Metric> metrics = new ArrayList<>();
    private static final List<EventFamily> events = new ArrayList<>();

    public ManagementNodeNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public ManagementNodeNamespace() {
        super();
    }

    @StaticInit
    static void staticInit() {
        new ManagementNodeNamespaceEvent();
    }

    public enum LabelNames {
        ManagementNodeIP,
        type,
        db,
        table,
    }

    public static final Metric TimeNeededToSyncDB = new TimeDurationMetric("TimeNeededToSyncDB", metrics,
            LabelNames.ManagementNodeIP
    );
    public static final Metric DbFencerIpReachable = new StateMetric("DbFencerIpReachable", metrics,
            LabelNames.ManagementNodeIP
    );

    public static final Metric MNProgressLasts = new TimePointMetric("MNProgressLasts", metrics,
            LabelNames.ManagementNodeIP
    );
    public static final Metric MNProgressSocketNum = new CountMetric("MNProgressSocketNum", metrics,
            LabelNames.ManagementNodeIP
    );
    public static final Metric MNProgressGCStatus = new StateMetric("MNProgressGCStatus", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );
    // for compatible
    public static final Metric MNProgressExpends = new CountMetric("MNProgressExpends", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );
    public static final Metric MNProgressMsgNum = new CountMetric("MNProgressMsgNum", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );

    public static final Metric MNThreadPoolStatus = new StateMetric("MNThreadPoolStatus", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );
    public static final Metric MNQueueStatus = new CountMetric("MNQueueStatus", metrics, LabelNames.ManagementNodeIP,
            LabelNames.type
    );

    public static final Metric MysqlLasts = new TimePointMetric("MysqlLasts", metrics, LabelNames.ManagementNodeIP);
    // for compatible
    public static final Metric MysqlProgressExpends = new CountMetric("MysqlProgressExpends", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );
    public static final Metric MysqlSlowQuery = new CountMetric("MysqlSlowQuery", metrics, LabelNames.ManagementNodeIP);
    public static final Metric MysqlProcessLists = new CountMetric("MysqlProcessLists", metrics,
            LabelNames.ManagementNodeIP, LabelNames.db, LabelNames.type
    );
    public static final Metric MysqlTableSize = new ByteSizeMetric("MysqlTableSize", metrics,
            LabelNames.ManagementNodeIP, LabelNames.db, LabelNames.type
    );

    public static final Metric MysqlQuerys = new CountMetric("MysqlQuerys", metrics, LabelNames.ManagementNodeIP,
            LabelNames.type
    );
    public static final Metric MysqlTransactions = new CountMetric("MysqlTransactions", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );

    public static final Metric MysqlOpenttables = new CountMetric("MysqlOpenttables", metrics,
            LabelNames.ManagementNodeIP, LabelNames.table
    );
    // for compatible
    public static final Metric MysqlQueryCache = new CountMetric("MysqlQueryCache", metrics,
            LabelNames.ManagementNodeIP, LabelNames.type
    );
    public static final Metric MysqlDbError = new CountMetric("MysqlDbError", metrics, LabelNames.ManagementNodeIP,
            LabelNames.type
    );

    public static final Metric ErrorCodes = new CountMetric("ErrorCodes", metrics, LabelNames.ManagementNodeIP,
            LabelNames.type
    );

    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    public enum EventLabelNames {
        ManagementNodeIP,
    }

    public static final EventFamily ManagementNodeLeft = new EventFamily("ManagementNodeLeft", events,
            EventLabelNames.ManagementNodeIP
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily ManagementNodeJoin = new EventFamily("ManagementNodeJoin", events,
            EventLabelNames.ManagementNodeIP
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily ManagementNodeTemporalRegression = new EventFamily(
            "ManagementNodeTemporalRegression", events, EventLabelNames.ManagementNodeIP).setEmergencyLevel(
            EventFamily.EmergencyLevel.Emergent);

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m -> !disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return ManagementNodeVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }
}
