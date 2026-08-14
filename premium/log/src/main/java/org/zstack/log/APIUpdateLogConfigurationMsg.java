package org.zstack.log;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/log/configurations",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateLogConfigurationEvent.class
)
public class APIUpdateLogConfigurationMsg extends APIMessage {
    @APIParam
    private long configId;
    @APIParam(required = false)
    private String name;
    @APIParam(required = false)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public static APIUpdateLogConfigurationMsg __example__() {
        APIUpdateLogConfigurationMsg msg = new APIUpdateLogConfigurationMsg();
        msg.setConfigId(1L);

        return msg;
    }
}
