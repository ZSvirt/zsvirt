package org.zstack.header.protocol;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vpc.VpcRouterVmVO;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/routerArea/{routerAreaUuid}/router/{vRouterUuid}/addnetworks",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddVRouterNetworksToOspfAreaEvent.class
)
public class APIAddVRouterNetworksToOspfAreaMsg extends APICreateMessage implements VmInstanceMessage {
    @APIParam(resourceType = RouterAreaVO.class, nonempty = true)
    private String routerAreaUuid;

    @APIParam(resourceType = VpcRouterVmVO.class, nonempty = true)
    private String vRouterUuid;

    @APIParam(resourceType = L3NetworkVO.class, nonempty = true)
    private List<String> l3NetworkUuids;

    public String getRouterAreaUuid() {
        return routerAreaUuid;
    }

    public void setRouterAreaUuid(String routerAreaUuid) {
        this.routerAreaUuid = routerAreaUuid;
    }

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    @Override
    public String getVmInstanceUuid() {
        return vRouterUuid;
    }

    public static APIAddVRouterNetworksToOspfAreaMsg __example__() {
        APIAddVRouterNetworksToOspfAreaMsg msg = new APIAddVRouterNetworksToOspfAreaMsg();
        msg.setRouterAreaUuid(uuid());
        msg.setvRouterUuid(uuid());
        msg.setL3NetworkUuids(asList(uuid()));

        return msg;
    }
}
