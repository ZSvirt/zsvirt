package org.zstack.accessKey;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeAccessKeyStateEvent extends APIEvent {
    private AccessKeyInventory inventory;

    public static APIChangeAccessKeyStateEvent __example__() {
        APIChangeAccessKeyStateEvent ret = new APIChangeAccessKeyStateEvent();
        AccessKeyInventory inventory = new AccessKeyInventory();
        inventory.setUuid(uuid(AccessKeyVO.class));
        inventory.setAccountUuid(uuid(AccountVO.class));
        inventory.setUserUuid(inventory.getAccountUuid());
        inventory.setState(AccessKeyState.Enabled);
        inventory.setAccessKeyID("1234567890abcdedfhij");
        inventory.setAccessKeySecret("1234567890abcdedfhij1234567890abcdedfhij");

        ret.setInventory(inventory);
        return ret;
    }

    public APIChangeAccessKeyStateEvent() {
    }

    public APIChangeAccessKeyStateEvent(String apiId) {
        super(apiId);
    }

    public AccessKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessKeyInventory inventory) {
        this.inventory = inventory;
    }
}
