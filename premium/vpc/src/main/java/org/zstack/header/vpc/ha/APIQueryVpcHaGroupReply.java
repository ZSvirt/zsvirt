package org.zstack.header.vpc.ha;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVpcHaGroupReply extends APIQueryReply {
    private List<VpcHaGroupInventory> inventories;

    public List<VpcHaGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VpcHaGroupInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVpcHaGroupReply __example__() {
        APIQueryVpcHaGroupReply reply = new APIQueryVpcHaGroupReply();

        VpcHaGroupInventory ha = new VpcHaGroupInventory();
        ha.setName("test-vpcha");
        ha.setUuid(uuid());

        VpcHaGroupMonitorIpVO monitorIpVO = new VpcHaGroupMonitorIpVO();
        monitorIpVO.setVpcHaRouterUuid(ha.getUuid());
        monitorIpVO.setMonitorIp("1.1.1.1");
        ha.setMonitors(asList(VpcHaGroupMonitorIpInventory.valueOf(monitorIpVO)));

        VpcHaGroupApplianceVmRefInventory vr = new VpcHaGroupApplianceVmRefInventory();
        vr.setUuid(uuid());
        vr.setVpcHaRouterUuid(ha.getUuid());
        ha.setVrRefs(asList(vr));

        reply.setInventories(list(ha));
        return reply;
    }

}
