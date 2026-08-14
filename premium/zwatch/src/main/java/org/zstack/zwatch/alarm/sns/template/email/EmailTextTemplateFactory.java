package org.zstack.zwatch.alarm.sns.template.email;

import groovy.text.SimpleTemplateEngine;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.sns.platform.email.SNSEmailPlatformFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

public class EmailTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new EmailTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSEmailPlatformFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new EmailTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new EmailThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return true;
    }
}
