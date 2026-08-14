package org.zstack.zwatch.metricpusher;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.api.*;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.metricpusher.message.PushMetricDataToReceiverMsg;
import org.zstack.zwatch.metricpusher.message.PushMetricDataToReceiverReply;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MetricPushManagerImpl extends AbstractService implements ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(MetricPushManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private RESTFacade restf;

    private Future pushMetricDataTask;
    private Set<String> metricReceiverInTracking = Collections.synchronizedSet(new HashSet<String>());

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof PushMetricDataToReceiverMsg) {
            handle((PushMetricDataToReceiverMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateMetricDataHttpReceiverMsg) {
            handle((APICreateMetricDataHttpReceiverMsg) msg);
        } else if (msg instanceof APICreateMetricTemplateMsg) {
            handle((APICreateMetricTemplateMsg) msg);
        } else if (msg instanceof APIDeleteMetricDataHttpReceiverMsg) {
            handle((APIDeleteMetricDataHttpReceiverMsg) msg);
        } else if (msg instanceof APIDeleteMetricTemplateMsg) {
            handle((APIDeleteMetricTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(Constants.SERVICE_ID);
    }

    @Override
    public void managementNodeReady() {
        startPushMetricTask();

        ZWatchGlobalConfig.METRIC_PUSH_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startPushMetricTask());
    }

    private synchronized void startPushMetricTask() {
        if (pushMetricDataTask != null) {
            pushMetricDataTask.cancel(true);
        }

        pushMetricDataTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ZWatchGlobalConfig.METRIC_PUSH_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "push-metric-data-to-receiver";
            }

            @Override
            public void run() {
                logger.debug("push metric data to receiver");
                pushMetricToReceiver();
            }
        });
    }

    private void pushMetricToReceiver() {
        List<MetricDataHttpReceiverVO> receiverVOS = Q.New(MetricDataHttpReceiverVO.class)
                .eq(MetricDataHttpReceiverVO_.state, ReceiverState.Enabled)
                .list();

        if (receiverVOS.isEmpty()) {
            return;
        }

        List<PushMetricDataToReceiverMsg> msgs = new ArrayList<>();

        for (MetricDataHttpReceiverVO receiverVO : receiverVOS) {
            if (!destinationMaker.isManagedByUs(receiverVO.getUuid())) {
                continue;
            }

            if (metricReceiverInTracking.contains(receiverVO.getUuid())) {
                continue;
            }

            metricReceiverInTracking.add(receiverVO.getUuid());

            PushMetricDataToReceiverMsg msg = new PushMetricDataToReceiverMsg();
            msg.setReceiverUuid(receiverVO.getUuid());
            bus.makeLocalServiceId(msg, Constants.SERVICE_ID);
            msgs.add(msg);
        }

        if (msgs.isEmpty()) {
            return;
        }

        new While<>(msgs).each((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    metricReceiverInTracking.remove(msg.getReceiverUuid());
                    compl.done();
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }

    private void pushMetricData(String httpUrl, MetricTemplateVO templateVO) {
        List<Label> labels = new ArrayList<>();
        String labelsJsonStr = templateVO.getLabelsJsonStr();
        if (labelsJsonStr != null && !labelsJsonStr.isEmpty()) {
            List<String> labelStrings = JSONObjectUtil.toObject(labelsJsonStr, List.class);
            for (String labelStr : labelStrings) {
                Label label = new Label(labelStr);
                labels.add(label);
            }
        }

        long endTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        long startTime = endTime - 1;
        String namespace = templateVO.getNamespace();
        MetricQueryObject qo = MetricQueryObject.New()
                .namespace(namespace)
                .startTime(startTime)
                .endTime(endTime)
                .labels(labels)
                .metricName(templateVO.getMetricName())
                .build();
        Namespace ns = Namespace.getMetricNameSpace(namespace, qo.getMetricName());
        List<Datapoint> datas = ns.query(qo);
        if (datas == null || datas.isEmpty()) {
            return;
        }

        List<Map<String, Object>> params = make(datas);
        List<String> metrics = MetricTemplateUtils.render(templateVO.getTemplate(), params);

        int slice = ZWatchGlobalConfig.METRIC_PUSH_SLICE_SIZE.value(Integer.class);
        List<List<String>> metricParts = Lists.partition(metrics, slice);
        for (List<String> metricPart : metricParts) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> req = new HttpEntity<>(metricPart.toString(), headers);

            logger.trace(String.format("push metrics to url[%s], %s", httpUrl, metricPart.toString()));
            ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
                @Override
                @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
                protected ResponseEntity<String> call() {
                    return restf.getRESTTemplate().exchange(httpUrl, HttpMethod.POST, req, String.class);
                }
            }.run();

            if (!rsp.getStatusCode().is2xxSuccessful()) {
                logger.error(String.format("push metric fail, %s", rsp.toString()));
                break;
            } else {
                logger.trace(String.format("push metric result: %s", rsp.getBody()));

                if (rsp.hasBody() &&
                        (rsp.getBody().contains("error") || rsp.getBody().contains("Exception"))) {
                    logger.error(String.format("push metric fail, %s", rsp.toString()));
                    break;
                }
            }
        }
    }

    private List<Map<String, Object>> make(List<Datapoint> datapoints) {
        List<Map<String, Object>> result = new ArrayList();

        for (Datapoint datapoint : datapoints) {
            Map<String, Object> params = new HashMap<>();
            params.put("METRIC_VALUE", datapoint.getValue());
            params.put("METRIC_LABLES", datapoint.getLabels());
            params.put("METRIC_TIME", datapoint.getTime());
            result.add(params);
        }

        return result;
    }

    private void handle(APICreateMetricDataHttpReceiverMsg msg) {
        APICreateMetricDataHttpReceiverEvent event = new APICreateMetricDataHttpReceiverEvent(msg.getId());
        MetricDataHttpReceiverVO receiverVO = new MetricDataHttpReceiverVO();
        String uuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        receiverVO.setUuid(uuid);
        receiverVO.setName(msg.getName());
        receiverVO.setDescription(msg.getDescription());
        receiverVO.setUrl(msg.getUrl());
        if (msg.isDefaultEnable()) {
            receiverVO.setState(ReceiverState.Enabled);
        } else {
            receiverVO.setState(ReceiverState.Disabled);
        }
        receiverVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        receiverVO = dbf.persist(receiverVO);

        event.setInventory(MetricDataHttpReceiverInventory.valueOf(receiverVO));
        bus.publish(event);
    }

    private void handle(APICreateMetricTemplateMsg msg) {
        APICreateMetricTemplateEvent event = new APICreateMetricTemplateEvent(msg.getId());
        MetricTemplateVO vo = new MetricTemplateVO();
        String uuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        vo.setUuid(uuid);
        vo.setReceiverUuid(msg.getReceiverUuid());
        vo.setTemplate(msg.getTemplate());
        vo.setLabelsJsonStr(msg.getLabelsJsonStr());
        vo.setMetricName(msg.getMetricName());
        vo.setNamespace(msg.getNamespace());
        vo.setDescription(msg.getDescription());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo = dbf.persist(vo);

        event.setInventory(MetricTemplateInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIDeleteMetricDataHttpReceiverMsg msg) {
        APIDeleteMetricDataHttpReceiverEvent event = new APIDeleteMetricDataHttpReceiverEvent(msg.getId());

        SQL.New(MetricTemplateVO.class)
                .eq(MetricTemplateVO_.receiverUuid, msg.getUuid())
                .hardDelete();
        SQL.New(MetricDataHttpReceiverVO.class)
                .eq(MetricDataHttpReceiverVO_.uuid, msg.getUuid())
                .hardDelete();

        bus.publish(event);
    }

    private void handle(APIDeleteMetricTemplateMsg msg) {
        APIDeleteMetricTemplateEvent event = new APIDeleteMetricTemplateEvent(msg.getId());

        SQL.New(MetricTemplateVO.class)
                .eq(MetricTemplateVO_.uuid, msg.getUuid())
                .hardDelete();

        bus.publish(event);
    }

    private void handle(PushMetricDataToReceiverMsg msg) {
        PushMetricDataToReceiverReply reply = new PushMetricDataToReceiverReply();

        List<MetricTemplateVO> templateVOS = Q.New(MetricTemplateVO.class)
                .eq(MetricTemplateVO_.receiverUuid, msg.getReceiverUuid())
                .list();
        if (templateVOS.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        String url = Q.New(MetricDataHttpReceiverVO.class)
                .select(MetricDataHttpReceiverVO_.url)
                .eq(MetricDataHttpReceiverVO_.uuid, msg.getReceiverUuid())
                .findValue();
        for (MetricTemplateVO templateVO : templateVOS) {
            pushMetricData(url, templateVO);
        }

        bus.reply(msg, reply);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        if (pushMetricDataTask != null) {
            pushMetricDataTask.cancel(true);
        }
        return true;
    }
}
