package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false)),
        @OverriddenApiParam(field = "zoneUuid", param = @APIParam(maxLength = 1024, required = false, resourceType = ZoneVO.class))
})
@RestRequest(
        path = "/l2-networks/vxlan",
        method = HttpMethod.POST,
        responseClass = APICreateL2VxlanNetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2VxlanNetworkMsg extends APICreateL2NetworkMsg {
    @APIParam(required = false, numberRange = {1, 16777214})
    private Integer vni;

    @APIParam(required = true, resourceType = VxlanNetworkPoolVO.class)
    private String poolUuid;

    public Integer getVni() {
        return vni;
    }

    @Override
    public String getType() {
        return VxlanNetworkConstant.VXLAN_NETWORK_TYPE;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public static APICreateL2VxlanNetworkMsg __example__() {
        APICreateL2VxlanNetworkMsg msg = new APICreateL2VxlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setVni(10);
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPoolUuid(uuid());

        return msg;
    }
}
