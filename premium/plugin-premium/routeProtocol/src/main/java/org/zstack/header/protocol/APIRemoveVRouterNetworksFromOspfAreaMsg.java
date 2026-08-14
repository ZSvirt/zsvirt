package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;

@RestRequest(
        path = "/routerArea/networks",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveVRouterNetworksFromOspfAreaEvent.class
)
public class APIRemoveVRouterNetworksFromOspfAreaMsg extends APIDeleteMessage {
    /**
     * @desc ref uuids
     */
    @APIParam(resourceType = NetworkRouterAreaRefVO.class, successIfResourceNotExisting = true)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIRemoveVRouterNetworksFromOspfAreaMsg __example__() {
        APIRemoveVRouterNetworksFromOspfAreaMsg msg = new APIRemoveVRouterNetworksFromOspfAreaMsg();
        msg.setUuids(asList(uuid()));
        return msg;
    }
}
