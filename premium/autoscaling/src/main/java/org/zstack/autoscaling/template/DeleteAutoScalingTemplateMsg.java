package org.zstack.autoscaling.template;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2018/10/8.
 */
public class DeleteAutoScalingTemplateMsg extends NeedReplyMessage {
    private String templateUuid;

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }
}
