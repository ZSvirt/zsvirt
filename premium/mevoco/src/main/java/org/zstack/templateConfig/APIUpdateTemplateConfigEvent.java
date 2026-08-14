package org.zstack.templateConfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateTemplateConfigEvent extends APIEvent{
    private TemplateConfigInventory inventory;

    public APIUpdateTemplateConfigEvent(String apiId) {
        super(apiId);
    }
    public APIUpdateTemplateConfigEvent() {
        super(null);
    }
    public TemplateConfigInventory getInventory() {
        return inventory;
    }
    public void setInventory(TemplateConfigInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateTemplateConfigEvent __example__() {
        APIUpdateTemplateConfigEvent event = new APIUpdateTemplateConfigEvent();
        TemplateConfigInventory inventory  = new TemplateConfigInventory();
        inventory.setCategory("vm");
        inventory.setName("emulateHyperV");
        inventory.setTemplateUuid("000011112222");
        inventory.setValue("true");
        inventory.setDefaultValue("true");
        event.setInventory(inventory);
        return event;
    }
}
