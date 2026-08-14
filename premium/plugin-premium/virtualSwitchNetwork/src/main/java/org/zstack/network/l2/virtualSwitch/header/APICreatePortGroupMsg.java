package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(L3NetworkVO.class)
@OverriddenApiParams({
        @OverriddenApiParam(field = "l2NetworkUuid", param = @APIParam(required = false, resourceType = L2PortGroupNetworkVO.class)),
})
@RestRequest(
        path = "/l3-networks/port-group",
        method = HttpMethod.POST,
        responseClass = APICreatePortGroupEvent.class,
        parameterName = "params"
)
public class APICreatePortGroupMsg extends APICreateL3NetworkMsg {
    @APIParam(resourceType = L2VirtualSwitchNetworkVO.class, scope = APIParam.SCOPE_ALLOWED_ALL)
    private String vSwitchUuid;

    @APIParam(validEnums = PortGroupVlanMode.class, required = false)
    private String vlanMode = PortGroupVlanMode.ACCESS.toString();

    /* for normal case, this is vlanID,
     *  for trunk port, this is the native vlan */
    @APIParam(numberRange = {0, 4094})
    private Integer vlan;

    /* it's used when mode is trunk, vlan ranges can be: [100-200, 300, 400-500],
     * max vlan range number is 10 */
    @APIParam(required = false)
    private String vlanRanges;

    @Override
    public String getType() {
        return VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE;
    }

    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }

    public String getvSwitchUuid() {
        return vSwitchUuid;
    }

    public void setvSwitchUuid(String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }

    public String getVlanMode() {
        return vlanMode;
    }

    public void setVlanMode(String vlanMode) {
        this.vlanMode = vlanMode;
    }

    public String getVlanRanges() {
        return vlanRanges;
    }

    public void setVlanRanges(String vlanRanges) {
        this.vlanRanges = vlanRanges;
    }

    public static APICreatePortGroupMsg __example__() {
        APICreatePortGroupMsg msg = new APICreatePortGroupMsg();

        msg.setName("port-group-1");
        msg.setDescription("Test");
        msg.setvSwitchUuid(uuid());
        msg.setVlanMode(PortGroupVlanMode.ACCESS.toString());
        msg.setVlan(100);
        msg.setType(L3NetworkConstant.L3_BASIC_NETWORK_TYPE);
        msg.setEnableIPAM(Boolean.FALSE);
        msg.setCategory(L3NetworkCategory.Private.toString());
        msg.setIpVersion(4);
        msg.setSystem(Boolean.FALSE);

        return msg;
    }
}
