package org.zstack.zwatch.alarm.sns.template.microsoftteams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import groovy.text.SimpleTemplateEngine;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointFactory;
import org.zstack.zwatch.alarm.AlarmAction;
import org.zstack.zwatch.alarm.sns.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;

public class MicrosoftTeamsTextTemplateFactory implements TextTemplateFactory {
    @Override
    public TextTemplate createAlarmTemplate(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam) {
        return new MicrosoftTeamsTextTemplate();
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSMicrosoftTeamsEndpointFactory.type.toString();
    }

    @Override
    public TextTemplate createEventTemplate(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new MicrosoftTeamsTextTemplate();
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

        } catch (ClassNotFoundException e) {
            throw new ApiMessageInterceptionException(argerr("template error：%s", e.getMessage()));
        } catch (IOException e) {
            throw new ApiMessageInterceptionException(argerr("template error：%s", e.getMessage()));
        }
    }

    @Override
    public TextTemplate createEventTemplateForThirdpartyAlert(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam) {
        return new MicrosoftTeamsThirdpartyTextTemplate();
    }
}
