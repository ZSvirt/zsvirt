package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.core.Completion;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.autoscaling.group.rule.trigger
 * @date 2020/12/18 2:31 PM
 */
public interface AutoScalingRuleTriggerFactory {

    AutoScalingRuleTriggerType getType();

    String getResourceUuid(String triggerUuid);

    void cleanResource(String resourceUuid, Completion completion);


}
