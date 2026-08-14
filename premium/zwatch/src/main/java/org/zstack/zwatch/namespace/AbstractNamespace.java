package org.zstack.zwatch.namespace;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalProperty;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public abstract class AbstractNamespace implements Namespace {
    private static final CLogger logger = Utils.getLogger(AbstractNamespace.class);
    protected DatabaseDriver driver;

    public AbstractNamespace() {
        namespaces.computeIfAbsent(getName(), key -> new CopyOnWriteArrayList<>()).add(this);
    }

    public AbstractNamespace(DatabaseDriver driver) {
        this();
        this.driver = driver;
    }

    @Override
    public DatabaseDriver getDatabaseDriver() {
        return driver;
    }

    public String getName() {
        return Namespace.zstackNamespaceName(getSubNamespaceName());
    }

    public List<Datapoint> query(MetricQueryObject queryObject) {
        if (!hasMetric(queryObject.getMetricName())) {
            throw new OperationFailureException(argerr("namespace[%s] has no metric[%s]", getName(), queryObject.getMetricName()));
        }

        // Try to get Metric object from getMetrics() list for label validation
        Optional<Metric> opt = getMetrics().stream().filter(m -> m.getName().equals(queryObject.getMetricName())).findAny();
        opt.ifPresent(m -> {
            List<String> effectiveLabels = getEffectiveMetricLabelNames(m);
            queryObject.getLabels().forEach(l -> {
                if (!effectiveLabels.contains(l.getKey())) {
                    throw new OperationFailureException(argerr("metric[%s] of the namespace[%s] has no label named %s", m.getName(), getName(), l.getKey()));
                }
            });
        });

        return doQuery(queryObject);
    }

    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        return driver.queryLabelValues(qo);
    }

    @Override
    public Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo) {
        return driver.queryPrometheusLabelValues(qo);
    }

    protected List<Datapoint> doQuery(MetricQueryObject queryObject) {
        logger.trace(String.format("query namespace[%s] with query object:\n%s", getName(), JSONObjectUtil.dumpPretty(queryObject)));

        List<Datapoint> datapointList = driver.query(queryObject);

        if (logger.isTraceEnabled() && datapointList != null) {
            logger.trace(String.format("dump namespace queried data points:\n%s",
                    JSONObjectUtil.dumpPretty(datapointList)));
        }

        return datapointList;
    }

    @Override
    public boolean deleteAll(String metricName, List<Label> labels) {
        return driver.deleteAll(getName(), metricName, labels);
    }

    protected abstract String getSubNamespaceName();

    public boolean hasMetric(String metricName) {
        return getMetrics().stream().anyMatch(m -> m.getName().equals(metricName));
    }

    protected static List<String> getDisableMetrics(String name) {
        List<String> result = new ArrayList<>();
        String disableString = ZWatchGlobalProperty.DISABLE_METRICS_LIST.replace("\"", "");
        List<String> disables = CollectionDSL.list(disableString.split(","));
        if (!disables.isEmpty()) {
            disables.stream().filter(it -> it.contains("::")).forEach(it -> {
                if (it.split("::")[0].trim().equals(name)) {
                    logger.debug(String.format("found disable metrics: %s", it));
                    result.add(it.split("::")[1].trim());
                }
            });
        }
        return result;
    }

    @Override
    public List<Map> filterLabelValues(List<Map> result, LabelValueQueryObject qo) {
        Metric metric = getMetrics().stream().filter(m -> m.getName().equals(qo.getMetricName())).findFirst().orElse(null);
        if (metric != null && !qo.getFilters().isEmpty()) {
            for (Label l : qo.getFilters()) {
                result = result.stream().filter(m -> metric.filterLabelValue(l.getValue(), m)).collect(Collectors.toList());
            }
        }

        return result;
    }

    @Override
    public String getResourceNameIfNull(AlarmAction.TakeAlarmActionParam param) {
        return null;
    }
}
