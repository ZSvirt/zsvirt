package org.zstack.zwatch.namespace;

import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.SessionVO;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.event.HaNamespaceEvent;

import java.util.ArrayList;
import java.util.List;

public class HaNamespace extends AbstractNamespace {
    public static final String NAME = "HA";

    private static final List<EventFamily> events = new ArrayList<>();

    @StaticInit
    static void staticInit() {
        new HaNamespaceEvent();
    }

    public enum EventLabelNames {
        hostUuid,
        vmUuid,
        Error
    }

    public HaNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public HaNamespace() {
        super();
    }

    public static final EventFamily MigrateVMFailedWithHostMaintain = new EventFamily("MigrateVMFailedWithHostMaintain", events, EventLabelNames.hostUuid,
            EventLabelNames.vmUuid, HaNamespace.EventLabelNames.Error).setEmergencyLevel(EventFamily.EmergencyLevel.Emergent);

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
