package org.zstack.zwatch.alarm;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ruleengine.AverageAlgorithm;
import org.zstack.zwatch.ruleengine.MetricRule;

/**
 * Created by kayo on 2018/9/17.
 */
public class AverageAlarmFactory implements AlarmFactory {
    private final static CLogger logger = Utils.getLogger(AverageAlarmFactory.class);

    @Override
    public MetricRule asRule(MetricRule rule) {
        rule.setAlgorithm(new AverageAlgorithm());
        return rule;
    }

    @Override
    public String getAlarmType() {
        return AlarmType.Average.toString();
    }
}
