package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.core.db.Q
import org.zstack.sns.platform.aliyunsms.AliyunSmsMessageMetadata
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zwatch.alarm.AlarmAction
import org.zstack.zwatch.alarm.AlarmStatus
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory
import org.zstack.zwatch.alarm.sns.SNSTopicMessage
import org.zstack.zwatch.alarm.sns.TextTemplate
import org.zstack.zwatch.alarm.sns.ThirdpartyAbstractTextTemplate

import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * Create by yaoning at 2020/07/18
 */
class AliyunSmsThirdpartyTextTemplate extends ThirdpartyAbstractTextTemplate implements TextTemplate {

    static final char endChar = '…'
    static final int lenLimit = 20
    static final int precisionLimit = 5
    static final String datePattern = "MM-dd HH:mm"
    static final String emptyStr = "None"

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeAlarmActionParam actionParam, String locale) {
        return null
    }

    @Override
    SNSTopicMessage createMessage(SNSTextTemplateInventory template, AlarmAction.TakeEventSubscriptionActionParam actionParam, String locale) {
        AliyunSmsSNSTextTemplateInventory inventory = getAliyunSmsTemplateInv()
        def msg = new SNSTopicMessage()

        Map<String, Object> bindMap = makeTemplateBindings(actionParam)

        AliyunSmsMessageMetadata messageMetadata = new AliyunSmsMessageMetadata()

        messageMetadata.setEVENT_NAMESPACE(stringLimiter((String) bindMap.get(PARAM_EVENT_NAMESPACE), lenLimit))
        messageMetadata.setEVENT_NAME(stringLimiter((String) bindMap.get(PARAM_EVENT_NAME), lenLimit))
        messageMetadata.setEVENT_LABELS(stringLimiter(JSONObjectUtil.toJsonString(bindMap.get(PARAM_EVENT_LABELS)), lenLimit))
        messageMetadata.setEVENT_EMERGENCY_LEVEL(stringLimiter((String) bindMap.get(PARAM_EVENT_EMERGENCY_LEVEL), lenLimit))
        messageMetadata.setEVENT_RESOURCE_ID(stringLimiter((String) bindMap.get(PARAM_EVENT_RESOURCE_ID), lenLimit))
        messageMetadata.setEVENT_RESOURCE_NAME(stringLimiter((String) bindMap.get(PARAM_EVENT_RESOURCE_NAME), lenLimit))

        String event_error = bindMap.get(PARAM_EVENT_ERROR)
        messageMetadata.setEVENT_ERROR(stringLimiter(event_error != null ? event_error : emptyStr, lenLimit))

        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern)
        messageMetadata.setEVENT_TIME(dateFormat.format((Timestamp) bindMap.get(PARAM_EVENT_TIME)))
        messageMetadata.setEVENT_SUBSCRIPTION_UUID(stringLimiter((String) bindMap.get(PARAM_EVENT_SUBSCRIPTION_UUID), lenLimit))
        messageMetadata.setEVENT_ACCOUNT_UUID(stringLimiter((String) bindMap.get(PARAM_EVENT_ACCOUNT_UUID), lenLimit))
        messageMetadata.setEVENT_DATA_UUID(stringLimiter((String) bindMap.get(PARAM_EVENT_DATA_UUID), lenLimit))

        msg.message = JSONObjectUtil.toJsonString(messageMetadata)
        if (inventory != null) {
            messageMetadata.setSmsSign(inventory.getSign())
            messageMetadata.setTemplateCode(inventory.getEventTemplateCode())
        }
        msg.metadata = JSONObjectUtil.rehashObject(messageMetadata, LinkedHashMap.class)
        return msg
    }

    private static AliyunSmsSNSTextTemplateInventory getAliyunSmsTemplateInv() {
        AliyunSmsSNSTextTemplateInventory inventory = null
        AliyunSmsSNSTextTemplateVO vo = Q.New(AliyunSmsSNSTextTemplateVO.class)
                .eq(AliyunSmsSNSTextTemplateVO_.defaultTemplate, true)
                .find()
        if (vo != null) {
            inventory = AliyunSmsSNSTextTemplateInventory.valueOf(vo)
        }
        return inventory
    }

    private static String stringLimiter(String origin, int len) {
        if (origin == null || origin.length() <= len) {
            return origin
        }

        if (len < 2) {
            return ""
        }

        String ret = origin.substring(0, len - 2) + endChar
        return ret
    }

    private static String floutPrecisionLimiter(String origin, int precision) {
        if (origin == null) {
            return origin
        }
        if (precision < 1) {
            precision = 1
        }
        if (origin.matches("[1-9]\\.[1-9]+E[1-9]+")) {
            int dotIdx = origin.indexOf('.')
            int EIdx = origin.indexOf('E')
            int len = EIdx - dotIdx
            if (len <= precision) {
                return origin
            }
            StringBuilder sb = new StringBuilder(origin)
            sb.delete(dotIdx + precision + 1, EIdx)
            return sb.toString()
        }
        return origin
    }
}
