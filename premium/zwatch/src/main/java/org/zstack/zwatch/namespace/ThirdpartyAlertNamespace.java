package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.ThirdpartyAlertNamespaceEvent;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertVO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ThirdpartyAlertNamespace extends AbstractNamespace {
    public static final String NAME = "Thirdparty";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();

    @StaticInit
    static void staticInit() {
        new ThirdpartyAlertNamespaceEvent();
    }

    public enum EventLabelNames {
        Uuid,
        PlatformUuid,
        Product,
        Service,
        Message,
        DataSource,
        Metric,
        AlertLevel,
        AlertTime,
        Dimensions
    }

    public static EventFamily ThirdpartyAlert = new EventFamily("ThirdpartyAlert", events,
            EventLabelNames.Uuid, EventLabelNames.PlatformUuid, EventLabelNames.Product,
            EventLabelNames.Service, EventLabelNames.Message,
            EventLabelNames.DataSource, EventLabelNames.Metric,
            EventLabelNames.AlertLevel, EventLabelNames.AlertTime,
            EventLabelNames.Dimensions);

    public ThirdpartyAlertNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public ThirdpartyAlertNamespace() {
        super();
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m ->!disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return ThirdpartyOriginalAlertVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
