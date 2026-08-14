package org.zstack.zwatch.alarm.sns.template.wecom;

import org.zstack.sns.platform.wecom.SNSWeComEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;
import org.zstack.zwatch.alarm.sns.TextTemplate;
import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

public class WeComTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new WeComTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSWeComEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new WeComTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new WeComThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return true;
    }
}
