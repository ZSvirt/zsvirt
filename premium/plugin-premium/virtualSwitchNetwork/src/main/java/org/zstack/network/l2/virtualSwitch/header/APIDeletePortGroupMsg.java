package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l3.APIDeleteL3NetworkMsg;
import org.zstack.header.rest.RestRequest;

@OverriddenApiParams({
        @OverriddenApiParam(field = "uuid", param = @APIParam(resourceType = PortGroupVO.class, nonempty = true)),
})
@RestRequest(
        path = "/l3-networks/port-group/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePortGroupEvent.class
)
public class APIDeletePortGroupMsg extends APIDeleteL3NetworkMsg {
    public static APIDeletePortGroupMsg __example__() {
        APIDeletePortGroupMsg msg = new APIDeletePortGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
