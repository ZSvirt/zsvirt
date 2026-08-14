package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by kayo on 2018/7/26.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateRoleEvent extends APIEvent {
    private RoleInventory inventory;

    public APIUpdateRoleEvent() {
    }

    public APIUpdateRoleEvent(String apiId) {
        super(apiId);
    }

    public RoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }


    public static APIUpdateRoleEvent __example__() {
        APIUpdateRoleEvent event = new APIUpdateRoleEvent();

        RoleInventory role = RoleInventory.__example__();
        role.setUuid(uuid());
        role.setType(RoleType.Customized.toString());
        role.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        role.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(role);

        return event;
    }
}
