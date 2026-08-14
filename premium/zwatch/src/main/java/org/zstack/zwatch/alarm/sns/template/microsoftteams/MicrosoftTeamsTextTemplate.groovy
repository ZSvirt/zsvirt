package org.zstack.zwatch.alarm.sns.template.microsoftteams

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.sns.SNSTopicVO
import org.zstack.sns.SNSTopicVO_
import org.zstack.sns.platform.microsoftteams.MicrosoftTeamsMessageMetadata
import org.zstack.utils.StringTemplateUtils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.AlarmStatus
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate

import javax.persistence.metamodel.SingularAttribute
import java.text.SimpleDateFormat

class MicrosoftTeamsTextTemplate extends AbstractTextTemplate implements TextTemplate {

    static final String ALARM_ENGLISH_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "Alarm Details",
    "sections": [{
        "activityTitle": "Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}",
        "facts": [{
            "name": "Alarm Details:",
            "value": null
        },
        {
            "name": "UUID",
            "value": "${ALARM_UUID}"
        }, {
            "name": "ResourceType",
            "value": "${ALARM_NAMESPACE}"
        }, {
            "name": "Condition",
            "value": "${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR}  ${ALARM_THRESHOLD}"
        }, {
            "name": "Duration",
            "value": "${ALARM_DURATION} seconds"
        },{
            "name": "Current Metric Value",
            "value": "${ALARM_EMERGENCY_LEVEL}"
        }, {
            "name": "Previous Status",
            "value": "${ALARM_PREVIOUS_STATUS}"
        }, {
            "name": "Alarm Resource UUID",
            "value": "${ALARM_RESOURCE_ID}"
        }, {
            "name": "Alarm Resource Name",
            "value": "${ALARM_RESOURCE_NAME}"
        },{
            "name": "Emergency Level",
            "value": "${ALARM_EMERGENCY_LEVEL}"
        }, {
            "name": "Labels",
            "value": "${ALARM_LABELS.join(\",\")}"
        }, {
            "name": "Alarm Trigger Time",
            "value": "${ALARM_TIME}"
        }, {
            "name": "Resource IP",
            "value": "${ALARM_RESOURCE_IP}"
        }, {
            "name": "Cluster UUID",
            "value": "${ALARM_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "Cluster NAME",
            "value": "${ALARM_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}

'''

    static final String ALARM_CHINESE_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "报警详情",
    "sections": [{
        "activityTitle": "报警器 ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}",
        "facts": [{
            "name": "报警详情:",
            "value": null
        },
        {
            "name": "UUID",
            "value": "${ALARM_UUID}"
        }, {
            "name": "资源类型",
            "value": "${ALARM_NAMESPACE}"
        }, {
            "name": "触发条件",
            "value": "${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR}  ${ALARM_THRESHOLD}"
        }, {
            "name": "触发条件持续时间",
            "value": "${ALARM_DURATION} seconds"
        }, {
            "name": "先前状态",
            "value": "${ALARM_PREVIOUS_STATUS}"
        }, {
            "name": "当前值",
            "value": "${ALARM_CURRENT_VALUE}"
        }, {
            "name": "报警资源UUID",
            "value": "${ALARM_RESOURCE_ID}"
        }, {
            "name": "报警资源名称",
            "value": "${ALARM_RESOURCE_NAME}"
        }, {
            "name": "报警级别",
            "value": "${ALARM_EMERGENCY_LEVEL}"
        }, {
            "name": "标签",
            "value": "${ALARM_LABELS.join(\",\")}"
        },{
            "name": "报警触发时间",
            "value": "${ALARM_TIME}"
        }, {
            "name": "报警资源IP",
            "value": "${ALARM_RESOURCE_IP}"
        }, {
            "name": "报警集群UUID",
            "value": "${ALARM_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "报警集群名称",
            "value": "${ALARM_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}
'''

    static final String RECOVERY_CHINESE_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "报警恢复详情",
    "sections": [{
        "activityTitle": "报警器 ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}",
        "facts": [{
            "name": "报警恢复详情:",
            "value": null
        },
        {
            "name": "UUID",
            "value": "${ALARM_UUID}"
        }, {
            "name": "资源类型",
            "value": "${ALARM_NAMESPACE}"
        }, {
            "name": "恢复条件",
            "value": "${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR_REVERSE}  ${ALARM_THRESHOLD}"
        }, {
            "name": "先前状态",
            "value": "${ALARM_PREVIOUS_STATUS}"
        }, {
            "name": "当前值",
            "value": "${ALARM_CURRENT_VALUE}"
        }, {
            "name": "报警资源UUID",
            "value": "${ALARM_RESOURCE_ID}"
        }, {
            "name": "报警资源名称",
            "value": "${ALARM_RESOURCE_NAME}"
        }, {
            "name": "报警触发时间",
            "value": "${ALARM_TIME}"
        }, {
            "name": "报警资源IP",
            "value": "${ALARM_RESOURCE_IP}"
        }, {
            "name": "报警集群UUID",
            "value": "${ALARM_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "报警集群名称",
            "value": "${ALARM_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}

'''

    static final String RECOVERY_ENGLISH_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "Recovery Details",
    "sections": [{
        "activityTitle": "Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}",
        "facts": [{
            "name": "Recovery Details:",
            "value": null
        },
        {
            "name": "UUID",
            "value": "${ALARM_UUID}"
        }, {
            "name": "ResourceType",
            "value": "${ALARM_NAMESPACE}"
        }, {
            "name": "Condition",
            "value": "${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR_REVERSE}  ${ALARM_THRESHOLD}"
        }, {
            "name": "Previous Status",
            "value": "${ALARM_PREVIOUS_STATUS}"
        }, {
            "name": "Current Metric Value",
            "value": "${ALARM_CURRENT_VALUE}"
        }, {
            "name": "Alarm Resource UUID",
            "value": "${ALARM_RESOURCE_ID}"
        }, {
            "name": "Alarm Resource Name",
            "value": "${ALARM_RESOURCE_NAME}"
        }, {
            "name": "Alarm Trigger Time",
            "value": "${ALARM_TIME}"
        }, {
            "name": "Resource IP",
            "value": "${ALARM_RESOURCE_IP}"
        }, {
            "name": "Cluster UUID",
            "value": "${ALARM_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "Cluster Name",
            "value": "${ALARM_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}
'''

    static final String ALARM_ENGLISH_SUBJECT = 'Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}'
    static final String ALARM_CHINESE_SUBJECT = '报警器 ${ALARM_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}'

    static final String RECOVERY_ENGLISH_SUBJECT = 'Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}'
    static final String RECOVERY_CHINESE_SUBJECT = '报警器 ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} 状态改变成 ${ALARM_CURRENT_STATUS}'

    static final String EVENT_ENGLISH_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "Event Details",
    "sections": [{
        "activityTitle": "Event ${EVENT_NAME} Happened",
        "facts": [{
            "name": "Event Details:",
            "value": null
        },
        {
            "name": "Name",
            "value": "${EVENT_NAME}"
        }, {
            "name": "ResourceType",
            "value": "${EVENT_NAMESPACE}"
        }, {
            "name": "Labels",
            "value": "${EVENT_LABELS.collect {k, v -> "$k = $v"}.join(',').replaceAll('\\\\\\\\|\\n|\\r\\n|\"', '')}"
        }, {
            "name": "Emergency Level",
            "value": "${EVENT_EMERGENCY_LEVEL}"
        }, {
            "name": "Resource UUID",
            "value": "${EVENT_RESOURCE_ID}"
        }, {
            "name": "Resource Name",
            "value": "${EVENT_RESOURCE_NAME}"
        }, {
            "name": "Alarm Trigger Time",
            "value": "${EVENT_TIME}"
        }, {
            "name": "Subscription UUID",
            "value": "${EVENT_SUBSCRIPTION_UUID}"
        }, {
            "name": "Error(empty if no error)",
            "value": "${EVENT_ERROR}"
        }, {
            "name": "Resource IP",
            "value": "${EVENT_RESOURCE_IP}"
        }, {
            "name": "Cluster UUID",
            "value": "${EVENT_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "Cluster Name",
            "value": "${EVENT_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}
'''

    static final String EVENT_CHINESE_TEMPLATE = '''{
    "@type": "MessageCard",
    "themeColor": "0076D7",
    "summary": "事件详情",
    "sections": [{
        "activityTitle": "事件 ${EVENT_NAME} 发生了",
        "facts": [{
            "name": "事件详情:",
            "value": null
        },
        {
            "name": "名称",
            "value": "${EVENT_NAME}"
        }, {
            "name": "资源类型",
            "value": "${EVENT_NAMESPACE}"
        }, {
            "name": "标签",
            "value": "${EVENT_LABELS.collect {k, v -> "$k = $v"}.join(',').replaceAll('\\\\\\\\|\\n|\\r\\n|\"', '')}"
        }, {
            "name": "报警级别",
            "value": "${EVENT_EMERGENCY_LEVEL}"
        }, {
            "name": "资源UUID",
            "value": "${EVENT_RESOURCE_ID}"
        }, {
            "name": "资源名称",
            "value": "${EVENT_RESOURCE_NAME}"
        }, {
            "name": "报警触发时间",
            "value": "${EVENT_TIME}"
        }, {
            "name": "事件订阅UUID",
            "value": "${EVENT_SUBSCRIPTION_UUID}"
        }, {
            "name": "错误（如果没有错误时为空）",
            "value": "${EVENT_ERROR}"
        }, {
            "name": "报警资源IP",
            "value": "${EVENT_RESOURCE_IP}"
        }, {
            "name": "报警集群UUID",
            "value": "${EVENT_RESOURCE_CLUSTER_UUID}"
        }, {
            "name": "报警集群名称",
            "value": "${EVENT_RESOURCE_CLUSTER_NAME}"
        }],
        "markdown": true
    }]
}
'''

    static final String EVENT_ENGLISH_SUBJECT = 'Event ${EVENT_NAME} Happened'
    static final String EVENT_CHINESE_SUBJECT = '事件 ${EVENT_NAME} 发生了'
    static final String datePattern = "MM-dd HH:mm"

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        String templateText
        String subjectText
        boolean isRecovery = actionParam.currentStatus == AlarmStatus.OK

        if (template != null) {
            templateText = isRecovery ? template.recoveryTemplate : template.template
        } else if (Platform.getLocale() == Locale.SIMPLIFIED_CHINESE) {
            templateText = isRecovery ? RECOVERY_CHINESE_TEMPLATE : ALARM_CHINESE_TEMPLATE
        } else {
            templateText = isRecovery ? RECOVERY_ENGLISH_TEMPLATE : ALARM_ENGLISH_TEMPLATE
        }

        if (Platform.locale == Locale.SIMPLIFIED_CHINESE) {
            subjectText = isRecovery ? RECOVERY_CHINESE_SUBJECT : ALARM_CHINESE_SUBJECT
        } else {
            subjectText = isRecovery ? RECOVERY_ENGLISH_SUBJECT : ALARM_ENGLISH_SUBJECT
        }
        if (template != null) {
            if (StringUtils.isNotEmpty(template.subject) && !isRecovery) {
                subjectText = template.subject
            } else if (StringUtils.isNotEmpty(template.recoverySubject) && isRecovery) {
                subjectText = template.recoverySubject
            }
        }

        if (template == null && StringUtils.isNotEmpty(locale)) {
            if (locale == "zh_CN") {
                templateText = isRecovery ? RECOVERY_CHINESE_TEMPLATE : ALARM_CHINESE_TEMPLATE
                subjectText = isRecovery ? RECOVERY_CHINESE_SUBJECT : ALARM_CHINESE_SUBJECT
            } else {
                templateText = isRecovery ? RECOVERY_ENGLISH_TEMPLATE : ALARM_ENGLISH_TEMPLATE
                subjectText = isRecovery ? RECOVERY_ENGLISH_SUBJECT : ALARM_ENGLISH_SUBJECT
            }
        }

        def bindings = makeTemplateBindings(actionParam)

        if (StringUtils.isNotEmpty(locale)) {
            bindings = locale == "zh_CN" ? makeTemplateBindings(actionParam) :
                    makeTemplateBindings(actionParam, false)
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_ALARM_TIME, dateFormat.format(bindings.get(PARAM_ALARM_TIME)))
        templateText = templateText.replaceAll("\\\\", "")

        SNSTopicMessage message = new SNSTopicMessage()
        message.message = StringTemplateUtils.createStringFromTemplate(templateText, bindings)
        message.metadata = JSONObjectUtil.rehashObject(new MicrosoftTeamsMessageMetadata(
                title: StringTemplateUtils.createStringFromTemplate(subjectText, bindings)),
                LinkedHashMap.class)
        return message
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        String templateText
        String subjectText

        if (template != null) {
            JsonParser parser = new JsonParser()
            JsonObject object = (JsonObject) parser.parse(template.template)

            JsonObject object1 = new JsonObject()
            object1.addProperty("name", "Labels")
            object1.addProperty("value", '''${EVENT_LABELS.toMapString().replaceAll('\\\\|\\n|\\r\\n|\"', '')}''');

            object.get("sections")[0].get("facts").add(object1)
            templateText = object.toString()
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
        if (template != null && StringUtils.isNotEmpty(template.subject)) {
            subjectText = template.subject
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
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_EVENT_TIME, dateFormat.format(bindings.get(PARAM_EVENT_TIME)))
        SNSTopicMessage message = new SNSTopicMessage()
        message.message = StringTemplateUtils.createStringFromTemplate(templateText, bindings)
        message.metadata = JSONObjectUtil.rehashObject(new MicrosoftTeamsMessageMetadata(
                title: StringTemplateUtils.createStringFromTemplate(subjectText, bindings)),
                LinkedHashMap.class)

        return message
    }
}
