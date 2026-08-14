package org.zstack.zwatch.alarm;

import org.zstack.zwatch.ruleengine.AnyAlgorithm;
import org.zstack.zwatch.ruleengine.MetricRule;

/**
 * Created by kayo on 2018/9/17.
 */
public class DefaultAlarmFactory implements AlarmFactory {
    @Override
    public MetricRule asRule(MetricRule rule) {
        rule.setAlgorithm(new AnyAlgorithm());
        return rule;
    }

    @Override
    public String getAlarmType() {
        return AlarmType.Any.toString();
    }
}
