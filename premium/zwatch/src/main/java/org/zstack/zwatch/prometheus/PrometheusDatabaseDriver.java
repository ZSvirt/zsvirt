package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.premium.externalservice.prometheus.*;
import org.zstack.premium.externalservice.prometheus.pushgateway.PushGateway;
import org.zstack.premium.externalservice.prometheus.pushgateway.PushGatewayFactory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.api.APIGetAllMetricMetadataReply;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.CustomNamespace;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PrometheusDatabaseDriver implements Component, DatabaseDriver, PreparePrometheusConfigExtensionPoint {
    private volatile Map<String, PrometheusNamespace> pnamespaces = Collections.emptyMap();

    @Autowired
    protected PushGatewayFactory pushGatewayFactory;
    @Autowired
    protected PrometheusFactory prometheusFactory;

    public static Set<String> EXCLUDED_LABEL_NAMES = new HashSet<>();

    private List<String> blacklists = getBlacklists();

    static {
        EXCLUDED_LABEL_NAMES.add(CustomNamespace.INSTANCE_LABEL_NAME);
        EXCLUDED_LABEL_NAMES.add("__name__");
        EXCLUDED_LABEL_NAMES.add("__instance__");
        EXCLUDED_LABEL_NAMES.add("job");
        EXCLUDED_LABEL_NAMES.add("instance");
    }

    private List<String> getBlacklists(){
        List<String> blackList = new ArrayList<>();
        if (PrometheusGlobalProperty.PROMETHEUS_EVALUATION_BLACKLIST != null) {
            for (String sub: PrometheusGlobalProperty.PROMETHEUS_EVALUATION_BLACKLIST.split(",")) {
                blackList.add(Namespace.ZSTACK_NAMESPACE_PREFIX + ":" + sub);
            }
        }
        return blackList;
    }

    @Override
    public boolean deleteAll(String namespaceName, String metricName, List<Label> labels) {
        PrometheusNamespace ns = pnamespaces.get(namespaceName);
        if (ns == null) {
            ns = new CustomPrometheusNamespace(namespaceName);
        }

        return ns.deleteMetrics(metricName, labels);
    }

    @Override
    public List<Datapoint> query(MetricQueryObject queryObject) {
        PrometheusNamespace ns = pnamespaces.get(queryObject.getNamespaceName());
        if (ns == null) {
            return customQuery(queryObject.getNamespaceName(), queryObject);
        }

        return ns.query(queryObject);
    }

    private List<Datapoint> customQuery(String namespaceName, MetricQueryObject queryObject) {
        return new CustomPrometheusNamespace(namespaceName).query(queryObject);
    }

    @Override
    public List<Map> queryLabelValues(LabelValueQueryObject qo) {
        PrometheusNamespace ns = pnamespaces.get(qo.getNamespaceName());
        if (ns == null) {
            return customQuery(qo.getNamespaceName(), qo);
        }
        return ns.queryLabelValues(qo);
    }

    @Override
    public Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo) {
        PrometheusNamespace ns = pnamespaces.get(qo.getNamespaceName());
        if (ns == null) {
            return new CustomPrometheusNamespace(qo.getNamespaceName())
                    .queryPrometheusLabelValues(qo);
        }
        return ns.queryPrometheusLabelValues(qo);
    }

    @Override
    public List<String> getAllMetricNames() {
        Prometheus prometheus = prometheusFactory.getPrometheus();
        Prometheus.APIQueryLabelValueStruct ret = prometheus.apiCall("/api/v1/label/__name__/values", new HashMap(), Prometheus.APIQueryLabelValueStruct.class);
        return ret.data;
    }

    private List<Map> customQuery(String namespaceName, LabelValueQueryObject qo) {
        return new CustomPrometheusNamespace(namespaceName).queryLabelValues(qo);
    }

    @Override
    public boolean start() {
        rebuildPrometheusNamespaces();

        ZWatchGlobalConfig.SCRAPE_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            Prometheus prometheus = prometheusFactory.getPrometheus();
            prometheus.getConfig().getGlobal().setScrape_interval(String.format("%ss", newConfig.value()));
            prometheus.rewriteConfigAndReload();
        });

        return true;
    }

    public void rebuildPrometheusNamespaces() {
        Map<String, PrometheusNamespace> rebuilt = new HashMap<>();
        Namespace.namespaces.forEach((name, namespaces) -> {
            namespaces.forEach(ns -> {
                if (ns.getDatabaseDriver() instanceof PrometheusDatabaseDriver && ns.getMetrics() != null && !ns.getMetrics().isEmpty()) {
                    PrometheusNamespace pns = PrometheusNamespace.getPrometheusNamespace(ns);
                    if (PrometheusGlobalProperty.ENABLE_INFO_LABEL_ENRICHMENT && pns instanceof AbstractPrometheusNamespace) {
                        ((AbstractPrometheusNamespace) pns).refreshInfoLabelEnrichment();
                    }
                    rebuilt.put(name, pns);
                }
            });
        });

        pnamespaces = Collections.unmodifiableMap(rebuilt);
    }

    @Override
    public boolean stop() {
        return true;
    }

    public static String toYamlExpression(String rule) {
        String[] r = rule.split(RecordingRule.expressionSpliter);
        DebugUtils.Assert(r.length == 2, String.format("found invalid prometheus rule expression format: %s", rule));
        return "  - record: " + r[0] + "\n" + "    expr: " + r[1];
    }

    private void writeConfig(final File ruleFile, final List<String> rules) throws IOException {
        String headStr = "groups:\n- name: zwatch.rule\n  rules:";
        List<String> yamlRules = new ArrayList<>();
        yamlRules.add(headStr);
        for (String rule: rules) {
            if (!rule.trim().isEmpty()) {
                yamlRules.add(toYamlExpression(rule.trim()));
            }
        }
        FileUtils.writeStringToFile(ruleFile, StringUtils.join(yamlRules, "\n"));
    }

    private boolean isBlocked(String seriesName) {
        for (String blacklist: blacklists) {
            if (blacklist.equals(seriesName.split(PrometheusNamespace.SERIES_NAME_SPLITTER)[0])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepareConfig(PrometheusConfig config) {
        File ruleFile = new File(PathUtil.join(Prometheus.RULES_ROOT, PrometheusConstant.ZWATCH_RULE_NAME));
        File skipRuleFile = new File(PathUtil.join(Prometheus.RULES_ROOT, PrometheusConstant.ZWATCH_RULE_NAME+".skip"));
        File blackListFile = new File(PathUtil.join(Prometheus.RULES_ROOT, PrometheusConstant.ZWATCH_RULE_NAME+".blacklist"));
        try {
            List<String> rules = new ArrayList<>();
            List<String> skipRules = new ArrayList<>();
            List<String> blackListRules = new ArrayList<>();
            pnamespaces.values().forEach(it -> rules.addAll(it.getRecordingRules()
                    .stream().filter(i->!i.isForLabelMappingOnly()).filter(i -> !i.isSkipRecord()).filter(i -> !isBlocked(i.getSeriesName()))
                    .map(RecordingRule::toString).collect(Collectors.toList())));

            pnamespaces.values().forEach(it -> skipRules.addAll(it.getRecordingRules()
                    .stream().filter(i->!i.isForLabelMappingOnly()).filter(RecordingRule::isSkipRecord)
                    .map(RecordingRule::toString).collect(Collectors.toList())));

            if (!blacklists.isEmpty()) {
                pnamespaces.values().forEach(it -> blackListRules.addAll(it.getRecordingRules()
                        .stream().filter(i->!i.isForLabelMappingOnly()).filter(i -> isBlocked(i.getSeriesName()))
                        .map(RecordingRule::toString).collect(Collectors.toList())));
            }


            boolean ruleFileChanged;
            if (!ruleFile.exists()) {
                ruleFileChanged = true;
            } else {
                String newConfig = StringUtils.join(rules, "\n");
                String oldConfig = FileUtils.readFileToString(ruleFile);
                String differ = StringUtils.difference(newConfig, oldConfig);
                ruleFileChanged = StringUtils.isNotBlank(differ);
            }

            boolean finalRuleFileChanged = ruleFileChanged;
            config.rebootPrometheusWhenManagmentNodeStartIf(()-> finalRuleFileChanged);

            if (!rules.isEmpty()) {
                writeConfig(ruleFile, rules);
            }
            if (!skipRules.isEmpty()) {
                writeConfig(skipRuleFile, skipRules);
            } else {
                FileUtils.forceDeleteOnExit(skipRuleFile);
            }
            if (!blackListRules.isEmpty()) {
                writeConfig(blackListFile, blackListRules);
            } else {
                FileUtils.forceDeleteOnExit(blackListFile);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        config.ruleFile(ruleFile.getAbsolutePath());
        config.getGlobal().setScrape_interval(String.format("%ss", ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class)));
    }

    public void write(String namespaceName, List<MetricDatum> data) {
        PushGateway gateway = pushGatewayFactory.getPushGateway();
        List<PushGateway.Data> dps = data.stream().map(d-> {
            PushGateway.Data pd = new PushGateway.Data();
            pd.labels = d.getLabels();
            pd.metricName = PrometheusNamespace.makeSeriesName(namespaceName, d.getMetricName());
            pd.time = d.getTime();
            pd.value = d.getValue();
            return pd;
        }).collect(Collectors.toList());

        gateway.push(dps);
    }

    public List<APIGetAllMetricMetadataReply.MetricStruct> getAllNonZStackMetricMetadata(Map<String, String> filteredLabels) {
        List<String> metricNames = getAllMetricNames();
        List<String> labels = new ArrayList<>();
        filteredLabels.forEach((k, v)-> labels.add(String.format("%s=\"%s\"", k, v)));

        List<String> matches = new ArrayList<>();
        metricNames.forEach(m-> matches.add(String.format("%s{%s}", m, StringUtils.join(labels, ","))));

        Map params = new HashMap();
        params.put("match[]", matches);

        Prometheus prometheus = prometheusFactory.getPrometheus();
        Prometheus.APIQueryMetadataStruct ret = prometheus.apiCall(Prometheus.HTTP_SERIES_PATH, params, Prometheus.APIQueryMetadataStruct.class);

        List<APIGetAllMetricMetadataReply.MetricStruct> res = new ArrayList<>();
        ret.data.forEach(m -> {
            String seriesName = m.get("__name__");
            String[] pair = seriesName.split("::");
            String namespace = pair[0];
            String metricName = pair[1];
            APIGetAllMetricMetadataReply.MetricStruct metricStruct = new APIGetAllMetricMetadataReply.MetricStruct();
            metricStruct.namespace = namespace;
            metricStruct.name = metricName;
            EXCLUDED_LABEL_NAMES.forEach(m::remove);
            metricStruct.labelNames = new ArrayList<>(m.keySet());
            res.add(metricStruct);
        });

        return res;
    }
}
