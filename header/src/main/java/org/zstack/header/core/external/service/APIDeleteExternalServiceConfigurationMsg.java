package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:44 AM
 */
@RestRequest(
        path = "/external/service/configurations/{uuid}",
        responseClass = APIDeleteExternalServiceConfigurationEvent.class,
        method = HttpMethod.DELETE
)
public class APIDeleteExternalServiceConfigurationMsg extends APIDeleteMessage implements APIAuditor {
    @APIParam(resourceType = ExternalServiceConfigurationVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteExternalServiceConfigurationMsg)msg).getUuid(), ExternalServiceConfigurationVO.class);
    }

    public static APIDeleteExternalServiceConfigurationMsg __example__() {
        APIDeleteExternalServiceConfigurationMsg msg = new APIDeleteExternalServiceConfigurationMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
