package org.zstack.zwatch.prometheus;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.LabelValueQueryObject;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CustomPrometheusNamespace implements PrometheusNamespace {
    protected static final CLogger logger = Utils.getLogger(CustomPrometheusNamespace.class);
    private final String namespaceName;

    @Autowired
    protected PrometheusFactory prometheusFactory;

    public CustomPrometheusNamespace(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    @Override
    public Collection<RecordingRule> getRecordingRules() {
        return null;
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        Map<String, String> params = new HashMap<>();
        params.put("start", String.valueOf(queryObject.getStartTime()));
        params.put("end", String.valueOf(queryObject.getEndTime()));
        params.put("step", String.format("%ss", queryObject.getPeriod()));

        String seriesName = PrometheusNamespace.makeSeriesName(namespaceName, queryObject.getMetricName());
        StringBuilder sb = new StringBuilder(seriesName);
        if (!queryObject.getLabels().isEmpty()) {
            List<String> ls = queryObject.getLabels().stream().map(l -> String.format("%s%s\"%s\"",
                    l.getKey(), l.getOp(), l.getValue())).collect(Collectors.toList());
            sb.append("{");
            sb.append(StringUtils.join(ls, ","));
            sb.append("}");
        }

        if (queryObject.getAggregationOp() != null) {
            sb.insert(0, '(').insert(0, queryObject.getAggregationOp()).append(')');
        }

        params.put("query", sb.toString());

        Prometheus prometheus = prometheusFactory.getPrometheus();
        Prometheus.APIRangeQueryStruct r = prometheus.apiCall(false, params, Prometheus.APIRangeQueryStruct.class);

        List<Datapoint> data = new ArrayList<>();
        r.data.result.forEach(d -> {
            Map<String, String> returnLabels = new HashMap<>();
            d.metric.forEach((k, v)  -> {
                if (!PrometheusDatabaseDriver.EXCLUDED_LABEL_NAMES.contains(k)) {
                    returnLabels.put(k, v);
                }
            });

            d.values.forEach(v -> {
                Datapoint dp = new Datapoint();
                dp.setLabels(returnLabels);
                dp.setValue(Double.valueOf((String) v.get(1)));
                long time = ((Number) v.get(0)).longValue();
                dp.setTime(time);
                data.add(dp);
            });
        });

        return data;
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        String seriesName = PrometheusNamespace.makeSeriesName(namespaceName, qo.getMetricName());
        List<String> ls = qo.getFilteredLabels().stream()
                .map(l -> String.format("%s%s\"%s\"", l.getKey(), l.getOp(), l.getValue()))
                .collect(Collectors.toList());
        String query = String.format("%s{%s}", seriesName, StringUtils.join(ls, ","));
        Map<String, String> params = new HashMap<>();
        params.put("match[]", query);
        if (qo.getStartTime() != null && qo.getEndTime() != null) {
            params.put("start", String.valueOf(qo.getStartTime()));
            params.put("end", String.valueOf(qo.getEndTime()));
        }

        Prometheus prometheus = prometheusFactory.getPrometheus();
        Prometheus.APIQueryMetadataStruct ret = prometheus.apiCall(Prometheus.HTTP_SERIES_PATH, params, Prometheus.APIQueryMetadataStruct.class);
        Map<List, Map> result = new HashMap<>();
        ret.data.forEach(m -> {
            List<String> identity = new ArrayList<>();
            Map mm = new HashMap();
            qo.getLabelNames().forEach(l-> {
                String value = m.get(l);

                if (value != null) {
                    mm.put(l, value);
                    identity.add(l + value);
                }
            });
            qo.getFilteredLabels().forEach(l -> {
                String value = m.get(l.getKey());

                if (value != null) {
                    mm.put(l.getKey(), value);
                    identity.add(l.getKey() + value);
                }
            });

            if (!mm.isEmpty()) {
                Collections.sort(identity);
                result.put(identity, mm);
            }
        });

        return new ArrayList<>(result.values());
    }

    @Override
    public Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo) {
        throw new UnsupportedOperationException("CustomPrometheusNamespace does not support queryPrometheusLabelValues yet");
    }

    @Override
    public boolean deleteMetrics(String metricName, List<Label> labels) {
        Set<String> metricNames;
        if (StringUtils.isEmpty(metricName) || StringUtils.equals(metricName, "all")) {
            Collection<RecordingRule> rules = this.getRecordingRules();
            if (rules == null || rules.isEmpty()) {
                return true;
            }
            metricNames = rules.stream()
                    .map(rule -> rule.getSeriesName())
                    .collect(Collectors.toSet());
        } else {
            metricNames = Collections.singleton(metricName);
        }

        List<String> matches = PrometheusNamespace.getMetricMatches(namespaceName, metricNames, labels);
        matches.forEach(match -> logger.debug(String.format("deleteMetrics(\"%s\") ...", match)));
        Prometheus prometheus = prometheusFactory.getPrometheus();
        return prometheus.apiDelete(matches);
    }
}
