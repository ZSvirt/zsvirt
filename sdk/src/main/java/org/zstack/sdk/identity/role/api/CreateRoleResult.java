package org.zstack.sdk.identity.role.api;

import org.zstack.sdk.identity.role.RoleInventory;

public class CreateRoleResult {
    public RoleInventory inventory;
    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }
    public RoleInventory getInventory() {
        return this.inventory;
    }

}
