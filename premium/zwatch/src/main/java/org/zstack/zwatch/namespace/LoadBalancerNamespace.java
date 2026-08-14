package org.zstack.zwatch.namespace;

import org.zstack.network.service.lb.LoadBalancerListenerVO;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.*;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerNamespace extends AbstractNamespace {
    public static final String NAME = "LoadBalancer";


    private static final List<Metric> metrics = new ArrayList<>();

    public static final Metric LoadBalancerBackendStatus = new StateMetric("LoadBalancerBackendStatus", metrics,
            false, LabelNames.LoadBalancerUuid, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendSessionNumber = new CountMetric("LoadBalancerBackendSessionNumber",
            metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendTrafficInBytes = new ByteRateMetric(
            "LoadBalancerBackendTrafficInBytes", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress);
    public static final Metric LoadBalancerBackendTrafficOutBytes = new ByteRateMetric(
            "LoadBalancerBackendTrafficOutBytes", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress);
    public static final Metric LoadBalancerStatus = new StateMetric("LoadBalancerStatus", metrics, false,
            LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerSessionNumber = new CountMetric("LoadBalancerSessionNumber", metrics,
            false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerTrafficInBytes = new ByteRateMetric("LoadBalancerTrafficInBytes", metrics,
            false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerTrafficOutBytes = new ByteRateMetric("LoadBalancerTrafficOutBytes", metrics,
            false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerSessionUsage = new PercentMetric("LoadBalancerSessionUsage", metrics, false,
            LabelNames.ListenerUuid
    );

    public static final Metric LoadBalancerRefusedSessionNumber = new CountMetric("LoadBalancerRefusedSessionNumber",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerConcurrentSessionNumber = new CountMetric("LoadBalancerConcurrentSessionNumber",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerNewSessionNumber = new CountMetric("LoadBalancerNewSessionNumber",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerTotalSessionNumber = new CountMetric("LoadBalancerTotalSessionNumber",
            metrics, false, LabelNames.ListenerUuid
    );

    public static final Metric LoadBalancerBackendRefusedSessionNumber = new CountMetric(
            "LoadBalancerBackendRefusedSessionNumber", metrics, false, LabelNames.ListenerUuid,
            LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendConcurrentSessionNumber = new CountMetric(
            "LoadBalancerBackendConcurrentSessionNumber", metrics, false, LabelNames.ListenerUuid,
            LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendNewSessionNumber = new CountMetric(
            "LoadBalancerBackendNewSessionNumber", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendTotalSessionNumber = new CountMetric(
            "LoadBalancerBackendTotalSessionNumber", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );

    public static final Metric LoadBalancerBackendHttp1xxResponses = new CountMetric(
            "LoadBalancerBackendHttp1xxResponses", metrics, false, LabelNames.ListenerUuid,
            LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendHttp2xxResponses = new CountMetric(
            "LoadBalancerBackendHttp2xxResponses", metrics, false, LabelNames.ListenerUuid,
            LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendHttp3xxResponses = new CountMetric(
            "LoadBalancerBackendHttp3xxResponses", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendHttp4xxResponses = new CountMetric(
            "LoadBalancerBackendHttp4xxResponses", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendHttp5xxResponses = new CountMetric(
            "LoadBalancerBackendHttp5xxResponses", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );
    public static final Metric LoadBalancerBackendHttpOtherResponses = new CountMetric(
            "LoadBalancerBackendHttpOtherResponses", metrics, false, LabelNames.ListenerUuid, LabelNames.NicIpAddress
    );

    public static final Metric LoadBalancerHttp1xxResponses = new CountMetric("LoadBalancerHttp1xxResponses",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerHttp2xxResponses = new CountMetric("LoadBalancerHttp2xxResponses",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerHttp3xxResponses = new CountMetric("LoadBalancerHttp3xxResponses",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerHttp4xxResponses = new CountMetric("LoadBalancerHttp4xxResponses",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerHttp5xxResponses = new CountMetric("LoadBalancerHttp5xxResponses",
            metrics, false, LabelNames.ListenerUuid
    );
    public static final Metric LoadBalancerHttpOtherResponses = new CountMetric("LoadBalancerHttpOtherResponses",
            metrics, false, LabelNames.ListenerUuid
    );



    public LoadBalancerNamespace() {
    }

    public LoadBalancerNamespace(DatabaseDriver driver) {
        super(driver);
    }

    public enum LabelNames {
        LoadBalancerUuid,
        ListenerUuid,
        NicIpAddress
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics;
    }

    @Override
    public List<EventFamily> getEvents() {
        return null;
    }

    @Override
    public String getResourceType() {
        return LoadBalancerListenerVO.class.getSimpleName();
    }

    @Override
    public String getIdentityLabelName() {
        return LabelNames.ListenerUuid.toString();
    }
}
