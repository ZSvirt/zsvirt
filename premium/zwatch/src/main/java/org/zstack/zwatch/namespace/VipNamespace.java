package org.zstack.zwatch.namespace;

import org.zstack.network.service.vip.VipVO;
import org.zstack.zwatch.datatype.metric.ByteRateMetric;
import org.zstack.zwatch.datatype.metric.ByteSizeMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PacketRateMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VipNamespace extends AbstractNamespace {
    public static final String NAME = "VIP";


    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    public static final Metric VIPInBoundTrafficInBytes = new ByteSizeMetric("VIPInBoundTrafficInBytes",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPInBoundTrafficInPackages = new PacketRateMetric("VIPInBoundTrafficInPackages",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPOutBoundTrafficInBytes = new ByteSizeMetric("VIPOutBoundTrafficInBytes",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPOutBoundTrafficInPackages = new PacketRateMetric("VIPOutBoundTrafficInPackages",
            metrics, false, LabelNames.VipUUID
    );

    public static final Metric VIPTotalInBytesIn5Min = new ByteRateMetric("VIPTotalInBytesIn5Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalInPacketsIn5Min = new PacketRateMetric("VIPTotalInPacketsIn5Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalOutBytesIn5Min = new ByteRateMetric("VIPTotalOutBytesIn5Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalOutPacketsIn5Min = new PacketRateMetric("VIPTotalOutPacketsIn5Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalInBytesIn1Min = new ByteRateMetric("VIPTotalInBytesIn1Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalInPacketsIn1Min = new PacketRateMetric("VIPTotalInPacketsIn1Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalOutBytesIn1Min = new ByteRateMetric("VIPTotalOutBytesIn1Min",
            metrics, false, LabelNames.VipUUID
    );
    public static final Metric VIPTotalOutPacketsIn1Min = new PacketRateMetric("VIPTotalOutPacketsIn1Min",
            metrics, false, LabelNames.VipUUID
    );

    public VipNamespace() {
    }

    public VipNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        VipUUID
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
        return VipVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.VipUUID.toString();
    }
}
