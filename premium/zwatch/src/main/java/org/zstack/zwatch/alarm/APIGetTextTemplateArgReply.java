package org.zstack.zwatch.alarm;

import com.google.common.collect.ImmutableMap;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.alarm.sns.AbstractTextTemplate;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateType;

import java.util.List;
import java.util.Map;

import static org.zstack.zwatch.alarm.sns.AbstractTextTemplate.defaultSupportedAlarmParams;
import static org.zstack.zwatch.alarm.sns.AbstractTextTemplate.defaultSupportedEventParams;

@RestResponse(allTo = "defaultSupportedParams")
public class APIGetTextTemplateArgReply extends APIReply {
    private Map<String, List<String>> defaultSupportedParams;

    public static APIGetTextTemplateArgReply __example__() {
        APIGetTextTemplateArgReply reply = new APIGetTextTemplateArgReply();
        reply.setDefaultSupportedParams(ImmutableMap.of(
                SNSTextTemplateType.ALARM.toString(), defaultSupportedAlarmParams,
                SNSTextTemplateType.EVENT.toString(), defaultSupportedEventParams));
        return reply;
    }

    public Map<String, List<String>> getDefaultSupportedParams() {
        return defaultSupportedParams;
    }

    public void setDefaultSupportedParams(Map<String, List<String>> defaultSupportedParams) {
        this.defaultSupportedParams = defaultSupportedParams;
    }
}
