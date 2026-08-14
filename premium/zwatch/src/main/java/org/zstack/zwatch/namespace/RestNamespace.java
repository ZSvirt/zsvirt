package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.rest.RestAPIVO;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.RestEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2020/3/19.
 */
public class RestNamespace extends AbstractNamespace {
    public static final String NAME = "REST";
    private static final List<Metric> metrics = new ArrayList<>();
    private static final List<EventFamily> events = new ArrayList<>();

    public RestNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public RestNamespace() {
        super();
    }

    @StaticInit
    static void staticInit() {
        new RestEvent();
    }

    public enum LabelNames {
        ip,
        type,
    }

    public static final Metric SyncAPIs = new CountMetric("SyncAPIs", metrics, LabelNames.ip, LabelNames.type);
    public static final Metric ASyncAPIs = new CountMetric("ASyncAPIs", metrics, LabelNames.ip, LabelNames.type);

    public static final Metric IncreaseSyncAPIs = new CountMetric("IncreaseSyncAPIs", metrics, LabelNames.ip,
            LabelNames.type
    );
    public static final Metric IncreaseASyncAPIs = new CountMetric("IncreaseASyncAPIs", metrics, LabelNames.ip,
            LabelNames.type
    );

    public static final Metric SyncRests = new CountMetric("SyncRests", metrics, LabelNames.ip, LabelNames.type);
    public static final Metric ASyncRests = new CountMetric("ASyncRests", metrics, LabelNames.ip, LabelNames.type);

    public static final Metric IncreaseSyncRests = new CountMetric("IncreaseSyncRests", metrics, LabelNames.ip,
            LabelNames.type
    );
    public static final Metric IncreaseASyncRests = new CountMetric("IncreaseASyncRests", metrics, LabelNames.ip,
            LabelNames.type
    );

    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

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
        return RestAPIVO.class.getSimpleName();
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
