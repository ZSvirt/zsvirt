package org.zstack.scheduler.autoscalinggroup;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.scheduler.autoscalinggroup
 * @date 2020/12/8 4:42 PM
 */
public interface TakeAutoScalingSchedulerJobExtensionPoint {
    void takeAutoScalingSchedulerJob(String schedulerJobUuid, String ruleUuid, ReturnValueCompletion completion);
    Message  buildRequest(String schedulerJobUuid, String ruleUuid);
    ErrorCode allowStateChange(String ruleUuid);
}
