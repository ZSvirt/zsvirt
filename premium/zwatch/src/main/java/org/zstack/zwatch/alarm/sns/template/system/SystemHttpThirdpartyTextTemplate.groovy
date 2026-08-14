package org.zstack.zwatch.alarm.sns.template.system

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate
import org.zstack.zwatch.alarm.sns.ThirdpartyAbstractTextTemplate

class SystemHttpThirdpartyTextTemplate extends ThirdpartyAbstractTextTemplate implements TextTemplate {
    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String topicUuid) {
        return null
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String topicUuid) {
        def msg = new SNSTopicMessage()
        msg.message = JSONObjectUtil.toJsonString(makeTemplateBindings(actionParam, false))
        return msg
    }
}
