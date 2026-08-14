package org.zstack.autoscaling.group.activity;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.autoscaling.group.activity.action.AutoScalingGroupActivityActionMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;


/**
 * Created by lining on 2018/9/15.
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class CreateAutoScalingGroupActivityMsg extends NeedReplyMessage implements AutoScalingGroupMessage, ConfigurableTimeoutMessage {
    private String activityUuid;

    private String name;

    private String scalingGroupUuid;

    private String activityAction;

    private String scalingGroupRuleUuid;

    private String cause;

    private String description;

    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private Object actionMessage;

    public String getActivityAction() {
        return activityAction;
    }

    public void setActivityAction(String activityAction) {
        this.activityAction = activityAction;
    }

    public String getScalingGroupRuleUuid() {
        return scalingGroupRuleUuid;
    }

    public void setScalingGroupRuleUuid(String scalingGroupRuleUuid) {
        this.scalingGroupRuleUuid = scalingGroupRuleUuid;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScalingGroupUuid() {
        return scalingGroupUuid;
    }

    public void setScalingGroupUuid(String scalingGroupUuid) {
        this.scalingGroupUuid = scalingGroupUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivityUuid() {
        return activityUuid;
    }

    public void setActivityUuid(String activityUuid) {
        this.activityUuid = activityUuid;
    }

    public Object getActionMessage() {
        return actionMessage;
    }

    public void setActionMessage(AutoScalingGroupActivityActionMessage actionMessage) {
        this.actionMessage = actionMessage;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return scalingGroupUuid;
    }
}
