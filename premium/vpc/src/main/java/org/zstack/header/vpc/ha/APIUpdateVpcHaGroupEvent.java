package org.zstack.header.vpc.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import static java.util.Arrays.asList;

/**
 * Created by shixin.ruan on 2019/07/03.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVpcHaGroupEvent extends APIEvent {
    private VpcHaGroupInventory inventory;

    public APIUpdateVpcHaGroupEvent() {
    }

    public APIUpdateVpcHaGroupEvent(String apiId) {
        super(apiId);
    }

    public VpcHaGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VpcHaGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateVpcHaGroupEvent __example__() {
        APIUpdateVpcHaGroupEvent event = new APIUpdateVpcHaGroupEvent();

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

        event.setInventory(ha);

        return event;
    }

}
