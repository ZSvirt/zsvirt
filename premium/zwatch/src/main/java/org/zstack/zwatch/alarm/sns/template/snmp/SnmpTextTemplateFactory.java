package org.zstack.zwatch.alarm.sns.template.snmp;

import org.zstack.sns.platform.snmp.SNSSnmpEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;
import org.zstack.zwatch.alarm.sns.TextTemplate;
import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

/**
 *
 * @Author : jingwang
 * @create 2023/7/25 6:55 PM
 */
public class SnmpTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new SnmpTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSSnmpEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new SnmpTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new SnmpThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return false;
    }
}
