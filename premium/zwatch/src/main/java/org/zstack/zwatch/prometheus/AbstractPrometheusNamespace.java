package org.zstack.zwatch.prometheus;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractPrometheusNamespace implements PrometheusNamespace {
    protected static final CLogger logger = Utils.getLogger(AbstractPrometheusNamespace.class);

    @Autowired
    protected PrometheusFactory prometheusFactory;

    protected Namespace namespace;
    protected Map<String, RecordingRule> recordingRules = new HashMap<>();
    protected Map<String, Metric> metrics = new HashMap<>();

    protected abstract RecordingRule createRecordingRule(Metric metric);

    public AbstractPrometheusNamespace(Namespace namespace) {
        this.namespace = namespace;

        if (namespace.getMetrics() != null) {
            namespace.getMetrics().forEach(m -> {
                RecordingRule rule = createRecordingRule(m);
                if (!rule.isForLabelMappingOnly()) {
                    DebugUtils.Assert(rule.getSeriesName() != null, String.format("seriesName cannot be null, metric[%s]", m.getName()));
                    DebugUtils.Assert(rule.getExpression() != null, String.format("expression cannot be null, metric[%s]", m.getName()));
                }
                recordingRules.put(m.getName(), rule);

                metrics.put(m.getName(), m);
            });
        }

        if (PrometheusGlobalProperty.ENABLE_INFO_LABEL_ENRICHMENT) {
            refreshInfoLabelEnrichment();
        }
    }

    protected String getNamespaceName() {
        return namespace.getName();
    }

    protected String makeSeriesName(String metricName) {
        return PrometheusNamespace.makeSeriesName(getNamespaceName(), metricName);
    }

    @Override
    public Collection<RecordingRule> getRecordingRules() {
        Metric infoMetric = namespace.getInfoMetric();
        if (infoMetric == null || PrometheusGlobalProperty.ENABLE_INFO_LABEL_ENRICHMENT) {
            return recordingRules.values();
        }

        String infoSeries = makeSeriesName(infoMetric.getName());
        return recordingRules.values().stream()
                .filter(r -> !infoSeries.equals(r.getSeriesName()))
                .collect(Collectors.toList());
    }

    private RecordingRule getRecordingRule(String metricName) {
        RecordingRule r = recordingRules.get(metricName);
        if (r == null) {
            throw new CloudRuntimeException(String.format("cannot find RecordingRule for the metric[%s]", metricName));
        }
        return r;
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        RecordingRule rule = getRecordingRule(qo.getMetricName());
        String seriesName = getSeriesName(qo.getMetricName(), rule);
        List<String> ls = qo.getFilteredLabels().stream()
                .map(l -> getLable(l, rule))
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder(seriesName);
        joinLabels(ls, sb);
        String query = sb.toString();
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
                String key = rule.findMappedLabelName(l);
                String value = m.get(key);

                if (value != null) {
                    mm.put(l, value);
                    identity.add(l + value);
                }
            });
            qo.getFilteredLabels().forEach(l -> {
                String key = rule.findMappedLabelName(l.getKey());
                String value = m.get(key);

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
        RecordingRule rule = getRecordingRule(qo.getMetricName());
        String seriesName = getSeriesName(qo.getMetricName(), rule);
        List<String> ls = qo.getFilteredLabels().stream()
                .map(l -> getLable(l, rule))
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder(seriesName);
        joinLabels(ls, sb);
        String query = sb.toString();
        Map<String, String> params = new HashMap<>();
        params.put("match[]", query);
        if (qo.getStartTime() != null && qo.getEndTime() != null) {
            params.put("start", String.valueOf(qo.getStartTime()));
            params.put("end", String.valueOf(qo.getEndTime()));
        }

        Map<String, List<String>> resultMap = new LinkedHashMap<>();
        for (String labelName : qo.getLabelNames()) {
            Prometheus prometheus = prometheusFactory.getPrometheus();
            String uri = qo.getLabelNames().isEmpty()
                    ? Prometheus.HTTP_LABEL_ALL_PATH
                    : String.format(Prometheus.HTTP_LABEL_DETAIL_PATH, rule.findMappedLabelName(labelName));
            Prometheus.APIQueryLabelValueStruct ret = prometheus.apiCall(uri,
                    params,
                    Prometheus.APIQueryLabelValueStruct.class);
            resultMap.put(labelName, ret.data);
        }
        return resultMap;
    }

    private String getSeriesName(String metricName, final RecordingRule rule) {
        if (!rule.isSkipRecord()) {
            return rule.getSeriesName() == null ? makeSeriesName(metricName) : rule.getSeriesName();
        }

        return rule.getExpression();
    }

    private String getLable(Label l, final RecordingRule rule) {
        if (!rule.isSkipRecord() || !rule.getLabelMapping().isEmpty()) {
            if (rule.getLabelMapping().get(l.getKey()) != null) {
                return String.format("%s%s\"%s\"", rule.getLabelMapping().get(l.getKey()), l.getOp(), l.getValue());
            }
        }
        return String.format("%s%s\"%s\"", l.getKey(), l.getOp(), l.getValue());
    }

    private void joinLabels(final List<String> labels, StringBuilder builder) {
        if (labels.isEmpty()) {
            return;
        }
        if (!builder.toString().contains("{")) {
            builder.append("{");
            builder.append(StringUtils.join(labels, ","));
            builder.append("}");
        } else {
            int index = builder.indexOf("{");
            int offset;
            while (index != -1) {
                String condition = StringUtils.join(labels, ",");
                builder.insert(index + 1, condition + ",");
                offset = index + 1 + condition.length() + 1;
                index = builder.indexOf("{", offset);
            }
        }
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        Prometheus prometheus = prometheusFactory.getPrometheus();
        List<Datapoint> data = new ArrayList<>();
        if (prometheus == null) {
            return data;
        }

        RecordingRule rule = getRecordingRule(queryObject.getMetricName());
        Map<String, String> params = new HashMap<>();
        params.put("start", String.valueOf(queryObject.getStartTime()));
        params.put("end", String.valueOf(queryObject.getEndTime()));
        params.put("step", String.format("%ss", queryObject.getPeriod()));

        String seriesName = getSeriesName(queryObject.getMetricName(), rule);
        StringBuilder sb = new StringBuilder(seriesName);
        if (!queryObject.getLabels().isEmpty()) {
            List<String> ls = queryObject.getLabels().stream()
                    .map(l -> getLable(l, rule))
                    .collect(Collectors.toList());
            joinLabels(ls, sb);
        }

        if (queryObject.getAggregationOp() != null) {
            sb.insert(0, '(').insert(0, queryObject.getAggregationOp()).append(')');
        }

        params.put("query", sb.toString());
        Prometheus.APIRangeQueryStruct r = prometheus.apiCall(false, params, Prometheus.APIRangeQueryStruct.class);

        Set<String> infoLabels = PrometheusGlobalProperty.ENABLE_INFO_LABEL_ENRICHMENT ?
                namespace.getInfoLabelNames() : Collections.emptySet();

        r.data.result.forEach(d -> {
            Metric metric = metrics.get(queryObject.getMetricName());
            List<String> activeLabels = metric == null ? Collections.emptyList() : namespace.getEffectiveMetricLabelNames(metric);

            Map<String, String> returnLabels = new HashMap<>();
            d.metric.forEach((k, v)  -> {
                String originalKey = k;
                String overridenLabelname = rule.getLabelMapping().get(k);
                if (overridenLabelname != null) {
                    if (metric != null && activeLabels.contains(overridenLabelname)) {
                        k = overridenLabelname;
                    } else if (metric != null && activeLabels.contains(originalKey)) {
                        k = originalKey;
                    } else {
                        k = overridenLabelname;
                    }
                }

                if (metric == null
                        || activeLabels.contains(k)
                        || activeLabels.contains(originalKey)
                        || infoLabels.contains(k)
                        || infoLabels.contains(originalKey)) {
                    returnLabels.put(k, v);
                }
            });

            d.values.forEach(v -> {
                Datapoint dp = new Datapoint();
                dp.setLabels(returnLabels);
                Double value = Double.valueOf((String) v.get(1));
                dp.setValue(Double.isNaN(value) ? 0.00 : value);
                dp.setTime(((long) v.get(0)));
                data.add(dp);
            });
        });

        return data.stream().sorted(Comparator.comparing(Datapoint::getTime)).collect(Collectors.toList());
    }

    @Override
    public boolean deleteMetrics(String metricName, List<Label> labels) {
        Set<String> metricNames;
        String namespaceName = getNamespaceName();
        if (StringUtils.isEmpty(metricName) || StringUtils.equals(metricName, "all")) {
            metricNames = metrics.keySet();
        } else {
            metricNames = Collections.singleton(metricName);
        }

        if (CollectionUtils.isNotEmpty(labels)) {
            Map<String, String> lableMapping = new HashMap<>();
            for (String metric : metricNames) {
                lableMapping.putAll(getRecordingRule(metric).getLabelMapping());
            }

            labels.forEach(v -> v.setKey(lableMapping.containsValue(v.getKey()) ?
                    lableMapping.get(v.getKey()) : v.getKey()));

        }

        List<String> matches = PrometheusNamespace.getMetricMatches(namespaceName, metricNames, labels);
        matches.forEach(match -> logger.debug(String.format("deleteMetrics(\"%s\") ...", match)));
        Prometheus prometheus = prometheusFactory.getPrometheus();

        List<List<String>> splitLists = IntStream.range(0, matches.size())
                .boxed()
                .collect(Collectors.groupingBy(index -> index / 50))
                .values()
                .stream()
                .map(indices -> indices.stream().map(matches::get).collect(Collectors.toList()))
                .collect(Collectors.toList());

        for (List<String> sublist : splitLists) {
            if(!prometheus.apiDelete(sublist)) {
                return false;
            }
        }
        return true;
    }

    public void refreshInfoLabelEnrichment() {
        String infoMetricName = getInfoMetricName();
        if (infoMetricName == null) {
            return;
        }

        String infoSeries = makeSeriesName(infoMetricName);
        String identityLabel = namespace.getBasicIdentityLabelName();
        if (StringUtils.isEmpty(identityLabel)) {
            return;
        }

        recordingRules.forEach((metricName, rule) -> {
            if (metricName.equals(infoMetricName) || rule.isForLabelMappingOnly()) {
                return;
            }

            Set<String> infoLabels = namespace.getInfoLabelNames();
            if (infoLabels.isEmpty()) {
                return;
            }

            Metric metric = metrics.get(metricName);
            if (metric == null) {
                return;
            }

            if (StringUtils.contains(rule.getExpression(), infoSeries)) {
                return;
            }

            Set<String> metricLabels = new HashSet<>(metric.getLabelNames());
            Set<String> missingInfoLabels = infoLabels.stream()
                    .filter(label -> !metricLabels.contains(label))
                    .collect(Collectors.toSet());
            if (missingInfoLabels.isEmpty()) {
                return;
            }

            String promIdentityLabel = determinePrometheusIdentityLabel(rule, identityLabel);
            if (StringUtils.isEmpty(rule.getExpression())) {
                return;
            }

            String baseExpressionWithIdentity = alignSeriesIdentity(rule.getExpression(), identityLabel, promIdentityLabel);

            List<String> infoJoinLabels = missingInfoLabels.stream()
                    .filter(l -> !Objects.equals(l, identityLabel))
                    .collect(Collectors.toList());
            String infoLabelString = String.join(", ", infoJoinLabels);
            String groupLeftClause = infoJoinLabels.isEmpty() ? "" : String.format(" group_left(%s)", infoLabelString);

            String expr = String.format("%s * on (%s)%s %s",
                    baseExpressionWithIdentity,
                    identityLabel,
                    groupLeftClause,
                    infoSeries);
            rule.setExpression(expr);

            Map<String, String> mapping = rule.getLabelMapping();
            if (mapping != null) {
                infoLabels.forEach(label -> mapping.put(label, label));
                if (identityLabel != null) {
                    mapping.put(identityLabel, identityLabel);
                }
            }
        });
    }

    protected String getInfoMetricName() {
        Metric infoMetric = namespace.getInfoMetric();
        if (infoMetric == null || infoMetric.getName() == null) {
            return null;
        }

        return infoMetric.getName();
    }

    private String determinePrometheusIdentityLabel(RecordingRule baseRule, String identityLabel) {
        if (identityLabel == null || baseRule == null || baseRule.getLabelMapping() == null) {
            return identityLabel;
        }

        Map<String, String> mapping = baseRule.getLabelMapping();

        String mapped = mapping.get(identityLabel);
        if (mapped != null) {
            return mapped;
        }

        return mapping.entrySet().stream()
                .filter(entry -> identityLabel.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(identityLabel);
    }

    private String alignSeriesIdentity(String baseSeries, String identityLabel, String promIdentityLabel) {
        if (identityLabel == null || promIdentityLabel == null || identityLabel.equals(promIdentityLabel)) {
            return baseSeries;
        }

        String renamedSeries = String.format("label_replace(%s, \"%s\", \"$1\", \"%s\", \"(.*)\")",
                baseSeries, identityLabel, promIdentityLabel);

        return String.format("label_replace(%s, \"%s\", \"\", \"%s\", \".*\")",
                renamedSeries, promIdentityLabel, promIdentityLabel);
    }
}
