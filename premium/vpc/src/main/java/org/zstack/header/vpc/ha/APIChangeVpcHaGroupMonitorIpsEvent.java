package org.zstack.header.vpc.ha;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import static java.util.Arrays.asList;


@RestResponse(allTo = "inventory")
public class APIChangeVpcHaGroupMonitorIpsEvent extends APIEvent {

    private VpcHaGroupInventory inventory;

    public APIChangeVpcHaGroupMonitorIpsEvent() {
        super(null);
    }

    public APIChangeVpcHaGroupMonitorIpsEvent(String apiId) {
        super(apiId);
    }

    public VpcHaGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VpcHaGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVpcHaGroupMonitorIpsEvent __example__() {
        APIChangeVpcHaGroupMonitorIpsEvent event = new APIChangeVpcHaGroupMonitorIpsEvent();
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
