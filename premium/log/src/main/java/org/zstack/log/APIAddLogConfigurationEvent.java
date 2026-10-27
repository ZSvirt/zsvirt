package org.zstack.log;

import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddLogConfigurationEvent extends APIEvent {
    private JsonLabelInventory inventory;

    public JsonLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(JsonLabelInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddLogConfigurationEvent() {
    }

    public APIAddLogConfigurationEvent(String apiId) {
        super(apiId);
    }

    public static APIAddLogConfigurationEvent __example__() {
        APIAddLogConfigurationEvent evt = new APIAddLogConfigurationEvent();
        JsonLabelInventory inv = new JsonLabelInventory();
        inv.setLabelKey("log4j2-" + uuid(JsonLabelVO.class));
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
