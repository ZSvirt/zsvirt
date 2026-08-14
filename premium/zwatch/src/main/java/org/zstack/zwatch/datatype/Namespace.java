package org.zstack.zwatch.datatype;

import org.zstack.core.Platform;
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;
import org.zstack.utils.CollectionUtils;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.datatype.metric.InfoMetric;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.DatabaseDriver;
import org.zstack.zwatch.namespace.CustomNamespace;
import org.zstack.zwatch.prometheus.PrometheusDatabaseDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public interface Namespace extends MultiTypeNamespace {
    Map<String, List<Namespace>> namespaces = new ConcurrentHashMap<>();

    static CustomNamespace getCustomNamespace() {
        return Platform.getComponentLoader().getComponent(CustomNamespace.class);
    }

    static Namespace getNamespaceByName(String name) {
        List<Namespace> namespaceList = namespaces.get(name);
        if (CollectionUtils.isEmpty(namespaceList)) {
            return null;
        }
        return namespaceList.get(0);
    }

    static Namespace getMetricNameSpace(String name, String metric) {
        Namespace ns = findNamespaceByMetric(name, metric);
        if (ns == null) {
            //throw new CloudRuntimeException(String.format("cannot find namespace[%s]", name));
            return getCustomNamespace();
        }

        return ns;
    }

    static Namespace getEventNameSpace(String name, String event) {
        Namespace ns = findNamespaceByEvent(name, event);
        if (ns == null) {
            //throw new CloudRuntimeException(String.format("cannot find namespace[%s]", name));
            return getCustomNamespace();
        }

        return ns;
    }

    static Namespace findNamespaceByEvent(String name, String event) {
        List<Namespace> namespaceList = namespaces.get(name);

        if (namespaceList == null) {
            return null;
        }

        Optional<Namespace> opt = namespaceList.stream()
                .filter(ns -> ns.getEvents().stream().anyMatch(e -> e.getName().equals(event)))
                .findAny();

        return opt.orElse(null);
    }

    static Namespace findNamespaceByMetric(String name, String metric) {
        List<Namespace> namespaceList = namespaces.get(name);

        if (namespaceList == null) {
            return null;
        }

        Optional<Namespace> opt;

        // when metric is null or all, return prometheus driver namespace
        if (metric == null || "all".equals(metric)) {
            opt = namespaceList.stream()
                    .filter(ns -> ns.getDatabaseDriver() instanceof PrometheusDatabaseDriver)
                    .findAny();
        } else {
            opt = namespaceList.stream()
                    .filter(ns -> ns.hasMetric(metric))
                    .findAny();
        }

        return opt.orElse(null);
    }

    static List<Namespace> getNameSpaces() {
        List<Namespace> namespacesAll = namespaces.values().stream().flatMap(List::stream).collect(Collectors.toCollection(LinkedList::new));
        namespacesAll.add(getCustomNamespace());
        return namespacesAll;
    }

    String ZSTACK_NAMESPACE_PREFIX = "ZStack";

    static String zstackNamespaceName(String subName) {
        return String.format("%s/%s", ZSTACK_NAMESPACE_PREFIX, subName);
    }

    String getName();

    List<Metric> getMetrics();

    List<EventFamily> getEvents();

    List<Datapoint> query(MetricQueryObject queryObject);

    List<Map> queryLabelValues(LabelValueQueryObject qo);

    Map<String, List<String>> queryPrometheusLabelValues(LabelValueQueryObject qo);

    List<Map> filterLabelValues(List<Map> result, LabelValueQueryObject qo);

    boolean deleteAll(String metricName, List<Label> labels);

    DatabaseDriver getDatabaseDriver();

    boolean hasMetric(String metricName);

    default Metric getInfoMetric() {
        return null;
    }

    default Set<String> getInfoLabelNames() {
        Metric info = getInfoMetric();
        if (info == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(info.getLabelNames());
    }

    default List<String> getEffectiveMetricLabelNames(Metric metric) {
        LinkedHashSet<String> labels = new LinkedHashSet<>(metric.getLabelNames());

        if (metric instanceof InfoMetric) {
            return new ArrayList<>(labels);
        }

        Set<String> infoLabels = new LinkedHashSet<>(getInfoLabelNames());
        if (PrometheusGlobalProperty.ENABLE_INFO_LABEL_ENRICHMENT) {
            labels.addAll(infoLabels);
        }

        return new ArrayList<>(labels);
    }

    String getResourceType();

    String getIdentityLabelName();

    default String getBasicIdentityLabelName() {
        return getIdentityLabelName();
    }

    String getResourceNameIfNull(AlarmAction.TakeAlarmActionParam param);
}
