package org.zstack.header.vpc.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import static java.util.Arrays.asList;

/**
 * Created by shixin on 20/04/2019
 */
@RestResponse(allTo = "inventory")
public class APICreateVpcHaGroupEvent extends APIEvent {
    private VpcHaGroupInventory inventory;

    public APICreateVpcHaGroupEvent() {
        super(null);
    }

    public APICreateVpcHaGroupEvent(String apiId) {
        super(apiId);
    }

    public VpcHaGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VpcHaGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVpcHaGroupEvent __example__() {
        APICreateVpcHaGroupEvent event = new APICreateVpcHaGroupEvent();

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
