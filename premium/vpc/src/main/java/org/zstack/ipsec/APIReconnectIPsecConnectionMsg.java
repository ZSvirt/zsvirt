package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 2022/8/10.
 */
@RestRequest(
        path = "/ipsec/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIReconnectIPsecConnectionEvent.class,
        isAction = true
)
public class APIReconnectIPsecConnectionMsg extends APIMessage implements IPsecConnectionMessage {
    @APIParam(resourceType = IPsecConnectionVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIReconnectIPsecConnectionMsg __example__() {
        APIReconnectIPsecConnectionMsg msg = new APIReconnectIPsecConnectionMsg();

        msg.setUuid(uuid());
        return msg;
    }
}
