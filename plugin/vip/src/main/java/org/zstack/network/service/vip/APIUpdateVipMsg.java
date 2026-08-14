package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/vips/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVipEvent.class,
        isAction = true
)
public class APIUpdateVipMsg extends APIMessage implements VipMessage {
    @APIParam(resourceType = VipVO.class)
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
    public String getVipUuid() {
        return uuid;
    }
 
    public static APIUpdateVipMsg __example__() {
        APIUpdateVipMsg msg = new APIUpdateVipMsg();
        msg.setName("new name");
        msg.setUuid(uuid());

        return msg;
    }
}
