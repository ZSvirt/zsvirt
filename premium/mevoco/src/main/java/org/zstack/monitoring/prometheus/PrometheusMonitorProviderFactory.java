package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.debug.DebugManager;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.monitoring.*;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2017/6/6.
 */
public class PrometheusMonitorProviderFactory extends AbstractService {
    private static final CLogger logger = Utils.getLogger(PrometheusMonitorProviderFactory.class);

    private PrometheusMonitorProvider provider;

    public static final String SERVICE_ID = "prometheus.legacy";

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrometheusFactory prometheusFactory;

    public static final String TRIGGER_UUID_ANNOTATION = "triggerUuid";
    public static final String TRIGGER_PROBLEM_STATUS_ANNOTATION = "triggerStatus";
    public static final String ITEM_RESOURCE_TYPE_ANNOTATION = "itemResourceType";
    public static final String ITEM_NAME_ANNOTATION = "itemName";
    public static final String VALUE_ANNOTATION = "value";
    public static final String DEBUG_SIGNAL = "prometheusDumpProblemMonitorTriggers";
    public static final String PROMETHEUS_NIL_END_TIME_STRING = "0001-01-01T00:00:00Z";

    private Timestamp PROMETHEUS_NIL_END_TIME;

    private static List<String> aggregatorSyntaxs = new ArrayList<>();

    static {
        aggregatorSyntaxs.add("sum(");
        aggregatorSyntaxs.add("min(");
        aggregatorSyntaxs.add("max(");
        aggregatorSyntaxs.add("avg(");
        aggregatorSyntaxs.add("stddev(");
        aggregatorSyntaxs.add("stdvar(");
        aggregatorSyntaxs.add("count(");
        aggregatorSyntaxs.add("count_values(");
        aggregatorSyntaxs.add("bottomk(");
        aggregatorSyntaxs.add("topk(");
        aggregatorSyntaxs.add("quantile(");
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIPrometheusQueryPassThroughMsg) {
            handle((APIPrometheusQueryPassThroughMsg) msg);
        } else if (msg instanceof APIPrometheusQueryVmMonitoringDataMsg) {
            handle((APIPrometheusQueryVmMonitoringDataMsg) msg);
        } else if (msg instanceof APIPrometheusQueryMetadataMsg) {
            handle((APIPrometheusQueryMetadataMsg) msg);
        } else if (msg instanceof APIPrometheusQueryLabelValuesMsg) {
            handle((APIPrometheusQueryLabelValuesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIPrometheusQueryLabelValuesMsg msg) {
        Prometheus prometheus = prometheusFactory.getPrometheus();
        Map ret = new HashMap();
        for (String label : msg.getLabels()) {
            String url = String.format("/api/v1/label/%s/values", label);
            ret.put(label, prometheus.apiCall(url, null, Prometheus.APIQueryLabelValueStruct.class));
        }

        APIPrometheusQueryLabelValuesReply reply = new APIPrometheusQueryLabelValuesReply();
        reply.setInventories(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIPrometheusQueryVmMonitoringDataMsg msg) {
        for (String aggregator : aggregatorSyntaxs) {
            if (msg.getExpression().contains(aggregator)) {
                // use without() to make query returns metrics
                msg.setExpression(String.format("%s without (up)", msg.getExpression()));
                break;
            }
        }

        Set<String> vmUuids = new HashSet<>();
        vmUuids.addAll(msg.getVmUuids());

        Map ret = makeQuery(msg);
        String status = (String) ret.get("status");
        if (status == null) {
            throw new CloudRuntimeException(String.format("unknown result format[no status]: %s", JSONObjectUtil.toJsonString(ret)));
        }

        if ("error".equals(status)) {
            throw new OperationFailureException(operr("query failure, errorType:%s, error: %s", ret.get("errorType"), ret.get("error")));
        }

        Map data = (Map) ret.get("data");
        if (data == null) {
            throw new CloudRuntimeException(String.format("unknown result format[not data]: %s", JSONObjectUtil.toJsonString(ret)));
        }

        List result = (List) data.get("result");
        if (result == null) {
            throw new CloudRuntimeException(String.format("unknown result format[no result]: %s", JSONObjectUtil.toJsonString(ret)));
        }

        Iterator it = result.iterator();
        while (it.hasNext()) {
            Map item = (Map) it.next();
            Map metric = (Map) item.get("metric");
            if (metric == null) {
                throw new CloudRuntimeException(String.format("unknown result format[no metric]: %s", JSONObjectUtil.toJsonString(ret)));
            }

            String name = (String) metric.get("__name__");
            if (name != null && name.startsWith("collectd")) {
                // vm data collected by collectd
                String vmUuid = (String) metric.get("virt");
                if (!vmUuids.contains(vmUuid)) {
                    logger.debug(String.format("remove metric[virt:%s] not queried by this APIPrometheusQueryVmMonitoringDataMsg", vmUuid));
                    it.remove();
                }
            }
        }

        APIPrometheusQueryVmMonitoringDataReply reply = new APIPrometheusQueryVmMonitoringDataReply();
        reply.setInventories(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIPrometheusQueryMetadataMsg msg) {
        APIPrometheusQueryMetadataReply reply = new APIPrometheusQueryMetadataReply();

        Map<String, List<String>> params = new HashMap<>();
        msg.getMatches().forEach(it -> {
            List<String> lst = params.computeIfAbsent("match[]", k-> new ArrayList<>());
            lst.add(it);
        });

        Prometheus prometheus = prometheusFactory.getPrometheus();
        Prometheus.APIQueryMetadataStruct ret = prometheus.apiCall("/api/v1/series", params, Prometheus.APIQueryMetadataStruct.class);
        reply.setInventories(JSONObjectUtil.rehashObject(ret, LinkedHashMap.class));
        bus.reply(msg, reply);
    }


    private void handle(APIPrometheusQueryPassThroughMsg msg) {
        APIPrometheusQueryPassThroughReply reply = new APIPrometheusQueryPassThroughReply();

        reply.setInventories(makeQuery(msg));
        bus.reply(msg, reply);
    }

    private Map makeQuery(APIPrometheusQueryMsg msg) {
        Prometheus prometheus = prometheusFactory.getPrometheus();
        Map<String, String> params = new HashMap<>();
        params.put("query", msg.getExpression());

        if (msg.isInstant()) {
            params.put("time", msg.getEndTime().toString());
        } else {
            if (msg.getStartTime() != null) {
                params.put("start", msg.getStartTime().toString());
            }
            if (msg.getEndTime() != null) {
                params.put("end", msg.getEndTime().toString());
            }
            if (msg.getStep() != null) {
                params.put("step", msg.getStep());
            }
        }

        return prometheus.apiCall(msg.instant, params, LinkedHashMap.class);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    class AlertEntry {
        long timeout;

        public AlertEntry(int duration) {
            timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration);
        }

        boolean isTimeout() {
            return System.currentTimeMillis() > timeout;
        }
    }

    private ConcurrentHashMap<String, AlertEntry> alertCache = new ConcurrentHashMap<>();

    public synchronized boolean isTriggerStillProblem(String triggerUuid) {
        alertCache.entrySet().removeIf(e -> e.getValue().isTimeout());
        return alertCache.containsKey(triggerUuid);
    }

    void init() {
        provider = new PrometheusMonitorProvider();


        DebugManager.registerDebugSignalHandler(DEBUG_SIGNAL, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("================ BEGIN: Prometheus Dump Problem Monitor Triggers ===================\n");
            for (Map.Entry<String, AlertEntry> e : alertCache.entrySet()) {
                AlertEntry ae = e.getValue();
                sb.append(String.format("trigger uuid: %s, timeout at %s (%s)\n", e.getKey(),
                        new Timestamp(ae.timeout), ae.timeout));
            }
            sb.append("================ END: Prometheus Dump Problem Monitor Triggers =====================\n");
            logger.debug(sb.toString());
        });

        try {
            PROMETHEUS_NIL_END_TIME = new Timestamp(javax.xml.datatype.DatatypeFactory
                    .newInstance().newXMLGregorianCalendar(PROMETHEUS_NIL_END_TIME_STRING)
                    .toGregorianCalendar().getTimeInMillis());
        } catch (DatatypeConfigurationException e) {
            throw new CloudRuntimeException(e);
        }
    }

    void handleAlert(PrometheusAlert alert) throws DatatypeConfigurationException {
        String triggerUuid = alert.getAnnotations().get(TRIGGER_UUID_ANNOTATION);
        String triggerStatus = alert.getAnnotations().get(TRIGGER_PROBLEM_STATUS_ANNOTATION);

        Timestamp endtime = new Timestamp(javax.xml.datatype.DatatypeFactory
                .newInstance().newXMLGregorianCalendar(alert.getEndsAt())
                .toGregorianCalendar().getTimeInMillis());

        if (!endtime.equals(PROMETHEUS_NIL_END_TIME) && endtime.getTime() < System.currentTimeMillis()) {
            // the alert has been end
            return;
        }

        MonitorTriggerVO trigger = dbf.findByUuid(triggerUuid, MonitorTriggerVO.class);
        if (trigger == null) {
            return;
        }

        alert.setProvider(MonitorConstants.PROMETHEUS_PROVIDER);

        if (trigger.getContextData() == null) {
            trigger.setContextData(JSONObjectUtil.toTypedJsonString(alert));
            dbf.update(trigger);
        }

        alertCache.put(trigger.getUuid(), new AlertEntry(trigger.getDuration()));

        if (trigger.getStatus().toString().equals(triggerStatus)) {
            // no need to change status
            return;
        }

        ChangeMonitorTriggerStatusMsg msg = new ChangeMonitorTriggerStatusMsg();
        msg.setStatus(MonitorTriggerStatus.valueOf(triggerStatus));
        msg.setUuid(triggerUuid);
        msg.setContext(alert);
        bus.makeTargetServiceIdByResourceUuid(msg, MonitorConstants.SERVICE_ID, triggerUuid);
        bus.send(msg);
    }
}
