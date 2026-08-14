package org.zstack.network.hostNetworkInterface.lldp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/hostNetworkInterface/lldp/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeHostNetworkInterfaceLldpModeEvent.class,
        isAction = true
)
public class APIChangeHostNetworkInterfaceLldpModeMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private List<String> interfaceUuids;

    @APIParam(required = false, validValues = {"rx_only", "tx_only", "rx_and_tx", "disable"})
    private String mode = "rx_only";

    public List<String> getInterfaceUuids() {
        return interfaceUuids;
    }

    public void setInterfaceUuids(List<String> interfaceUuids) {
        this.interfaceUuids = interfaceUuids;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public static APIChangeHostNetworkInterfaceLldpModeMsg __example__() {
        APIChangeHostNetworkInterfaceLldpModeMsg msg = new APIChangeHostNetworkInterfaceLldpModeMsg();
        msg.setInterfaceUuids(Collections.singletonList(uuid()));
        msg.setMode("rx_only");
        return msg;
    }
}
