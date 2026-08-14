package org.zstack.header.acl;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAccessControlListEvent extends APIEvent {
    private AccessControlListInventory inventory;

    public APICreateAccessControlListEvent() { }

    public APICreateAccessControlListEvent(String apiId) {
        super(apiId);
        }

    public void setInventory(AccessControlListInventory inventory) {
        this.inventory = inventory;
        }

    public AccessControlListInventory getInventory() {
        return inventory;
        }

    public static APICreateAccessControlListEvent __example__() {
    APICreateAccessControlListEvent event = new APICreateAccessControlListEvent();
    AccessControlListInventory inv = new AccessControlListInventory();

        inv.setName("acl-group");
        inv.setIpVersion(4);

        event.setInventory(inv);
        return event;
        }
}
