package org.zstack.zwatch.namespace;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class L3NetworkNamespace extends AbstractNamespace {
    public static final String NAME = "L3Network";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private final AccountFilter filter = new AccountFilter(getName(), LabelNames.L3NetworkUuid.name(),
            L3NetworkVO.class
    );


    public enum LabelNames {
        L3NetworkUuid,
        L3NetworkType,
    }

    public static final Metric TotalAvailableIPCount = new CountMetric("TotalAvailableIPCount", metrics, false);
    public static final Metric TotalAvailableIPInPercent = new PercentMetric("TotalAvailableIPInPercent", metrics,
            false
    );
    public static final Metric TotalUsedIPCount = new CountMetric("TotalUsedIPCount", metrics, false);
    public static final Metric TotalUsedIPInPercent = new PercentMetric("TotalUsedIPInPercent", metrics, false);
    public static final Metric TotalLockedIPCount = new CountMetric("TotalLockedIPCount", metrics, false);
    public static final Metric TotalLockedIPInPercent = new PercentMetric("TotalLockedIPInPercent", metrics, false);

    public static final Metric AvailableIPCount = new CountMetric("AvailableIPCount", metrics, false,
            LabelNames.L3NetworkUuid, LabelNames.L3NetworkType
    );
    public static final Metric AvailableIPInPercent = new PercentMetric("AvailableIPInPercent", metrics, false,
            LabelNames.L3NetworkUuid, LabelNames.L3NetworkType
    );
    public static final Metric UsedIPCount = new CountMetric("UsedIPCount", metrics, false, LabelNames.L3NetworkUuid,
            LabelNames.L3NetworkType
    );
    public static final Metric UsedIPInPercent = new PercentMetric("UsedIPInPercent", metrics, false,
            LabelNames.L3NetworkUuid, LabelNames.L3NetworkType
    );

    public L3NetworkNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public L3NetworkNamespace() {
        super();
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
        return null;
    }

    @Override
    public String getResourceType() {
        return L3NetworkVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.L3NetworkUuid.toString();
    }
}
