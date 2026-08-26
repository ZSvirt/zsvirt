package org.zstack.zwatch.namespace;

import org.zstack.header.Component;
import org.zstack.header.core.StaticInit;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.KeyProviderNamespaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeyProviderNamespace extends AbstractNamespace implements Component {
    public static final String NAME = "KeyProvider";

    private static final List<Metric> metrics = new ArrayList<>();
    public static final List<String> disableMetrics = getDisableMetrics(NAME);
    private static final List<EventFamily> events = new ArrayList<>();

    public static final EventFamily KeyProviderDefaultServiceUnavailable = new EventFamily("KeyProviderDefaultServiceUnavailable", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.StatusCode,
            EventLabelNames.StatusMessage,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily KeyProviderDefaultServiceRecovered = new EventFamily("KeyProviderDefaultServiceRecovered", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.StatusCode,
            EventLabelNames.StatusMessage,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);

    public static final EventFamily KeyProviderResourceServiceUnavailable = new EventFamily("KeyProviderResourceServiceUnavailable", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.StatusCode,
            EventLabelNames.StatusMessage,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    public static final EventFamily KeyProviderResourceServiceRecovered = new EventFamily("KeyProviderResourceServiceRecovered", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.StatusCode,
            EventLabelNames.StatusMessage,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);

    public static final EventFamily KeyProviderCertificateExpiring = new EventFamily("KeyProviderCertificateExpiring", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.CertificateType,
            EventLabelNames.ExpiredDate,
            EventLabelNames.DaysLeft,
            EventLabelNames.ReportDate,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Important);

    public static final EventFamily KeyProviderCertificateRecovered = new EventFamily("KeyProviderCertificateRecovered", events,
            EventLabelNames.KeyProviderUuid,
            EventLabelNames.KeyProviderName,
            EventLabelNames.KeyProviderType,
            EventLabelNames.CertificateType,
            EventLabelNames.ExpiredDate,
            EventLabelNames.DaysLeft,
            EventLabelNames.ReportDate,
            EventLabelNames.DefaultProvider
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Recovery);

    @StaticInit
    static void staticInit() {
        new KeyProviderNamespaceEvent();
    }

    public enum EventLabelNames {
        KeyProviderUuid,
        KeyProviderName,
        KeyProviderType,
        StatusCode,
        StatusMessage,
        CertificateType,
        ExpiredDate,
        DaysLeft,
        ReportDate,
        DefaultProvider,
        ResourceType,
        ResourceUuid,
        ProviderUuid,
        ProviderName,
    }

    public KeyProviderNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public KeyProviderNamespace() {
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
        return events;
    }

    @Override
    public String getResourceType() {
        return KeyProviderVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return EventLabelNames.KeyProviderUuid.toString();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
