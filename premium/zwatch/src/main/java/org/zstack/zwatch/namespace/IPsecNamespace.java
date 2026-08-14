package org.zstack.zwatch.namespace;

import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IPsecNamespace extends AbstractNamespace {
    public static final String NAME = "IPSec";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);


    public static final Metric IPSecConnectionBytes = new ByteRateMetric("IPSecConnectionBytes", metrics,
            false, LabelNames.IPSecUuid);
    public static final Metric IPSecConnectionPackets = new CountMetric("IPSecConnectionPackets", metrics,
            false, LabelNames.IPSecUuid);


    public IPsecNamespace() {}
    public IPsecNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        IPSecUuid,
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
        return IPsecConnectionVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.IPSecUuid.toString();
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }
}
