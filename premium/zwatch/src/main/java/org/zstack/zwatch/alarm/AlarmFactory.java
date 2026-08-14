package org.zstack.zwatch.alarm;

import org.zstack.zwatch.ruleengine.MetricRule;

/**
 * Created by kayo on 2018/9/17.
 */
public interface AlarmFactory {
    MetricRule asRule(MetricRule rule);

    String getAlarmType();
}
