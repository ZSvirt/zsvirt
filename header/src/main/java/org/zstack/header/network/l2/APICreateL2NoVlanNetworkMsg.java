package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 */
@TagResourceType(L2NetworkVO.class)
@RestRequest(
        path = "/l2-networks/no-vlan",
        method = HttpMethod.POST,
        responseClass = APICreateL2NetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2NoVlanNetworkMsg extends APICreateL2NetworkMsg {
    @Override
    public String getType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }
 
    public static APICreateL2NoVlanNetworkMsg __example__() {
        APICreateL2NoVlanNetworkMsg msg = new APICreateL2NoVlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPhysicalInterface("eth0");

        return msg;
    }

}
