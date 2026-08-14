package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by shixin on 2017/12/06.
 */
@RestRequest(path = "/ipsec/{uuid}/l3networks",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachL3NetworksToIPsecConnectionEvent.class)
public class APIAttachL3NetworksToIPsecConnectionMsg extends APICreateMessage implements IPsecConnectionMessage {

    @APIParam(resourceType = IPsecConnectionVO.class)
    private String uuid;

    @APIParam(resourceType = L3NetworkVO.class)
    private List<String> l3NetworkUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIAttachL3NetworksToIPsecConnectionMsg __example__() {
        APIAttachL3NetworksToIPsecConnectionMsg msg = new APIAttachL3NetworksToIPsecConnectionMsg();
        msg.setUuid(uuid());
        msg.setL3NetworkUuids(Collections.singletonList(uuid()));
        return msg;
    }
}
