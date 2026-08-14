package org.zstack.sso.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2023/7/14
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSSORedirectTemplateEvent extends APIEvent {
    private SSORedirectTemplateInventory inventory;

    public APIUpdateSSORedirectTemplateEvent() {
    }

    public APIUpdateSSORedirectTemplateEvent(String apiId) {
        super(apiId);
    }

    public SSORedirectTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(SSORedirectTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSSORedirectTemplateEvent __example__() {
        APIUpdateSSORedirectTemplateEvent evt = new APIUpdateSSORedirectTemplateEvent();
        SSORedirectTemplateInventory inv = new SSORedirectTemplateInventory();
        inv.setUuid(uuid());
        inv.setClientUuid(uuid());
        inv.setRedirectTemplate("http://zstack.com/code");
        evt.setInventory(inv);
        return evt;
    }
}
