package org.zstack.zwatch.prometheus;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.zwatch.datatype.*;

import java.util.*;
import java.util.stream.Collectors;

public interface PrometheusNamespace {
    Collection<RecordingRule> getRecordingRules();

    List<Datapoint> query(MetricQueryObject queryObject);

    List<Map> queryLabelValues(LabelValueQueryObject qo);

    Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo);

    boolean deleteMetrics(String metricName, List<Label> labels);

    Map<Class, Class> namespacesClasses = new HashMap<>();

    static PrometheusNamespace getPrometheusNamespace(Namespace ns) {
        Class nsClz = namespacesClasses.get(ns.getClass());
        if (nsClz == null) {
            throw new CloudRuntimeException(String.format("cannot find PrometheusNamespace for the Namespace class[%s]", ns.getClass()));
        }

        try {
            return (PrometheusNamespace) nsClz.getConstructor(Namespace.class).newInstance(ns);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    String SERIES_NAME_SPLITTER = "::";
    static String makeSeriesName(String namespaceName, String metricName) {
        return String.format("%s%s%s", namespaceName.replace("/", ":"), SERIES_NAME_SPLITTER, metricName);
    }

    static List<String> getMetricMatches(String namespaceName, Set<String> metricNames, List<Label> labels) {
        final String labelString;
        if (labels != null && !labels.isEmpty()) {
            List<String> labelStringList = labels.stream()
                    .map(label -> String.format("%s%s\"%s\"", label.getKey(), label.getOp(), label.getValue()))
                    .collect(Collectors.toList());
            labelString = StringUtils.join(labelStringList, ",");
        } else {
            labelString = null;
        }

        return metricNames.stream().map(metricName -> {
            String seriesName = PrometheusNamespace.makeSeriesName(namespaceName, metricName);
            return (labelString == null) ? seriesName : String.format("%s{%s}", seriesName, labelString);
        }).collect(Collectors.toList());
    }
}
