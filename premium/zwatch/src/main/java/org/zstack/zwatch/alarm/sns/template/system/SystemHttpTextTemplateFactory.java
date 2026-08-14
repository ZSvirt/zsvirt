package org.zstack.zwatch.alarm.sns.template.system;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.sns.platform.http.SNSSystemHttpEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;
import org.zstack.zwatch.alarm.sns.TextTemplate;
import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

import java.util.ArrayList;
import java.util.List;

public class SystemHttpTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new SystemHttpTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSSystemHttpEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new SystemHttpTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new SystemHttpThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return false;
    }
}
