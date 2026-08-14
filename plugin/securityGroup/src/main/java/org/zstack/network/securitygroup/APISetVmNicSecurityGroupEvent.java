package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventory")
public class APISetVmNicSecurityGroupEvent extends APIEvent {

    private List<VmNicSecurityGroupRefInventory> inventory;

    public APISetVmNicSecurityGroupEvent() {
    }

    public APISetVmNicSecurityGroupEvent(String apiId) {
        super(apiId);
    }

    public List<VmNicSecurityGroupRefInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<VmNicSecurityGroupRefInventory> inventory) {
        this.inventory = inventory;
    }
 
    public static APISetVmNicSecurityGroupEvent __example__() {
        APISetVmNicSecurityGroupEvent event = new APISetVmNicSecurityGroupEvent();
        VmNicSecurityGroupRefInventory inv = new VmNicSecurityGroupRefInventory();
        inv.setUuid(uuid());
        inv.setPriority(1);
        inv.setSecurityGroupUuid(uuid());
        event.setInventory(asList(inv));
        event.setSuccess(true);
        return event;
    }

}
