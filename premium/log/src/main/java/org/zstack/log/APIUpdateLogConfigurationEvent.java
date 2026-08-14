package org.zstack.log;

import org.zstack.core.Platform;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateLogConfigurationEvent extends APIEvent {
    private JsonLabelInventory inventory;

    public JsonLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(JsonLabelInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateLogConfigurationEvent() {
    }

    public APIUpdateLogConfigurationEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateLogConfigurationEvent __example__() {
        APIUpdateLogConfigurationEvent evt = new APIUpdateLogConfigurationEvent();
        JsonLabelInventory inv = new JsonLabelInventory();
        inv.setLabelKey("log4j2-" + Platform.getUuid());
        inv.setLabelValue("{\n" +
                "\"appenderType\": \"Syslog\",\n" +
                "\"configuration\": {\n" +
                "\"hostname\": \"192.168.0.11\",\n" +
                "\"port\": \"514\",\n" +
                "\"protocol\": \"UDP\",\n" +
                "\"facility\": \"LOCAL5\"\n" +
                "}\n" +
                "}");

        evt.setInventory(inv);
        return evt;
    }
}
