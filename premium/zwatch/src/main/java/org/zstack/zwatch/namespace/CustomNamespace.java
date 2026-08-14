package org.zstack.zwatch.namespace;

import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.api.APIGetAllMetricMetadataReply;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomNamespace implements Namespace {
    public static String NAME = "Custom";
    public static String INSTANCE_LABEL_NAME = "zstack_instance";

    private DatabaseDriver driver;

    public enum LabelNames {
        AccountUUID
    }

    public CustomNamespace(DatabaseDriver driver) {
        this.driver = driver;
    }

    public List<APIGetAllMetricMetadataReply.MetricStruct> getAllMetricMetadata(String accountUuid) {
        Map<String, String> labels = new HashMap<>();
        labels.put(INSTANCE_LABEL_NAME, NAME);
        labels.put(LabelNames.AccountUUID.toString(), accountUuid);
        return driver.getAllNonZStackMetricMetadata(labels);
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        if (queryObject != null) {
            queryObject.getLabels().add(new Label(String.format("%s=%s", LabelNames.AccountUUID, queryObject.getAccountUuid())));
        }

        return driver.query(queryObject);
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        if (qo.getAccountUuid() != null) {
            qo.getFilteredLabels().add(new Label(String.format("%s=%s", LabelNames.AccountUUID, qo.getAccountUuid())));
        }

        return driver.queryLabelValues(qo);
    }

    @Override
    public Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo) {
        if (qo.getAccountUuid() != null) {
            qo.getFilteredLabels().add(new Label(String.format("%s=%s", LabelNames.AccountUUID, qo.getAccountUuid())));
        }

        return driver.queryPrometheusLabelValues(qo);
    }

    @Override
    public boolean deleteAll(String metricName, List<Label> labels) {
        return driver.deleteAll(getName(), metricName, labels);
    }

    @Override
    public DatabaseDriver getDatabaseDriver() {
        return driver;
    }

    public void write(String namespaceName, List<MetricDatum> data, String accountUuid) {
        long time = System.currentTimeMillis();
        data.forEach(d -> {
            d.getLabels().put(LabelNames.AccountUUID.toString(), accountUuid);
            d.getLabels().put(INSTANCE_LABEL_NAME, NAME);
            if (d.getTime() == null) {
                d.setTime(time);
            }
        });
        driver.write(namespaceName, data);
    }

    @Override
    public boolean hasMetric(String metricName) {
        return true;
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Override
    public String getIdentityLabelName() {
        return null;
    }

    @Override
    public String getResourceNameIfNull(AlarmAction.TakeAlarmActionParam param) {
        return null;
    }

    @Override
    public String getName() {
        return Namespace.zstackNamespaceName(NAME);
    }

    @Override
    public List<Metric> getMetrics() {
        return null;
    }

    @Override
    public List<EventFamily> getEvents() {
        return null;
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
}
