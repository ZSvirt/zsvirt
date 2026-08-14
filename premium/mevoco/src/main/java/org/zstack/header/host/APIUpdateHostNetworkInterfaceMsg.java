package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;

@RestRequest(
        path = "/hosts/nics/{interfaceUuid}/actions",
        method = HttpMethod.POST,
        responseClass = APIUpdateHostNetworkInterfaceEvent.class,
        parameterName = "params"
)
public class APIUpdateHostNetworkInterfaceMsg extends APIMessage implements HostMessage {
    /**
     * @desc uuid of interface
     */
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private String interfaceUuid;

    /**
     * @desc max length of 2048 characters
     */
    @APIParam(maxLength = 2048)
    private String description;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getHostUuid() {
        return Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .eq(HostNetworkInterfaceVO_.uuid, interfaceUuid).findValue();

    }

    public static APIUpdateHostNetworkInterfaceMsg __example__() {
        APIUpdateHostNetworkInterfaceMsg msg = new APIUpdateHostNetworkInterfaceMsg();
        msg.setInterfaceUuid(uuid(HostNetworkInterfaceVO.class));
        msg.setDescription("test");
        return msg;
    }
}
