package org.zstack.zwatch.alarm.sns.template.snmp


import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate
import org.zstack.zwatch.alarm.sns.ThirdpartyAbstractTextTemplate

import java.text.SimpleDateFormat
/**
 * @Author : jingwang
 * @create 2023/7/25 6:56 PM
 *
 */
class SnmpThirdpartyTextTemplate extends ThirdpartyAbstractTextTemplate implements TextTemplate{
    static final String datePattern = "MM-dd HH:mm"

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        def bindings = makeTemplateBindings(actionParam, false)
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_ALARM_TIME, dateFormat.format(bindings.get(PARAM_ALARM_TIME)))
        SNSTopicMessage message = new SNSTopicMessage()
        message.metadata = bindings
        return message
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        def bindings = makeTemplateBindings(actionParam, false)
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_ALARM_TIME, dateFormat.format(bindings.get(PARAM_EVENT_TIME)))
        SNSTopicMessage message = new SNSTopicMessage()
        message.metadata = bindings
        return message
    }
}
