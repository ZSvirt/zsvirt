package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.network.IPv6Constants;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APISetVmDnsEvent extends APIEvent {
    private List<VmDnsInventory> inventories;

    public APISetVmDnsEvent() {
        super(null);
    }

    public APISetVmDnsEvent(String apiId) {
        super(apiId);
    }

    public List<VmDnsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmDnsInventory> inventories) {
        this.inventories = inventories;
    }

    public static APISetVmDnsEvent __example__() {
        APISetVmDnsEvent event = new APISetVmDnsEvent();
        VmDnsInventory inv = new VmDnsInventory();
        inv.setVmInstanceUuid(uuid(VmInstanceVO.class));
        inv.setDns("223.5.5.5");
        inv.setIpVersion(IPv6Constants.IPv4);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventories(Collections.singletonList(inv));
        return event;
    }
}

