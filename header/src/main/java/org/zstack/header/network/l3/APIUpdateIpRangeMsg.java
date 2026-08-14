package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/16/2015.
 */
@RestRequest(
        path = "/l3-networks/ip-ranges/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateIpRangeEvent.class
)
public class APIUpdateIpRangeMsg extends APIMessage implements L3NetworkMessage, IpRangeMessage {
    @APIParam(resourceType = IpRangeVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APINoSee
    private String l3NetworkUuid;

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
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getIpRangeUuid() {
        return uuid;
    }
 
    public static APIUpdateIpRangeMsg __example__() {
        APIUpdateIpRangeMsg msg = new APIUpdateIpRangeMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
