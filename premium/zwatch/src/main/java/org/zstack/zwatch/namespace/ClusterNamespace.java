package org.zstack.zwatch.namespace;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.ClusterHostQemuVersionMismatchEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cluster namespace event
 * 
 * Created by Wenhao.Zhang on 23/03/06
 */
public class ClusterNamespace extends AbstractNamespace {
    public static final String NAME = "Cluster";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    protected static final List<EventFamily> events = new ArrayList<>();

    public enum EventLabelNames {
        ClusterUuid,
        HostCount,
    }

    public static final EventFamily ClusterQemuVersionMismatch = new EventFamily("ClusterQemuVersionMismatch", events,
            EventLabelNames.HostCount
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Normal);

    public ClusterNamespace() {}

    public ClusterNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @StaticInit
    static void staticInit() {
        ClusterHostQemuVersionMismatchEvent.installEventCollector(ClusterQemuVersionMismatch);
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

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
        return ClusterVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return EventLabelNames.ClusterUuid.toString();
    }
}
