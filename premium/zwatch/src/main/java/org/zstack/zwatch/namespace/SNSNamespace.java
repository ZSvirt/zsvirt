package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.SNSNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2019-07-15
 */
public class SNSNamespace extends AbstractNamespace {
    public static final String NAME = "SNS";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();

    @StaticInit
    static void staticInit() {
        new SNSNamespaceEvent();
    }

    public SNSNamespace() {
        super();
    }

    public SNSNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum EventLabelNames {
        PhoneNumber,
        ErrCode,
        ErrMessage
    }

    public static final EventFamily SendSmsFailed = new EventFamily("SendSmsFailed", events,
            EventLabelNames.PhoneNumber, EventLabelNames.ErrCode, EventLabelNames.ErrMessage)
            .setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(metric -> !disableMetrics.contains(metric.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return SNSApplicationEndpointVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
