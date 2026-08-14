package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.monitoring.MonitorManager;
import org.zstack.monitoring.MonitorProvider;
import org.zstack.monitoring.MonitorTriggerInventory;
import org.zstack.monitoring.MonitorTriggerStatus;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.targets.MonitorTarget;
import org.zstack.monitoring.trigger.expression.TriggerExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/6/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusMonitorProvider implements MonitorProvider {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private MonitorManager mmgr;

    @Override
    public void setupMonitor(MonitorTarget template, Completion completion) {
        completion.success();
    }

    @Override
    public void deleteMonitor(MonitorTarget template, Completion completion) {
        List<String> itemNames = template.getItems().stream().map(i -> ((PrometheusItem)i).makeAlertRuleNameWithoutTriggerUuid(template.getTargetResourceUuid())).collect(Collectors.toList());
        AlertRuleWriter.deleteRules(itemNames);
        completion.success();
    }

    void recreateTriggers(List<MonitorTriggerInventory> triggers) {
        List<String> triggerNames = new ArrayList<>();
        List<AlertRuleWriter.RuleBuilder> rules = new ArrayList<>();

        for (MonitorTriggerInventory trigger : triggers) {
            TriggerExpression triggerExpression = TriggerExpression.expressionFromString(trigger.getExpression());
            String resourceUuid = trigger.getTargetResourceUuid();

            boolean resourceNotExists = !Q.New(ResourceVO.class).eq(ResourceVO_.uuid, resourceUuid).isExists();
            if (resourceNotExists){
                Item item = mmgr.getItemByExpressionString(trigger.getExpression());
                triggerNames.add(((PrometheusItem) item).makeAlertRuleName(resourceUuid, trigger.getUuid()));
                continue;
            }

            Item item = mmgr.getItem(resourceUuid, triggerExpression);
            triggerNames.add(((PrometheusItem) item).makeAlertRuleName(resourceUuid, trigger.getUuid()));

            rules.add(createRuleBuilder(trigger, item, triggerExpression));
        }

        AlertRuleWriter.deleteRules(triggerNames);
        AlertRuleWriter.writeRules(rules);
    }

    private AlertRuleWriter.RuleBuilder createRuleBuilder(MonitorTriggerInventory trigger, Item item, TriggerExpression triggerExpression) {
        AlertRuleWriter.RuleBuilder rule = ((PrometheusItem) item).createAlertRule(trigger, triggerExpression);
        rule.annotation(PrometheusMonitorProviderFactory.TRIGGER_UUID_ANNOTATION, trigger.getUuid());
        rule.annotation(PrometheusMonitorProviderFactory.TRIGGER_PROBLEM_STATUS_ANNOTATION, MonitorTriggerStatus.Problem.toString());
        rule.annotation(PrometheusMonitorProviderFactory.ITEM_RESOURCE_TYPE_ANNOTATION, item.getResourceType());
        rule.annotation(PrometheusMonitorProviderFactory.ITEM_NAME_ANNOTATION, item.getName());
        rule.annotation(PrometheusMonitorProviderFactory.VALUE_ANNOTATION, "{{$value}}");
        return rule;
    }

    @Override
    public void createTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression triggerExpression, Completion completion) {
        AlertRuleWriter.RuleBuilder rule = createRuleBuilder(trigger, item, triggerExpression);
        AlertRuleWriter.writeRule(rule);
        completion.success();
    }

    @Override
    public void deleteTrigger(MonitorTriggerInventory trigger, Item item, TriggerExpression expression, Completion completion) {
        AlertRuleWriter.deleteRule(((PrometheusItem) item).makeAlertRuleName(trigger.getTargetResourceUuid(), trigger.getUuid()));
        completion.success();
    }
}
