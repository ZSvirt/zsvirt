package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Qi Le on 2019-07-15
 */
@AutoQuery(replyClass = APIQueryAliyunSmsSNSTextTemplateReply.class, inventoryClass = AliyunSmsSNSTextTemplateInventory.class)
@RestRequest(
        path = "/zwatch/alarms/sns/text-templates/aliyun-sms",
        optionalPaths = {"/zwatch/alarms/sns/text-templates/aliyun-sms/{uuid}"},
        responseClass = APIQueryAliyunSmsSNSTextTemplateReply.class, method = HttpMethod.GET
)
public class APIQueryAliyunSmsSNSTextTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("smsTemplateCode=SMS_123456789");
    }
}
