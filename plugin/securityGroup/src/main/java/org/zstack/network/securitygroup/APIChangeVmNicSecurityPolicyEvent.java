package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIChangeVmNicSecurityPolicyEvent extends APIEvent {
    private VmNicSecurityPolicyInventory inventory;

    public APIChangeVmNicSecurityPolicyEvent() {
        super(null);
    }

    public APIChangeVmNicSecurityPolicyEvent(String apiId) {
        super(apiId);
    }

    public VmNicSecurityPolicyInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmNicSecurityPolicyInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVmNicSecurityPolicyEvent __example__() {
        APIChangeVmNicSecurityPolicyEvent event = new APIChangeVmNicSecurityPolicyEvent();
        VmNicSecurityPolicyInventory inventory = new VmNicSecurityPolicyInventory();
        inventory.setUuid(uuid());
        inventory.setVmNicUuid(uuid());
        inventory.setIngressPolicy("DENY");
        inventory.setEgressPolicy("ALLOW");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inventory);
        event.setSuccess(true);
        return event;
    }
    

}
