package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.sns.SNSConstants;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory;
import org.zstack.zwatch.alarm.sns.TextTemplate;
import org.zstack.zwatch.alarm.sns.TextTemplateFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Qi Le on 2019-07-12
 */
public class AliyunSmsTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new AliyunSmsTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSConstants.ALIYUNSMS_PLATFORM;
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new AliyunSmsTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new AliyunSmsThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return true;
    }

}
