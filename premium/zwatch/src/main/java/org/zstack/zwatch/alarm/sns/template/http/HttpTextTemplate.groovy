package org.zstack.zwatch.alarm.sns.template.http

import org.zstack.utils.StringTemplateUtils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.AlarmStatus
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate

import java.text.SimpleDateFormat

class HttpTextTemplate extends AbstractTextTemplate implements TextTemplate {
    static final String datePattern = "MM-dd HH:mm"

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        def msg = new SNSTopicMessage()

        if (template == null) {
            Map<String, Object> bindMap = makeTemplateBindings(actionParam, false)
            bindMap.remove(PARAM_TITLE_ALARM_RESOURCE_NAME)
            msg.message = JSONObjectUtil.toJsonString(bindMap)
            return msg
        }

        boolean isRecovery = actionParam.currentStatus == AlarmStatus.OK
        String templateText = isRecovery ? template.recoveryTemplate : template.template
        Map<String, Object> bindings = makeTemplateBindings(actionParam)
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_ALARM_TIME, dateFormat.format(bindings.get(PARAM_ALARM_TIME)))

        templateText = templateText.replaceAll("\\\\", "")
        msg.message = StringTemplateUtils.createStringFromTemplate(templateText, bindings)

        return msg
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        def msg = new SNSTopicMessage()

        if (template == null) {
            msg.message = JSONObjectUtil.toJsonString(makeTemplateBindings(actionParam, false))
            return msg
        }

        def bindings = makeTemplateBindings(actionParam)
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_EVENT_TIME, dateFormat.format(bindings.get(PARAM_EVENT_TIME)))
        msg.message = StringTemplateUtils.createStringFromTemplate(template.template, bindings)
        return msg
    }
}
