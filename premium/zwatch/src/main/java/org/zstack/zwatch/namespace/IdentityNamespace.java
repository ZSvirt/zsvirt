package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.SessionVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.IdentityNamespaceEvent;

import java.util.ArrayList;
import java.util.List;

public class IdentityNamespace extends AbstractNamespace {
    public static final String NAME = "Identity";

    private static final List<EventFamily> events = new ArrayList<>();

    @StaticInit
    static void staticInit() {
        new IdentityNamespaceEvent();
    }

    public enum EventLabelNames {
        sessionUuid,
        accountUuid,
        userUuid
    }

    public IdentityNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public IdentityNamespace() {
        super();
    }

    public static final EventFamily SessionForceLogout = new EventFamily("SessionForceLogout", events,
            EventLabelNames.accountUuid, EventLabelNames.userUuid
    ).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return null;
    }

    @Override
    public List<EventFamily> getEvents() {
        return events;
    }

    @Override
    public String getResourceType() {
        return SessionVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
