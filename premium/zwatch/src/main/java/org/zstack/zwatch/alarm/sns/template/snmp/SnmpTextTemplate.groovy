package org.zstack.zwatch.alarm.sns.template.snmp

import org.apache.commons.lang.StringUtils
import org.zstack.sns.platform.snmp.SNSSnmpEndpoint
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate

import java.text.SimpleDateFormat
/**
 * @Author : jingwang
 * @create 2023/7/25 6:54 PM
 *
 */
class SnmpTextTemplate extends AbstractTextTemplate implements TextTemplate{
    static final String datePattern = "yyyy-MM-dd HH:mm"

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        def bindings
        if (StringUtils.isNotEmpty(locale)) {
            bindings = locale == "zh_CN" ? makeTemplateBindings(actionParam) :
                    makeTemplateBindings(actionParam, false)
        } else {
            bindings = makeTemplateBindings(actionParam, false)
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_ALARM_TIME, dateFormat.format(bindings.get(PARAM_ALARM_TIME)))

        Map<String, Object> oidBindings = new HashMap<>();
        oidBindings.put(SNSSnmpEndpoint.ALARM_UUID_OID, bindings.get(PARAM_ALARM_UUID))
        oidBindings.put(SNSSnmpEndpoint.ALARM_NAMESPACE_OID, bindings.get(PARAM_ALARM_NAMESPACE))
        String condition = String.format("%s %s %s", bindings.get(PARAM_ALARM_METRIC), bindings.get(PARAM_ALARM_COMPARISON_OPERATOR), bindings.get(PARAM_ALARM_THRESHOLD))
        oidBindings.put(SNSSnmpEndpoint.ALARM_CONDITION, condition)
        oidBindings.put(SNSSnmpEndpoint.ALARM_DURATION_OID, String.format("%s seconds", bindings.get(PARAM_ALARM_DURATION)))
        oidBindings.put(SNSSnmpEndpoint.ALARM_PREVIOUS_STATUS_OID, bindings.get(PARAM_ALARM_PREVIOUS_STATUS))
        oidBindings.put(SNSSnmpEndpoint.ALARM_CURRENT_VALUE_OID, bindings.get(PARAM_ALARM_CURRENT_VALUE))
        oidBindings.put(SNSSnmpEndpoint.ALARM_RESOURCE_ID_OID, bindings.get(PARAM_ALARM_RESOURCE_ID))
        oidBindings.put(SNSSnmpEndpoint.ALARM_RESOURCE_NAME_OID, bindings.get(PARAM_ALARM_RESOURCE_NAME))
        oidBindings.put(SNSSnmpEndpoint.ALARM_EMERGENCY_LEVEL_OID, bindings.get(PARAM_ALARM_EMERGENCY_LEVEL))
        List<String> labels = bindings.get(PARAM_ALARM_LABELS) as List<String>
        oidBindings.put(SNSSnmpEndpoint.ALARM_LABELS_OID, labels.join(","))
        oidBindings.put(SNSSnmpEndpoint.ALARM_TIME_OID, bindings.get(PARAM_ALARM_TIME))
        oidBindings.put(SNSSnmpEndpoint.ALARM_SNMP_TRAP_OID, true)
        oidBindings.replaceAll({ k, v -> Objects.isNull(v) ? "" : v })

        SNSTopicMessage message = new SNSTopicMessage()
        message.metadata = oidBindings
        return message
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        def bindings
        if (StringUtils.isNotEmpty(locale)) {
            bindings = locale == "zh_CN" ? makeTemplateBindings(actionParam) :
                    makeTemplateBindings(actionParam, false)
        } else {
            bindings = makeTemplateBindings(actionParam, false)
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        bindings.put(PARAM_EVENT_TIME, dateFormat.format(bindings.get(PARAM_EVENT_TIME)))

        Map<String, Object> oidBindings = new HashMap<>()
        oidBindings.put(SNSSnmpEndpoint.EVENT_NAME_OID, bindings.get(PARAM_EVENT_RESOURCE_NAME))
        oidBindings.put(SNSSnmpEndpoint.EVENT_NAMESPACE_OID, bindings.get(PARAM_EVENT_NAMESPACE))
        oidBindings.put(SNSSnmpEndpoint.EVENT_EMERGENCY_LEVEL_OID, bindings.get(PARAM_EVENT_EMERGENCY_LEVEL))
        oidBindings.put(SNSSnmpEndpoint.EVENT_RESOURCE_ID_OID, bindings.get(PARAM_EVENT_RESOURCE_ID))
        oidBindings.put(SNSSnmpEndpoint.EVENT_RESOURCE_NAME_OID, bindings.get(PARAM_EVENT_RESOURCE_NAME))
        oidBindings.put(SNSSnmpEndpoint.EVENT_TIME_OID, bindings.get(PARAM_EVENT_TIME))
        oidBindings.put(SNSSnmpEndpoint.EVENT_SUBSCRIPTION_UUID_OID, bindings.get(PARAM_EVENT_SUBSCRIPTION_UUID))
        Map<String, String> labels = bindings.get(PARAM_EVENT_LABELS) as Map<String, String>
        oidBindings.put(SNSSnmpEndpoint.EVENT_LABELS_OID, labels.collect {k, v -> "$k = $v"}.join(','))
        oidBindings.put(SNSSnmpEndpoint.EVENT_ERROR_OID, bindings.get(PARAM_EVENT_ERROR))
        oidBindings.replaceAll({ k, v -> Objects.isNull(v) ? "" : v })

        SNSTopicMessage message = new SNSTopicMessage()
        message.metadata = oidBindings
        return message
    }
}
