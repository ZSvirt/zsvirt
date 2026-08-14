package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/host/network-interface-vlan-ids",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateInterfaceVlanIdsReply.class
)
public class APIGetCandidateInterfaceVlanIdsMsg extends APIGetMessage {
    /**
     * @desc uuids of interfaces or bondings which are going to set service types
     */
    @APIParam
    private List<String> interfaceUuids;

    public List<String> getInterfaceUuids() {
        return interfaceUuids;
    }

    public void setInterfaceUuids(List<String> interfaceUuids) {
        this.interfaceUuids = interfaceUuids;
    }

    public static APIGetCandidateInterfaceVlanIdsMsg __example__() {
        APIGetCandidateInterfaceVlanIdsMsg msg = new APIGetCandidateInterfaceVlanIdsMsg();
        msg.setInterfaceUuids(Collections.singletonList(uuid()));
        return msg;
    }
}
