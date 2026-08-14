package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

/**
 * use {@link APICreatePortGroupMsg}
 */
@Deprecated
@TagResourceType(L2PortGroupNetworkVO.class)
@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false)),
        @OverriddenApiParam(field = "zoneUuid", param = @APIParam(maxLength = 1024, required = false, resourceType = ZoneVO.class))
})
@RestRequest(
        path = "/l2-networks/port-group",
        method = HttpMethod.POST,
        responseClass = APICreateL2PortGroupEvent.class,
        parameterName = "params"
)
public class APICreateL2PortGroupMsg extends APICreateL2NetworkMsg {

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

    public static APICreateL2PortGroupMsg __example__() {
        APICreateL2PortGroupMsg msg = new APICreateL2PortGroupMsg();

        msg.setName("port-group-1");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setVlan(100);
        msg.setvSwitchUuid(uuid());

        return msg;
    }
}
