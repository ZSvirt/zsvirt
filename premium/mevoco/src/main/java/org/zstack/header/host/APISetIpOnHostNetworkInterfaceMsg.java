package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;

@RestRequest(
        path = "/hosts/nics/{interfaceUuid}/ip",
        method = HttpMethod.POST,
        responseClass = APISetIpOnHostNetworkInterfaceEvent.class,
        parameterName = "params"
)
public class APISetIpOnHostNetworkInterfaceMsg extends APIMessage implements HostMessage {
    /**
     * @desc uuid of interface which is going to set ip
     */
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private String interfaceUuid;

    /**
     * @desc ip address in IPv4
     */
    @APIParam(required = false)
    private String ipAddress;

    /**
     * @desc netmask in IPv4
     */
    @APIParam(required = false)
    private String netmask;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getHostUuid() {
        return Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .eq(HostNetworkInterfaceVO_.uuid, interfaceUuid).findValue();

    }

    public static APISetIpOnHostNetworkInterfaceMsg __example__() {
        APISetIpOnHostNetworkInterfaceMsg msg = new APISetIpOnHostNetworkInterfaceMsg();
        msg.setInterfaceUuid(uuid(HostNetworkInterfaceVO.class));
        msg.setIpAddress("192.168.0.1");
        msg.setNetmask("255.255.255.0");
        return msg;
    }
}
