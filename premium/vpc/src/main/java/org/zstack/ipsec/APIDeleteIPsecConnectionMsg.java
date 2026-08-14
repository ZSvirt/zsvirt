package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/11/3.
 */
@RestRequest(
        path = "/ipsec/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteIPsecConnectionEvent.class
)
public class APIDeleteIPsecConnectionMsg extends APIDeleteMessage implements IPsecConnectionMessage {
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

    public static APIDeleteIPsecConnectionMsg __example__() {
        APIDeleteIPsecConnectionMsg msg = new APIDeleteIPsecConnectionMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
