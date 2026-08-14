package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.NoDoc;
import org.zstack.header.network.l2.APIDeleteL2NetworkEvent;
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg;
import org.zstack.header.rest.NoSDK;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/vxlan/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteL2NetworkEvent.class
)
@NoSDK
@NoDoc
public class APIDeleteVxlanL2Network extends APIDeleteL2NetworkMsg {
    public static APIDeleteL2NetworkMsg __example__() {
        APIDeleteL2NetworkMsg msg = new APIDeleteL2NetworkMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
