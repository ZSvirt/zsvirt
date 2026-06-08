package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.log.NoLogging;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:49 AM
 */
@RestRequest(
        path = "/external/service/configurations/{uuid}",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateExternalServiceConfigurationEvent.class
)
public class APIUpdateExternalServiceConfigurationMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = ExternalServiceConfigurationVO.class, maxLength = 32, operationTarget = true)
    private String uuid;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 65535, required = false)
    @NoLogging
    private String configuration;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateExternalServiceConfigurationMsg)msg).getUuid(), ExternalServiceConfigurationVO.class);
    }

    public static APIUpdateExternalServiceConfigurationMsg __example__() {
        APIUpdateExternalServiceConfigurationMsg msg = new APIUpdateExternalServiceConfigurationMsg();
        msg.setUuid(uuid());
        msg.setDescription("description");
        msg.setConfiguration("{}");
        return msg;
    }
}
