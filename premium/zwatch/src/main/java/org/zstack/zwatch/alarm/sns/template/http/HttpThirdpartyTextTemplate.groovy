package org.zstack.zwatch.alarm.sns.template.http

import org.zstack.utils.StringTemplateUtils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate
import org.zstack.zwatch.alarm.sns.ThirdpartyAbstractTextTemplate

class HttpThirdpartyTextTemplate extends ThirdpartyAbstractTextTemplate implements TextTemplate {

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        return null
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        def msg = new SNSTopicMessage()

        if (template == null) {
            msg.message = JSONObjectUtil.toJsonString(makeTemplateBindings(actionParam, false))
            return msg
        }

        def bindings = makeTemplateBindings(actionParam)
        msg.message = StringTemplateUtils.createStringFromTemplate(template.template, bindings)
        return msg
    }
}
