package org.zstack.log;

import org.zstack.core.Platform;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetLogConfigurationReply extends APIReply {
    List<JsonLabelInventory> inventories;

    public List<JsonLabelInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<JsonLabelInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetLogConfigurationReply __example__() {
        APIGetLogConfigurationReply evt = new APIGetLogConfigurationReply();
        JsonLabelInventory inv1 = new JsonLabelInventory();
        inv1.setLabelKey("log4j2-" + Platform.getUuid());
        inv1.setLabelValue("{\n" +
                "\"appenderType\": \"Syslog\",\n" +
                "\"configuration\": {\n" +
                "\"hostname\": \"192.168.0.13\",\n" +
                "\"port\": \"514\",\n" +
                "\"protocol\": \"UDP\",\n" +
                "\"facility\": \"LOCAL5\"\n" +
                "}\n" +
                "}");

        JsonLabelInventory inv2 = new JsonLabelInventory();
        inv2.setLabelKey("log4j2-" + Platform.getUuid());
        inv2.setLabelValue("{\n" +
                "\"appenderType\": \"Syslog\",\n" +
                "\"configuration\": {\n" +
                "\"hostname\": \"192.168.0.11\",\n" +
                "\"port\": \"514\",\n" +
                "\"protocol\": \"UDP\",\n" +
                "\"facility\": \"LOCAL5\"\n" +
                "}\n" +
                "}");

        evt.setInventories(Arrays.asList(inv1, inv2));
        return evt;
    }
}
