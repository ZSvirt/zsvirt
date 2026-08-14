package org.zstack.log;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/log/configurations/log4j2",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLogConfigurationEvent.class
)
public class APIDeleteLogConfigurationMsg extends APIDeleteMessage {
    @APIParam
    private Long configId;

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public static APIDeleteLogConfigurationMsg __example__() {
        APIDeleteLogConfigurationMsg msg = new APIDeleteLogConfigurationMsg();
        msg.setConfigId(0L);

        return msg;
    }
}
