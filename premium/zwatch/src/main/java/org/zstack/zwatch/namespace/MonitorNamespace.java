package org.zstack.zwatch.namespace;

import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2020/4/7.
 */
public class MonitorNamespace extends AbstractNamespace {
    public static final String NAME = "Prometheus";

    private static final List<Metric> metrics = new ArrayList<>();
    private static final List<EventFamily> events = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    public MonitorNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public MonitorNamespace() {
        super();
    }

    public enum LabelNames {
        ip,
        type,
        target,
        table,
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
        return NAME;
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }

    public static final Metric PrometheusLasts = new TimePointMetric("PrometheusLasts", metrics,
            MonitorNamespace.LabelNames.ip
    );
    public static final Metric PrometheusSocketNum = new CountMetric("PrometheusSocketNum", metrics,
            MonitorNamespace.LabelNames.ip
    );
    // this metric combines the cpu usage percent and the memory usage, two value are in different unit
    // just use a counting metric to keep compatible
    public static final Metric PrometheusExpends = new CountMetric("PrometheusExpends", metrics,
            MonitorNamespace.LabelNames.ip, MonitorNamespace.LabelNames.type
    );
    public static final Metric PrometheusDiskSpace = new ByteSizeMetric("PrometheusDiskSpace", metrics,
            MonitorNamespace.LabelNames.ip, MonitorNamespace.LabelNames.type
    );
    public static final Metric PrometheusQueries = new CountMetric("PrometheusQueries", metrics,
            MonitorNamespace.LabelNames.ip, LabelNames.target, MonitorNamespace.LabelNames.type
    );
    public static final Metric PrometheusDeltaQueries = new CountRateMetric("PrometheusDeltaQueries", metrics,
            MonitorNamespace.LabelNames.ip, LabelNames.target, MonitorNamespace.LabelNames.type
    );

}
