package org.zstack.sns.platform.snmp;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.platform.email.APIQuerySNSEmailPlatformReply;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @Author : jingwang
 * @create 2023/8/24 14:06
 */
@AutoQuery(replyClass = APIQuerySNSSnmpPlatformReply.class, inventoryClass = SNSSnmpPlatformInventory.class)
@RestRequest(path = "/sns/application-platforms/snmp", optionalPaths = {"/sns/application-platforms/snmp/{uuid}"},
        responseClass = APIQuerySNSEmailPlatformReply.class, method = HttpMethod.GET)
public class APIQuerySNSSnmpPlatformMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=snmp");
    }
}
