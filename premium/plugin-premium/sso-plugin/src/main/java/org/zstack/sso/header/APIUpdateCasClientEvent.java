package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCasClientEvent extends APIEvent {
    private CasClientInventory inventory;

    public APIUpdateCasClientEvent() {
    }

    public APIUpdateCasClientEvent(String apiId) {
        super(apiId);
    }

    public CasClientInventory getInventory() {
        return inventory;
    }

    public void setInventory(CasClientInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateCasClientEvent __example__() {
        APIUpdateCasClientEvent evt = new APIUpdateCasClientEvent();
        CasClientInventory inv = new CasClientInventory();
        inv.setUuid(uuid());
        inv.setName("test");
        inv.setDescription("test");
        inv.setCasServerLoginUrl("http://zstack.com/login");
        inv.setCasServerUrlPrefix("http://zstack.com");
        inv.setServerName("http://127.0.0.1:8080/");
        inv.setLoginMNUrl("http://127.0.0.1:8080/zstack/sso/cas/");
        evt.setInventory(inv);
        return evt;
    }
}
