package org.zstack.zwatch.alarm.sns;

import groovy.text.SimpleTemplateEngine;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.zwatch.alarm.AlarmAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.argerr;


public interface TextTemplateFactory {
    TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam);

    String getApplicationPlatformType();

    TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam);

    TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam);

    boolean isSupportCustomTemplate();

    default void checkTemplate(String alarmTemplate, String recoveryTemplate, String type) {
        Map<String, Object> templateMap = AbstractTextTemplate.defaultTemplateMap.get(SNSTextTemplateType.get(type));
        SimpleTemplateEngine simpleTemplateEngine = new SimpleTemplateEngine();

        if (templateMap ==null) {
            throw new ApiMessageInterceptionException(argerr("no template of this type：%s, ", type));
        }

        try {
            simpleTemplateEngine.createTemplate(alarmTemplate).make(templateMap);
            if (recoveryTemplate != null && !recoveryTemplate.isEmpty()) {
                simpleTemplateEngine.createTemplate(recoveryTemplate).make(templateMap);
            }

        } catch (ClassNotFoundException e) {
            throw new ApiMessageInterceptionException(argerr("template error：%s", e.getMessage()));
        } catch (IOException e) {
            throw new ApiMessageInterceptionException(argerr("template error：%s", e.getMessage()));
        }
    }
}
