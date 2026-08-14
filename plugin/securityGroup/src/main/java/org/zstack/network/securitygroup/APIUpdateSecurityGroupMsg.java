package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/security-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateSecurityGroupEvent.class
)
public class APIUpdateSecurityGroupMsg extends APIMessage implements SecurityGroupMessage {
    
    @APIParam(resourceType = SecurityGroupVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

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
    public String getSecurityGroupUuid() {
        return uuid;
    }
 
    public static APIUpdateSecurityGroupMsg __example__() {
        APIUpdateSecurityGroupMsg msg = new APIUpdateSecurityGroupMsg();
        msg.setUuid(uuid());
        msg.setName("new sg");
        msg.setDescription("for test update");
        return msg;
    }
}
