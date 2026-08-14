package org.zstack.header.core.external.service;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetExternalServicesReply extends APIReply {
    private List<ExternalServiceInventory> inventories;

    public List<ExternalServiceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExternalServiceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetExternalServicesReply __example__() {
        APIGetExternalServicesReply reply = new APIGetExternalServicesReply();
        ExternalServiceInventory inv = new ExternalServiceInventory();
        inv.setName("prometheus");
        inv.setStatus(ExternalServiceStatus.RUNNING.toString());
        ExternalServiceCapabilities cap = new ExternalServiceCapabilities();
        cap.setReloadConfig(true);
        inv.setCapabilities(cap);
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
