package org.zstack.header.core.external.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 12:48 AM
 */
@RestResponse(allTo = "inventory")
public class APIAddExternalServiceConfigurationEvent extends APIEvent {

    private ExternalServiceConfigurationInventory inventory;

    public APIAddExternalServiceConfigurationEvent() {}

    public APIAddExternalServiceConfigurationEvent(String apiId) { super(apiId);}

    public void setInventory(ExternalServiceConfigurationInventory inventory) {this.inventory = inventory;}

    public ExternalServiceConfigurationInventory getInventory() {return inventory;}

    public static APIAddExternalServiceConfigurationEvent __example__() {
        APIAddExternalServiceConfigurationEvent event = new APIAddExternalServiceConfigurationEvent();
        ExternalServiceConfigurationInventory inv = new ExternalServiceConfigurationInventory();

        inv.setUuid(uuid());
        inv.setServiceType("Prometheus2");
        inv.setConfiguration("{}");
        event.setInventory(inv);

        return event;
    }
}
