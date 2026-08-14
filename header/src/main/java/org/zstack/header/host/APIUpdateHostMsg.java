package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/hosts/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateHostEvent.class,
        isAction = true
)
public class APIUpdateHostMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String managementIp;

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public static APIUpdateHostMsg __example__() {
        APIUpdateHostMsg msg = new APIUpdateHostMsg();
        msg.setUuid(uuid());
        msg.setDescription("example");
        msg.setManagementIp("192.168.0.1");
        msg.setName("example");
        return msg;
    }
}
