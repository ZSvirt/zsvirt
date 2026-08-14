package org.zstack.zwatch.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.zstack.core.Platform;
import org.zstack.header.core.StaticInit;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.RestNamespace;

import java.util.*;

/**
 * Created by mingjian.deng on 2020/3/19.
 */
public class RestPrometheusNamespace extends AbstractPrometheusNamespace {
    private static double syncAPI;
    private static double asyncAPI;
    private static double succeedEvents;
    private static double failedEvents;
    private static double succeedReply;
    private static double failedReply;

    private static double lastSyncAPI;
    private static double lastAsyncAPI;
    private static double lastSucceedEvents;
    private static double lastFailedEvents;
    private static double lastSucceedReply;
    private static double lastFailedReply;


    private static double syncRest;
    private static double asyncRest;
    private static double syncRestReply;
    private static double asyncRestReply;

    private static double lastSyncRest;
    private static double lastAsyncRest;
    private static double lastSyncRestReply;
    private static double lastAsyncRestReply;

    private static String mgmtIp;

    @StaticInit
    static void staticInit () {
        PrometheusNamespace.namespacesClasses.put(RestNamespace.class, RestPrometheusNamespace.class);
        PrometheusCollector.registerMetricCollector(new RestCollector());
    }

    public RestPrometheusNamespace(Namespace namespace) {
        super(namespace);
    }

    private static Map<String, String> metricMap = new HashMap<>();

    static {
        metricMap.put(RestNamespace.SyncAPIs.getName(), "sync_api_total");
        metricMap.put(RestNamespace.ASyncAPIs.getName(), "async_api_total");
        metricMap.put(RestNamespace.IncreaseSyncAPIs.getName(), "sync_api_increase");
        metricMap.put(RestNamespace.IncreaseASyncAPIs.getName(), "async_api_increase");

        metricMap.put(RestNamespace.SyncRests.getName(), "sync_rest_total");
        metricMap.put(RestNamespace.ASyncRests.getName(), "async_rest_total");
        metricMap.put(RestNamespace.IncreaseSyncRests.getName(), "sync_rest_increase");
        metricMap.put(RestNamespace.IncreaseASyncRests.getName(), "async_rest_increase");
    }

    @Override
    protected RecordingRule createRecordingRule(Metric metric) {
        RecordingRule rule = new RecordingRule(makeSeriesName(metric.getName()));
        if (metricMap.get(metric.getName()) != null) {
            rule.setExpression(metricMap.get(metric.getName()));
        } else {
            rule.setForLabelMappingOnly(true);
            metric.getLabelNames().forEach(l->rule.labelMapping(l, l));
            return rule;
        }

        rule.setSeriesName(makeSeriesName(metric.getName()));
        return rule;
    }

    public static class RestCollector implements MetricCollector {
        @Override
        public boolean skipManagementNodeCheck() {
            return true;
        }

        @Override
        public List<Collector.MetricFamilySamples> collect() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            samples.addAll(apiMetricsCollect());

            return samples;
        }

        @Override
        public String getCollectorName() {
            return RestCollector.class.getName();
        }

        private List<Collector.MetricFamilySamples> getTotalRest() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            CounterMetricFamily SyncRests = createCounterMetric(RestNamespace.SyncRests);
            CounterMetricFamily ASyncRests = createCounterMetric(RestNamespace.ASyncRests);

            samples.add(SyncRests);
            samples.add(ASyncRests);

            List<String> labels = Arrays.asList(mgmtIp, "start");
            SyncRests.addMetric(labels, syncRest);
            ASyncRests.addMetric(labels, asyncRest);

            labels = Arrays.asList(mgmtIp, "end");
            SyncRests.addMetric(labels, syncRestReply);
            ASyncRests.addMetric(labels, asyncRestReply);

            return samples;
        }

        private List<Collector.MetricFamilySamples> getIncreaseRest() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            CounterMetricFamily IncreaseSyncRests = createCounterMetric(RestNamespace.IncreaseSyncRests);
            CounterMetricFamily IncreaseASyncRests = createCounterMetric(RestNamespace.IncreaseASyncRests);

            samples.add(IncreaseSyncRests);
            samples.add(IncreaseASyncRests);

            List<String> labels = Arrays.asList(mgmtIp, "start");
            IncreaseSyncRests.addMetric(labels, syncRest - lastSyncRest);
            IncreaseASyncRests.addMetric(labels, asyncRest - lastAsyncRest);

            labels = Arrays.asList(mgmtIp, "end");
            IncreaseSyncRests.addMetric(labels, syncRestReply - lastSyncRestReply);
            IncreaseASyncRests.addMetric(labels, asyncRestReply - lastAsyncRestReply);

            lastSyncRest = syncRest;
            lastAsyncRest = asyncRest;

            lastSyncRestReply = syncRestReply;
            lastAsyncRestReply = asyncRestReply;

            return samples;
        }

        private List<Collector.MetricFamilySamples> getTotalAPI() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            CounterMetricFamily SyncAPIs = createCounterMetric(RestNamespace.SyncAPIs);
            CounterMetricFamily ASyncAPIs = createCounterMetric(RestNamespace.ASyncAPIs);

            samples.add(SyncAPIs);
            samples.add(ASyncAPIs);

            List<String> labels = Arrays.asList(mgmtIp, "start");
            SyncAPIs.addMetric(labels, syncAPI);
            ASyncAPIs.addMetric(labels, asyncAPI);

            labels = Arrays.asList(mgmtIp, "succeed");
            SyncAPIs.addMetric(labels, succeedReply);
            ASyncAPIs.addMetric(labels, succeedEvents);

            labels = Arrays.asList(mgmtIp, "failed");
            SyncAPIs.addMetric(labels, failedReply);
            ASyncAPIs.addMetric(labels, failedEvents);

            labels = Arrays.asList(mgmtIp, "end");
            SyncAPIs.addMetric(labels, succeedReply + failedReply);
            ASyncAPIs.addMetric(labels, succeedEvents + failedEvents);

            return samples;
        }

        private List<Collector.MetricFamilySamples> getIncreaseAPI() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();

            CounterMetricFamily IncreaseSyncAPIs = createCounterMetric(RestNamespace.IncreaseSyncAPIs);
            CounterMetricFamily IncreaseASyncAPIs = createCounterMetric(RestNamespace.IncreaseASyncAPIs);

            samples.add(IncreaseSyncAPIs);
            samples.add(IncreaseASyncAPIs);

            List<String> labels = Arrays.asList(mgmtIp, "start");
            IncreaseSyncAPIs.addMetric(labels, syncAPI - lastSyncAPI);
            IncreaseASyncAPIs.addMetric(labels, asyncAPI - lastAsyncAPI);

            labels = Arrays.asList(mgmtIp, "succeed");
            IncreaseSyncAPIs.addMetric(labels, succeedReply - lastSucceedReply);
            IncreaseASyncAPIs.addMetric(labels, succeedEvents - lastSucceedEvents);

            labels = Arrays.asList(mgmtIp, "failed");
            IncreaseSyncAPIs.addMetric(labels, failedReply - lastFailedReply);
            IncreaseASyncAPIs.addMetric(labels, failedEvents - lastFailedEvents);

            labels = Arrays.asList(mgmtIp, "end");
            IncreaseSyncAPIs.addMetric(labels, succeedReply + failedReply - lastSucceedReply - lastFailedReply);
            IncreaseASyncAPIs.addMetric(labels, succeedEvents + failedEvents - lastSucceedEvents - lastFailedEvents);

            lastSyncAPI = syncAPI;
            lastAsyncAPI = asyncAPI;
            lastSucceedReply = succeedReply;
            lastSucceedEvents = succeedEvents;
            lastFailedReply = failedReply;
            lastFailedEvents = failedEvents;

            return samples;
        }

        private List<Collector.MetricFamilySamples> apiMetricsCollect() {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            if (mgmtIp == null) {
                mgmtIp = Platform.getManagementServerIp();
            }
            samples.addAll(getTotalAPI());
            if (lastFailedEvents <= failedEvents &&
                    lastSucceedEvents <= succeedEvents &&
                    lastFailedReply <= failedReply &&
                    lastSucceedReply <= succeedReply &&
                    lastSyncAPI <= syncAPI &&
                    lastAsyncAPI <= asyncAPI) {
                samples.addAll(getIncreaseAPI());
            }

            samples.addAll(getTotalRest());
            if (lastSyncRest <= syncRest &&
                    lastSyncRestReply <= syncRestReply &&
                    lastAsyncRest <= asyncRest &&
                    lastAsyncRestReply <= asyncRestReply) {
                samples.addAll(getIncreaseRest());
            }

            return samples;
        }

        private CounterMetricFamily createCounterMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new CounterMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }

        private GaugeMetricFamily createGaugeMetric(Metric m) {
            DebugUtils.Assert(metricMap.get(m.getName()) != null, String.format("cannot find this metrics: %s", m.getName()));
            return new GaugeMetricFamily(metricMap.get(m.getName()), String.format("help for %s", m.getName()), m.getLabelNames());
        }
    }


    public static void afterAPIRequest(Message msg) {
        if (msg instanceof APISyncCallMessage) {
            syncAPI ++;
        } else {
            asyncAPI ++;
        }
    }

    public static void beforeAPIResponse(Message msg) {
        if (msg instanceof APIEvent) {
            APIEvent evt = (APIEvent)msg;
            if (evt.isSuccess()) {
                succeedEvents ++;
            } else {
                failedEvents ++;
            }
        } else if (msg instanceof APIReply) {
            APIReply reply = (APIReply)msg;
            if (reply.isSuccess()) {
                succeedReply ++;
            } else {
                failedReply ++;
            }
        }
    }

    public static void beforeRestResponse(String method, int statusCode) {
        if (method.equalsIgnoreCase("get")) {
            syncRestReply ++;
        } else {
            asyncRestReply ++;
        }
    }

    public static void afterRestRequest(String method) {
        if (method.equalsIgnoreCase("get")) {
            syncRest ++;
        } else {
            asyncRest ++;
        }
    }
}
