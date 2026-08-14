package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;

@RestRequest(
        path = "/hosts/bondings/{bondingUuid}/ip",
        method = HttpMethod.POST,
        responseClass = APISetIpOnHostNetworkBondingEvent.class,
        parameterName = "params"
)
public class APISetIpOnHostNetworkBondingMsg extends APIMessage implements HostMessage {
    /**
     * @desc uuid of bonding which is going to set ip
     */
    @APIParam(resourceType = HostNetworkBondingVO.class)
    private String bondingUuid;

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

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
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
        return Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.hostUuid)
                .eq(HostNetworkBondingVO_.uuid, bondingUuid).findValue();

    }

    public static APISetIpOnHostNetworkBondingMsg __example__() {
        APISetIpOnHostNetworkBondingMsg msg = new APISetIpOnHostNetworkBondingMsg();
        msg.setBondingUuid(uuid(HostNetworkBondingVO.class));
        msg.setIpAddress("192.168.0.1");
        msg.setNetmask("255.255.255.0");
        return msg;
    }
}
