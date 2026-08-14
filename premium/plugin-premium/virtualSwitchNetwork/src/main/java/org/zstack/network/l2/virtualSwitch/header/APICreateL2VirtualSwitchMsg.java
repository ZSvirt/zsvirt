package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(L2VirtualSwitchNetworkVO.class)
@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false))
})
@RestRequest(
        path = "/l2-networks/virtual-switch",
        method = HttpMethod.POST,
        responseClass = APICreateL2VirtualSwitchEvent.class,
        parameterName = "params"
)
public class APICreateL2VirtualSwitchMsg extends APICreateL2NetworkMsg {

    /**
     * @desc is distributed switch of standard switch
     */
    @APIParam(required = false)
    private Boolean isDistributed = Boolean.TRUE;

    @Override
    public String getType() {
        return VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE;
    }

    public Boolean getDistributed() {
        return isDistributed;
    }

    public void setDistributed(Boolean distributed) {
        isDistributed = distributed;
    }

    public static APICreateL2VirtualSwitchMsg __example__() {
        APICreateL2VirtualSwitchMsg msg = new APICreateL2VirtualSwitchMsg();

        msg.setName("dvs-1");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPhysicalInterface("bond1");

        return msg;
    }
}
