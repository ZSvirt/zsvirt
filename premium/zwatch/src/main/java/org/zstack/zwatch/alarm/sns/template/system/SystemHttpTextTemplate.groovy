package org.zstack.zwatch.alarm.sns.template.system

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate

class SystemHttpTextTemplate extends AbstractTextTemplate implements TextTemplate {
    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        def msg = new SNSTopicMessage()
        Map<String, Object> bindMap = makeTemplateBindings(actionParam, false)
        bindMap.remove(PARAM_TITLE_ALARM_RESOURCE_NAME)
        msg.message = JSONObjectUtil.toJsonString(bindMap)
        return msg
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        def msg = new SNSTopicMessage()
        msg.message = JSONObjectUtil.toJsonString(makeTemplateBindings(actionParam, false))
        return msg
    }
}
