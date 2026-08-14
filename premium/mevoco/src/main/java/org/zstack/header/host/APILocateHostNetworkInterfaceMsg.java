package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/{hostUuid}/locate/network-interface",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APILocateHostNetworkInterfaceEvent.class
)
public class APILocateHostNetworkInterfaceMsg extends APIMessage {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(emptyString = false)
    private String networkInterfaceName;

    @APIParam(required = false, numberRange = {1, 255})
    private Long interval = 15L;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getNetworkInterfaceName() {
        return networkInterfaceName;
    }

    public void setNetworkInterfaceName(String networkInterfaceName) {
        this.networkInterfaceName = networkInterfaceName;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public static APILocateHostNetworkInterfaceMsg __example__() {
        APILocateHostNetworkInterfaceMsg msg = new APILocateHostNetworkInterfaceMsg();
        msg.setHostUuid(uuid());
        msg.setNetworkInterfaceName("eth0");
        return msg;
    }
}
