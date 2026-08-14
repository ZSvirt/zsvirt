package org.zstack.sns.platform.snmp;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationEndpointEvent;
import org.zstack.sns.APICreateSNSApplicationEndpointMsg;
import org.zstack.sns.SNSApplicationPlatformMessage;

/**
 * @Author : jingwang
 * @create 2023/7/13 7:13 PM
 */
@RestRequest(
        path = "/sns/application-endpoints/snmp",
        method = HttpMethod.POST,
        responseClass = APICreateSNSApplicationEndpointEvent.class,
        parameterName = "params"
)
public class APICreateSNSSnmpEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    public static APICreateSNSSnmpEndpointMsg __example__() {
        APICreateSNSSnmpEndpointMsg msg = new APICreateSNSSnmpEndpointMsg();
        msg.setName("snmp endpoint");
        msg.setPlatformUuid(uuid());
        return msg;
    }
}
