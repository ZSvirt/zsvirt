package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.MapField;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.*;

/**
 * Created by frank on 1/4/2016.
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/network-services",
        optionalPaths = "/l3-networks/{l3NetworkUuid}/network-services/{service}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachNetworkServiceFromL3NetworkEvent.class
)
public class APIDetachNetworkServiceFromL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc a map where key is network service provider uuid and value is list of network service types
     */
    @APIParam(required = false)
    @MapField(keyType = String.class, valueType = List.class)
    private Map<String, List<String>> networkServices;

    @APIParam(required = false)
    private String service;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Map<String, List<String>> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Map<String, List<String>> networkServices) {
        this.networkServices = networkServices;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public static APIDetachNetworkServiceFromL3NetworkMsg __example__() {
        APIDetachNetworkServiceFromL3NetworkMsg msg = new APIDetachNetworkServiceFromL3NetworkMsg();

        Map<String, List<String>> m = new HashMap<>();
        m.put(uuid(), Arrays.asList("PortForwarding","EIP"));

        msg.setL3NetworkUuid(uuid());
        msg.setNetworkServices(m);


        return msg;
    }
}
