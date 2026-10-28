package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@RestResponse(allTo = "inventory")
public class APICreateCasClientEvent extends APIEvent {
    private CasClientInventory inventory;

    public APICreateCasClientEvent() {
    }

    public APICreateCasClientEvent(String apiId) {
        super(apiId);
    }

    public CasClientInventory getInventory() {
        return inventory;
    }

    public void setInventory(CasClientInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateCasClientEvent __example__() {
        APICreateCasClientEvent evt = new APICreateCasClientEvent();
        evt.setInventory(CasClientInventory.__example__());
        return evt;
    }
}
