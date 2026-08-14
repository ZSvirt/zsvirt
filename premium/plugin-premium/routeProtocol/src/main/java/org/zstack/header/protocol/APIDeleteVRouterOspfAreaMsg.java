package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/routerArea/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVRouterOspfAreaEvent.class
)
public class APIDeleteVRouterOspfAreaMsg extends APIDeleteMessage implements AreaMessage {
    /**
     * @desc Router Area uuid
     */
    @APIParam(resourceType = RouterAreaVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAreaUuid() {
        return uuid;
    }

    public static APIDeleteVRouterOspfAreaMsg __example__() {
        APIDeleteVRouterOspfAreaMsg msg = new APIDeleteVRouterOspfAreaMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
