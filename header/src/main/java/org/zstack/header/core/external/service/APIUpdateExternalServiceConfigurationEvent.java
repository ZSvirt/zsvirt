package org.zstack.header.core.external.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:53 AM
 */
@RestResponse(allTo = "inventory")
public class APIUpdateExternalServiceConfigurationEvent extends APIEvent {
    private ExternalServiceConfigurationInventory inventory;

    public APIUpdateExternalServiceConfigurationEvent() {}

    public APIUpdateExternalServiceConfigurationEvent(String apiId) { super(apiId); }

    public ExternalServiceConfigurationInventory getInventory() {return inventory;}

    public void setInventory(ExternalServiceConfigurationInventory inventory) {this.inventory = inventory;}

    public static APIUpdateExternalServiceConfigurationEvent __example__() {
        APIUpdateExternalServiceConfigurationEvent event = new APIUpdateExternalServiceConfigurationEvent();
        ExternalServiceConfigurationInventory inv = new ExternalServiceConfigurationInventory();

        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
