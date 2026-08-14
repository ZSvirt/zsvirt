package org.zstack.zwatch.namespace;

import org.zstack.core.Platform;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.datatype.metric.ByteSizeMetric;
import org.zstack.zwatch.datatype.EventFamily;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.metric.PercentMetric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class SystemNamespace extends AbstractNamespace implements Component {
    public static final String NAME = "System";

    private static final List<Metric> metrics = new ArrayList<>();
    protected static final List<String> disableMetrics = getDisableMetrics(NAME);

    @Override
    public boolean start() {
        ZWatchGlobalConfig.MANAGEMENT_NODE_DIR_TO_MONITOR.installUpdateExtension((oldConfig, newConfig) -> {
            for (String d : newConfig.value().split(",")) {
                d = d.trim();
                if (!new File(d).exists()) {
                    throw new OperationFailureException(argerr("folder[%s] not found on the management server[%s]", d,
                            Platform.getManagementServerIp()
                    ));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public enum LabelNames {
        ManagementNodeIP,
        DirPath,
    }

    public static final Metric ManagementServerDirFreeCapacityInBytes = new ByteSizeMetric(
            "ManagementServerDirFreeCapacityInBytes", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementServerDirFreeCapacityInPercent = new PercentMetric(
            "ManagementServerDirFreeCapacityInPercent", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementServerDirUsedCapacityInBytes = new ByteSizeMetric(
            "ManagementServerDirUsedCapacityInBytes", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementServerDirUsedCapacityInPercent = new PercentMetric(
            "ManagementServerDirUsedCapacityInPercent", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementPrometheusUsedCapacityInBytes = new ByteSizeMetric(
            "ManagementPrometheusUsedCapacityInBytes", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementZStackLogCapacityInBytes = new ByteSizeMetric(
            "ManagementZStackLogCapacityInBytes", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementZStackDBBackupCapacityInBytes = new ByteSizeMetric(
            "ManagementZStackDBBackupCapacityInBytes", metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath);
    public static final Metric ManagementMysqlCapacityInBytes = new ByteSizeMetric("ManagementMysqlCapacityInBytes",
            metrics, LabelNames.ManagementNodeIP, LabelNames.DirPath
    );

    public SystemNamespace() {
    }

    public SystemNamespace(DatabaseDriver driver) {
        super(driver);
    }

    @Override
    protected String getSubNamespaceName() {
        return NAME;
    }

    @Override
    public List<Metric> getMetrics() {
        return metrics.stream().filter(m ->!disableMetrics.contains(m.getName())).collect(Collectors.toList());
    }

    @Override
    public List<EventFamily> getEvents() {
        return null;
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }
}
