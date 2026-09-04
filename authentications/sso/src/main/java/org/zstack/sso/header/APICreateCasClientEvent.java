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
        CasClientInventory inv = new CasClientInventory();
        inv.setUuid(uuid());
        inv.setState(CasState.Enabled.toString());
        inv.setServerName("http://127.0.0.1");
        inv.setCasServerUrlPrefix("http://zstack.com/login");
        inv.setCasServerUrlPrefix("http://zstack.com");
        inv.setLoginMNUrl("http://127.0.0.1:8080/zstack/sso/cas/");
        evt.setInventory(inv);
        return evt;
    }

}
