package org.zstack.zwatch.namespace;

import org.zstack.zwatch.datatype.metric.CountMetric;
import org.zstack.zwatch.datatype.metric.Metric;

public class LocalPrimaryStorageHostNamespace extends PrimaryStorageNamespace {
    public enum LabelNames {
        PrimaryStorageUuid,
        HostUuid
    }

    public static final Metric TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage =
            new CountMetric("TimeDurationRequiredForLocalStorageHostForecastUsageExceedingThresholdUsage",
                    metrics, LabelNames.PrimaryStorageUuid, LabelNames.HostUuid
            );

    static void init() {}
}
