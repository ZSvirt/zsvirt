package org.zstack.sns.platform.snmp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSApplicationEndpointTestConnectionMessage;
import org.zstack.sns.SNSApplicationEndpointType;
import org.zstack.sns.SNSApplicationEndpointVO;

@RestRequest(path = "/sns/application-endpoints/snmp/test-connection",
        method = HttpMethod.POST,
        responseClass = APISNSSnmpTestConnectionEvent.class,
        parameterName = "params")
public class APISNSSnmpTestConnectionMsg extends APIMessage implements SNSApplicationEndpointTestConnectionMessage {

    @APIParam(required = false, resourceType = SNSSnmpPlatformVO.class)
    private String platformUuid;

    @APIParam(required = false, resourceType = SNSApplicationEndpointVO.class)
    private String endpointUuid;

    public static APISNSSnmpTestConnectionMsg __example__() {
        APISNSSnmpTestConnectionMsg msg = new APISNSSnmpTestConnectionMsg();
        msg.setPlatformUuid(uuid());
        msg.setEndpointUuid(uuid());
        return msg;
    }

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public SNSApplicationEndpointType getEndpointType() {
        return SNSSnmpPlatformFactory.endpointType;
    }
}
