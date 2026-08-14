package org.zstack.zwatch.alarm.sns.template.feishu;

import org.zstack.sns.platform.feishu.SNSFeiShuEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;
import org.zstack.zwatch.alarm.sns.TextTemplate;
import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

public class FeiShuTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new FeiShuTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSFeiShuEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new FeiShuTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new FeiShuThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return true;
    }
}
