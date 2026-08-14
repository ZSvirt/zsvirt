package org.zstack.header.core.external.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.log.NoLogging;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 12:43 AM
 */
@RestRequest(
        path = "/external/service/configurations",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddExternalServiceConfigurationEvent.class
)
public class APIAddExternalServiceConfigurationMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String externalServiceType;
    @APIParam(maxLength = 65535)
    @NoLogging
    private String configuration;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getExternalServiceType() {
        return externalServiceType;
    }

    public void setExternalServiceType(String externalServiceType) {
        this.externalServiceType = externalServiceType;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        APIAddExternalServiceConfigurationEvent evt = (APIAddExternalServiceConfigurationEvent) rsp;
        return new Result(rsp.isSuccess() ? evt.getInventory().getUuid(): "", ExternalServiceConfigurationVO.class);
    }

    public static APIAddExternalServiceConfigurationMsg __example__() {
        APIAddExternalServiceConfigurationMsg msg = new APIAddExternalServiceConfigurationMsg();
        msg.setExternalServiceType("Prometheus2");
        msg.setConfiguration("{}");
        msg.setDescription("description");
        return msg;
    }
}
