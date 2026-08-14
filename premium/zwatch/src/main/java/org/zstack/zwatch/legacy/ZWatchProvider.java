package org.zstack.zwatch.legacy;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.monitoring.*;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.items.host.*;
import org.zstack.monitoring.items.vm.VmCpuUtilItem;
import org.zstack.monitoring.items.vm.VmDiskIOItem;
import org.zstack.monitoring.items.vm.VmMemUtilItem;
import org.zstack.monitoring.items.vm.VmNetworkIOItem;
import org.zstack.monitoring.prometheus.PrometheusAlert;
import org.zstack.monitoring.prometheus.PrometheusMonitorProviderFactory;
import org.zstack.monitoring.targets.MonitorTarget;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.monitoring.MonitorConstants.MONITOR_ITEM_TYPE_KEY;


/**
 * this provider is a temporary solution that uses zwatch as a backend for
 * current alarm APIs. However, we will finally move to zwatch for all
 * devops APIs(e.g. monitoring, alarm), this implementation will deprecate
 * then.
 */
public class ZWatchProvider implements MonitorProvider, Component, ManagementNodeReadyExtensionPoint {
    @Autowired
    private RuleManager ruleMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private MonitorManager mmgr;

    @Override
    public void setupMonitor(MonitorTarget template, Completion completion) {
        completion.success();
    }

    @Override
    public void deleteMonitor(MonitorTarget template, Completion completion) {
        ruleMgr.deleteIf(r -> (r instanceof TRule) && ((TRule)r).resourceUuid.equals(template.getTargetResourceUuid()));
        completion.success();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        long total = Q.New(MonitorTriggerVO.class).count();
        int step = 5000;
        int times = (int) (total / step) + (total % step != 0 ? 1 : 0);
        int start = 0;

        for (int i=0; i<times; i++) {
            List<MonitorTriggerVO> triggers = Q.New(MonitorTriggerVO.class)
                    .limit(step).start(start).list();
            start += step;
            recreateTriggers(triggers);
        }
    }

    private void recreateTriggers(List<MonitorTriggerVO> triggers) {
        for (MonitorTriggerVO trigger : triggers) {
            TriggerExpression triggerExpression = TriggerExpression.expressionFromString(trigger.getExpression());
            Item item = mmgr.getItem(trigger.getTargetResourceUuid(), triggerExpression);
            createTrigger(MonitorTriggerInventory.valueOf(trigger), item, triggerExpression, new NopeCompletion());
        }
    }

    // override so we know it's our rules
    public static class TRule extends MetricRule {
        public String resourceUuid;
        public transient RuleEvaluationResultListener listener;

        TRule(String uuid, String resUuid) {
            super(uuid);
            resourceUuid = resUuid;
        }

        public RuleEvaluationResultListener getRuleStateChangeListener() {
            return listener;
        }

    }

    private void handleAlarm(RuleEvaluationResult res, Rule rule) {
        if (!(rule instanceof TRule)) {
            // not our rules
            return;
        }

        if (res.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.NoData) {
            return;
        }

        MetricRule mrule = (MetricRule) rule;
        MonitorTriggerVO trigger = dbf.findByUuid(mrule.getUuid(), MonitorTriggerVO.class);
        if (trigger == null) {
            // the trigger has been deleted
            ruleMgr.deleteRule(mrule.getUuid());
            return;
        }

        MonitorTriggerStatus status;
        if (res.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.OK) {
            status = MonitorTriggerStatus.OK;
        } else if (res.getCurrentState() == RuleEvaluationResult.RuleEvaluationState.Problem) {
            status = MonitorTriggerStatus.Problem;
        } else {
            throw new CloudRuntimeException(String.format("unsupported state: %s", res.getCurrentState()));
        }

        PrometheusAlert alert = new PrometheusAlert();
        alert.setProvider(MonitorConstants.PROMETHEUS_PROVIDER);
        Map<String, String> annotations = new HashMap<>();
        annotations.put(PrometheusMonitorProviderFactory.TRIGGER_UUID_ANNOTATION, trigger.getUuid());
        annotations.put(PrometheusMonitorProviderFactory.VALUE_ANNOTATION, String.valueOf(res.getCurrentValue()));
        alert.setAnnotations(annotations);

        ChangeMonitorTriggerStatusMsg msg = new ChangeMonitorTriggerStatusMsg();
        msg.setStatus(status);
        msg.setUuid(trigger.getUuid());
        msg.setContext(alert);
        bus.makeTargetServiceIdByResourceUuid(msg, MonitorConstants.SERVICE_ID, trigger.getUuid());
        bus.send(msg);
    }

    @Override
    public boolean stop() {
        return true;
    }

    public static class NamespaceMetric {
        String namespaceName;
        String metricName;
    }

    NamespaceMetric translateToZWatchTerms(Item item, TriggerExpression expr) {
        NamespaceMetric nm = new NamespaceMetric();

        String vmNamespaceName = String.format("ZStack/%s", VmNamespace.NAME);
        String hostNamespaceName = String.format("ZStack/%s", HostNamespace.NAME);

        if (item.getName().equals(VmCpuUtilItem.NAME)) {
            nm.namespaceName = vmNamespaceName;
            nm.metricName = VmNamespace.CPUUsedUtilization.getName();
        } else if (item.getName().equals(VmDiskIOItem.NAME)) {
            nm.namespaceName = vmNamespaceName;

            String type = (String) expr.argument("type");
            String dir = (String) expr.argument("direction");

            if (type.equals(VmDiskIOItem.TYPE_IOPS)) {
                if (dir.equals(VmDiskIOItem.DIRECTION_READ)) {
                    nm.metricName = VmNamespace.DiskAllReadOps.getName();
                } else {
                    nm.metricName = VmNamespace.DiskAllWriteOps.getName();
                }
            } else {
                if (dir.equals(VmDiskIOItem.DIRECTION_READ)) {
                    nm.metricName = VmNamespace.DiskAllReadBytes.getName();
                } else {
                    nm.metricName = VmNamespace.DiskAllWriteBytes.getName();
                }
            }
        } else if (item.getName().equals(VmMemUtilItem.NAME)) {
            nm.namespaceName = vmNamespaceName;
            nm.metricName = VmNamespace.MemoryFreeInPercent.getName();
        } else if (item.getName().equals(VmNetworkIOItem.NAME)) {
            nm.namespaceName = vmNamespaceName;

            String dir = (String) expr.argument("direction");
            if (dir.equals(VmNetworkIOItem.DIRECTION_RX)) {
                nm.metricName = VmNamespace.NetworkAllInBytes.getName();
            } else {
                nm.metricName = VmNamespace.NetworkAllOutBytes.getName();
            }
        } else if (item.getName().equals(HostCpuUtilItem.NAME)) {
            nm.namespaceName = hostNamespaceName;

            String type = (String) expr.argument("type");
            if (type.equals(HostCpuUtilItem.TYPE_IDLE)) {
                nm.metricName = HostNamespace.CPUAllIdleUtilization.getName();
            } else {
                nm.metricName = HostNamespace.CPUAllUsedUtilization.getName();
            }
        } else if (item.getName().equals(HostMemUtilItem.NAME)) {
            nm.namespaceName = hostNamespaceName;

            String type = (String) expr.argument("type");
            if (type.equals(HostMemUtilItem.TYPE_FREE)) {
                nm.metricName = HostNamespace.MemoryFreeInPercent.getName();
            } else {
                nm.metricName = HostNamespace.MemoryUsedInPercent.getName();
            }
        } else if (item.getName().equals(HostDiskIOItem.NAME)) {
            nm.namespaceName = hostNamespaceName;

            String type = (String) expr.argument("type");
            String dir = (String) expr.argument("direction");

            if (type.equals(HostDiskIOItem.TYPE_IOPS)) {
                if (dir.equals(HostDiskIOItem.DIRECTION_READ)) {
                    nm.metricName = HostNamespace.DiskAllReadOps.getName();
                } else {
                    nm.metricName = HostNamespace.DiskAllWriteOps.getName();
                }
            } else {
                if (dir.equals(HostDiskIOItem.DIRECTION_READ)) {
                    nm.metricName = HostNamespace.DiskAllReadBytes.getName();
                } else {
                    nm.metricName = HostNamespace.DiskAllWriteBytes.getName();
                }
            }

        } else if (item.getName().equals(HostNetworkIOItem.NAME)) {
            nm.namespaceName = hostNamespaceName;

            String dir = (String) expr.argument("direction");
            if (dir.equals(HostNetworkIOItem.DIRECTION_RX)) {
                nm.metricName = HostNamespace.NetworkAllInBytes.getName();
            } else {
                nm.metricName = HostNamespace.NetworkAllOutBytes.getName();
            }
        } else if (item.getName().equals(HostDiskCapacityItem.NAME)) {
            nm.namespaceName = hostNamespaceName;

            String type = (String) expr.argument(MONITOR_ITEM_TYPE_KEY);

            if (type.equals(HostDiskCapacityItem.TYPE_AVAIL_SIZE)) {
                nm.metricName = HostNamespace.DiskFreeCapacityInBytes.getName();
            } else {
                nm.metricName = HostNamespace.DiskFreeCapacityInPercent.getName();
            }
        } else {
            throw new CloudRuntimeException("unsupported");
        }

        return nm;
    }

    private ComparisonOperator translateToZWatchComparisonOperator(String op) {
        if (op.equals(">=")) {
            return ComparisonOperator.GreaterThanOrEqualTo;
        } else if (op.equals(">")) {
            return ComparisonOperator.GreaterThan;
        } else if (op.equals("<=")) {
            return ComparisonOperator.LessThanOrEqualTo;
        } else if (op.equals("<")) {
            return ComparisonOperator.LessThan;
        } else {
            throw new CloudRuntimeException(String.format("zwatch doesn't support the operator[%s]", op));
        }
    }

    Map<String, String> translateToZWatchLabels(MonitorTriggerInventory trigger, Item item) {
        Map<String, String> labels = new HashMap<>();
        if (item.getName().equals(VmCpuUtilItem.NAME) || item.getName().equals(VmDiskIOItem.NAME)
                || item.getName().equals(VmMemUtilItem.NAME) || item.getName().equals(VmNetworkIOItem.NAME)) {
            labels.put(VmNamespace.LabelNames.VMUuid.toString(), trigger.getTargetResourceUuid());
            return labels;
        } else if (item.getName().equals(HostCpuUtilItem.NAME) || item.getName().equals(HostDiskIOItem.NAME)
                || item.getName().equals(HostMemUtilItem.NAME) || item.getName().equals(HostNetworkIOItem.NAME)
                || item.getName().equals(HostDiskCapacityItem.NAME)) {
            labels.put(HostNamespace.LabelNames.HostUuid.toString(), trigger.getTargetResourceUuid());
        } else {
            throw new CloudRuntimeException("unsupported");
        }

        return labels;
    }

    double translateToZWatchThreshold(Item item, TriggerExpression expression) {
        if (item.getName().equals(VmMemUtilItem.NAME) || item.getName().equals(HostMemUtilItem.NAME)) {
            return Double.valueOf(expression.getConstant()) * 100;
        } else {
            return Double.valueOf(expression.getConstant());
        }
    }

    private MetricRule asRule(MonitorTriggerInventory trigger, Item item, TriggerExpression triggerExpression) {
        NamespaceMetric nm = translateToZWatchTerms(item, triggerExpression);
        TRule rule = new TRule(trigger.getUuid(), trigger.getTargetResourceUuid());
        rule.setComparisonOperator(translateToZWatchComparisonOperator(triggerExpression.getOperator()));
        rule.setLabels(translateToZWatchLabels(trigger, item)
                .entrySet().stream()
                .map(entry -> new Label(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        rule.setDuration(trigger.getDuration().longValue());
        rule.setMetricName(nm.metricName);
        rule.setNamespaceName(nm.namespaceName);
        rule.setThreshold(translateToZWatchThreshold(item, triggerExpression));
        rule.listener = this::handleAlarm;
        return rule;
    }

    @Override
    public void createTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression triggerExpression, Completion completion) {
        ruleMgr.addRule(asRule(trigger, item, triggerExpression));
        completion.success();
    }

    @Override
    public void deleteTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression expression, Completion completion) {
        ruleMgr.deleteRule(trigger.getUuid());
        completion.success();
    }
}
