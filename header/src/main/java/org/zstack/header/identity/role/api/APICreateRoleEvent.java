package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateRoleEvent extends APIEvent {
    private RoleInventory inventory;

    public APICreateRoleEvent() {
    }

    public APICreateRoleEvent(String apiId) {
        super(apiId);
    }

    public RoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }


    public static APICreateRoleEvent __example__() {
        APICreateRoleEvent event = new APICreateRoleEvent();

        RoleInventory role = RoleInventory.__example__();
        role.setUuid(uuid());
        role.setType(RoleType.Customized.toString());
        role.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        role.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(role);

        return event;
    }
}
