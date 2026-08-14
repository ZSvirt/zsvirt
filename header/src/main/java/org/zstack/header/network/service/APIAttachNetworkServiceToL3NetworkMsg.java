package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.*;

/**
 * @api attach network service to l3Network
 * @category network service
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg": {
 * "l3NetworkUuid": "fba7bf08a590444c9e21eee394b61135",
 * "networkServices": {
 * "1d1d5ff248b24906a39f96aa3c6411dd": [
 * "PortForwarding"
 * ]
 * },
 * "session": {
 * "uuid": "d93f354c4339450e8c2a4c31de89da15"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg": {
 * "l3NetworkUuid": "fba7bf08a590444c9e21eee394b61135",
 * "networkServices": {
 * "1d1d5ff248b24906a39f96aa3c6411dd": [
 * "PortForwarding"
 * ]
 * },
 * "session": {
 * "uuid": "d93f354c4339450e8c2a4c31de89da15"
 * },
 * "timeout": 1800000,
 * "id": "ba6534c0256c4166b9742c40b201e956",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAttachNetworkServiceToL3NetworkEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/network-services",
        method = HttpMethod.POST,
        responseClass = APIAttachNetworkServiceToL3NetworkEvent.class,
        parameterName = "params"
)
public class APIAttachNetworkServiceToL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc a map where key is network service provider uuid and value is list of network service types
     */
    @APIParam
    private Map<String, List<String>> networkServices;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public Map<String, List<String>> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Map<String, List<String>> networkServices) {
        this.networkServices = networkServices;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
 
    public static APIAttachNetworkServiceToL3NetworkMsg __example__() {
        APIAttachNetworkServiceToL3NetworkMsg msg = new APIAttachNetworkServiceToL3NetworkMsg();

        Map<String, List<String>> m = new HashMap<>();
        m.put(uuid(), Arrays.asList("PortForwarding"));
        msg.setL3NetworkUuid(uuid());
        msg.setNetworkServices(m);

        return msg;
    }
}
