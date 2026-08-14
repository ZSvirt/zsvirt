package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by camile on 2017/5/19.
 */
@RestRequest(
        path = "/ipsec/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateIPsecConnectionEvent.class ,
        isAction = true
)
public class APIUpdateIPsecConnectionMsg extends APIMessage implements IPsecConnectionMessage {
    @APIParam(resourceType = IPsecConnectionVO.class)
    private String uuid;

    @APIParam(maxLength = 255 , required = false)
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
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIUpdateIPsecConnectionMsg __example__() {
        APIUpdateIPsecConnectionMsg msg = new APIUpdateIPsecConnectionMsg();

        msg.setUuid(uuid());
        msg.setName("test Ipsec");
        msg.setDescription("info");
        return msg;
    }
}
