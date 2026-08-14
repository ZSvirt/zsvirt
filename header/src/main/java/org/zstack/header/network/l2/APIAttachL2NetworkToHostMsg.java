package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/{l2NetworkUuid}/hosts/{hostUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachL2NetworkToHostEvent.class,
        parameterName = "params"
)
public class APIAttachL2NetworkToHostMsg extends APIMessage implements L2NetworkMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;
    /**
     * @desc host uuid. See :ref:`HostInventory`
     */
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(required = false, validValues = {L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE})
    private String l2ProviderType;

    @APIParam(required = false)
    private String hostParam;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2ProviderType() {
        return l2ProviderType;
    }

    public void setL2ProviderType(String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }

    public String getHostParam() {
        return hostParam;
    }

    public void setHostParam(String hostParam) {
        this.hostParam = hostParam;
    }

    public static APIAttachL2NetworkToHostMsg __example__() {
        APIAttachL2NetworkToHostMsg msg = new APIAttachL2NetworkToHostMsg();

        msg.setL2NetworkUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setL2ProviderType(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE);

        return msg;
    }
}
