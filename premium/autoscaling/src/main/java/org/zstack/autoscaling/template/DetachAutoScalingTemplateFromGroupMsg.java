package org.zstack.autoscaling.template;

import org.zstack.autoscaling.group.AutoScalingGroupMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/8.
 */
public class DetachAutoScalingTemplateFromGroupMsg extends NeedReplyMessage implements AutoScalingGroupMessage {
    private String templateUuid;

    private String autoScalingGroupUuid;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return autoScalingGroupUuid;
    }

    public void setAutoScalingGroupUuid(String autoScalingGroupUuid) {
        this.autoScalingGroupUuid = autoScalingGroupUuid;
    }
}
