package org.zstack.header.vpc.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 2019/07/03.
 */
@RestRequest(
        path = "/vpc/hagroups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVpcHaGroupEvent.class,
        isAction = true
)
public class APIUpdateVpcHaGroupMsg extends APIMessage {
    @APIParam(resourceType = VpcHaGroupVO.class)
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
 
    public static APIUpdateVpcHaGroupMsg __example__() {
        APIUpdateVpcHaGroupMsg msg = new APIUpdateVpcHaGroupMsg();
        msg.setName("ha-1");
        msg.setDescription("vpc ha test");
        msg.setUuid(uuid());

        return msg;
    }
}
