package org.zstack.zwatch.alarm.sns.template.http;

import groovy.text.SimpleTemplateEngine;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.sns.platform.http.SNSHttpEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

public class HttpTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new HttpTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSHttpEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new HttpTextTemplate();
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new HttpThirdpartyTextTemplate();
    }

    @Override
    public boolean isSupportCustomTemplate() {
        return true;
    }

    @Override
    public void checkTemplate(String alarmTemplate, String recoveryTemplate, String type) {
        Map<String, Object> templateMap = AbstractTextTemplate.defaultTemplateMap.get(SNSTextTemplateType.get(type));
        SimpleTemplateEngine simpleTemplateEngine = new SimpleTemplateEngine();

        if (templateMap ==null) {
            throw new ApiMessageInterceptionException(argerr("no template of this type：%s, ", type));
        }

        try {
            alarmTemplate = alarmTemplate.replaceAll("\\\\", "");
            simpleTemplateEngine.createTemplate(alarmTemplate).make(templateMap);

            if (recoveryTemplate != null && !recoveryTemplate.isEmpty()) {
                recoveryTemplate = recoveryTemplate.replaceAll("\\\\", "");
                simpleTemplateEngine.createTemplate(recoveryTemplate).make(templateMap);
            }

        } catch (ClassNotFoundException | IOException e) {
            throw new ApiMessageInterceptionException(argerr("http template save error, please check template text：%s", e.getMessage()));
        }
    }

}
