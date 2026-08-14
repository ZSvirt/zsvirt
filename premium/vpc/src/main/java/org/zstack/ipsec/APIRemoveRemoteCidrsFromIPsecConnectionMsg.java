package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by shixin on 2017/12/06.
 */
@RestRequest(path = "/ipsec/{uuid}/remote-cidrs",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveRemoteCidrsFromIPsecConnectionEvent.class)
public class APIRemoveRemoteCidrsFromIPsecConnectionMsg extends APIMessage implements IPsecConnectionMessage {

    @APIParam(resourceType = IPsecConnectionVO.class)
    private String uuid;

    @APIParam(nonempty = true, required = true)
    private List<String> peerCidrs;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getPeerCidrs() {
        return peerCidrs;
    }

    public void setPeerCidrs(List<String> peerCidrs) {
        this.peerCidrs = peerCidrs;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIRemoveRemoteCidrsFromIPsecConnectionMsg __example__() {
        APIRemoveRemoteCidrsFromIPsecConnectionMsg msg = new APIRemoveRemoteCidrsFromIPsecConnectionMsg();
        msg.setUuid(uuid());
        msg.setPeerCidrs(Collections.singletonList("192.168.100.0/24"));
        return msg;
    }
}
