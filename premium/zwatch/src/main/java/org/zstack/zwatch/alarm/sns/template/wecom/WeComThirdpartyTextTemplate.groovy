package org.zstack.zwatch.alarm.sns.template.wecom

import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.sns.platform.wecom.WeComMessageMetadata
import org.zstack.utils.StringTemplateUtils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate
import org.zstack.zwatch.alarm.sns.ThirdpartyAbstractTextTemplate

class WeComThirdpartyTextTemplate extends ThirdpartyAbstractTextTemplate implements TextTemplate {
    static final String ALARM_ENGLISH_TEMPLATE = '''
## Alarm Details:
- UUID: ${ALARM_UUID}
- Namespace: ${ALARM_NAMESPACE}
- Condition: ${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR} ${ALARM_THRESHOLD}
- Duration: ${ALARM_DURATION} seconds
- Previous Status: ${ALARM_PREVIOUS_STATUS}
- Current Metric Value: ${ALARM_CURRENT_VALUE}
- Alarm Resource UUID: ${ALARM_RESOURCE_ID}
- Alarm Resource Name: ${ALARM_RESOURCE_NAME}
- Emergency Level: ${ALARM_EMERGENCY_LEVEL}
- Labels: ${ALARM_LABELS.join(",")}
- Alarm Trigger Time: ${ALARM_TIME}
'''

    static final String ALARM_CHINESE_TEMPLATE = '''
## 报警器详情:
- UUID: ${ALARM_UUID}
- 资源类型: ${ALARM_NAMESPACE}
- 触发条件: ${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR} ${ALARM_THRESHOLD}
- 触发条件持续时间: ${ALARM_DURATION} seconds
- 先前状态: ${ALARM_PREVIOUS_STATUS}
- 当前值: ${ALARM_CURRENT_VALUE}
- 报警资源UUID: ${ALARM_RESOURCE_ID}
- 报警资源名称: ${ALARM_RESOURCE_NAME}
- 报警级别: ${ALARM_EMERGENCY_LEVEL}
- 标签: ${ALARM_LABELS.join(",")}
- 报警触发时间: ${ALARM_TIME}
'''

    static final String RECOVERY_CHINESE_TEMPLATE = '''
## 报警恢复详情:
- UUID: ${ALARM_UUID}
- 资源类型: ${ALARM_NAMESPACE}
- 恢复条件: ${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR_REVERSE} ${ALARM_THRESHOLD}
- 先前状态: ${ALARM_PREVIOUS_STATUS}
- 当前值: ${ALARM_CURRENT_VALUE}
- 报警资源UUID: ${ALARM_RESOURCE_ID}
- 报警资源名称: ${ALARM_RESOURCE_NAME}
- 报警触发时间: ${ALARM_TIME}
'''
    static final String RECOVERY_ENGLISH_TEMPLATE = '''
## Recovery Details:
- UUID: ${ALARM_UUID}
- Namespace: ${ALARM_NAMESPACE}
- Condition: ${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR_REVERSE} ${ALARM_THRESHOLD}
- Previous Status: ${ALARM_PREVIOUS_STATUS}
- Current Metric Value: ${ALARM_CURRENT_VALUE}
- Alarm Resource UUID: ${ALARM_RESOURCE_ID}
- Alarm Resource Name: ${ALARM_RESOURCE_NAME}
- Alarm Trigger Time: ${ALARM_TIME}
'''

    static final String ALARM_ENGLISH_SUBJECT = 'Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}'
    static final String ALARM_CHINESE_SUBJECT = '报警器 ${ALARM_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}'

    static final String RECOVERY_ENGLISH_SUBJECT = 'Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}'
    static final String RECOVERY_CHINESE_SUBJECT = '报警器 ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}'

    static final String EVENT_ENGLISH_TEMPLATE = '''
## Event Details:
- Name: ${EVENT_NAME}
- Namespace: ${EVENT_NAMESPACE}
- Emergency Level: ${EVENT_EMERGENCY_LEVEL}
- Resource UUID: ${EVENT_RESOURCE_ID}
- Resource Name: ${EVENT_RESOURCE_NAME}
- When: ${EVENT_TIME}
- Subscription UUID: ${EVENT_SUBSCRIPTION_UUID}
- Labels: ${EVENT_LABELS.collect {k, v -> "$k = $v"}.join(',')}
- Error(empty if no error): ${EVENT_ERROR}
'''

    static final String EVENT_CHINESE_TEMPLATE = '''
## 事件详情:
- 名称: ${EVENT_NAME}
- 事件名字空间: ${EVENT_NAMESPACE}
- 报警级别: ${EVENT_EMERGENCY_LEVEL}
- 资源UUID: ${EVENT_RESOURCE_ID}
- 资源名称: ${EVENT_RESOURCE_NAME}
- 发生时间: ${EVENT_TIME}
- 事件订阅UUID: ${EVENT_SUBSCRIPTION_UUID}
- 标签: ${EVENT_LABELS.collect {k, v -> "$k = $v"}.join(',')}
- 错误（如果没有错误时为空）: ${EVENT_ERROR}
'''
    static final String EVENT_ENGLISH_SUBJECT = 'Event ${EVENT_NAME} Happened'
    static final String EVENT_CHINESE_SUBJECT = '事件 ${EVENT_NAME} 发生了'

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        return null
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        String templateText
        String subjectText

        if (template != null) {
            templateText = template.template
        } else if (Platform.getLocale() == Locale.SIMPLIFIED_CHINESE) {
            templateText = EVENT_CHINESE_TEMPLATE
        } else {
            templateText = EVENT_ENGLISH_TEMPLATE
        }

        if (Platform.locale == Locale.SIMPLIFIED_CHINESE) {
            subjectText = EVENT_CHINESE_SUBJECT
        } else {
            subjectText = EVENT_ENGLISH_SUBJECT
        }

        if (template == null && StringUtils.isNotEmpty(locale)) {
            if (locale == "zh_CN") {
                templateText = EVENT_CHINESE_TEMPLATE
                subjectText = EVENT_CHINESE_SUBJECT
            } else {
                templateText = EVENT_ENGLISH_TEMPLATE
                subjectText = EVENT_ENGLISH_SUBJECT
            }
        }

        def bindings = makeTemplateBindings(actionParam)
        if (StringUtils.isNotEmpty(locale)) {
            bindings = locale == "zh_CN" ? makeTemplateBindings(actionParam) :
                    makeTemplateBindings(actionParam, false)
        }
        SNSTopicMessage message = new SNSTopicMessage()
        message.message = StringTemplateUtils.createStringFromTemplate(templateText, bindings)
        message.metadata = JSONObjectUtil.rehashObject(new WeComMessageMetadata(
                title: StringTemplateUtils.createStringFromTemplate(subjectText, bindings)),
                LinkedHashMap.class)

        return message
    }
}
