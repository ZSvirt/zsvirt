package org.zstack.storage.device.nvme;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIRefreshNvmeServerEvent extends APIEvent {
    private NvmeServerInventory inventory;

    public NvmeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(NvmeServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIRefreshNvmeServerEvent(String apiId) {
        super(apiId);
    }

    public APIRefreshNvmeServerEvent() {
        super(null);
    }

    public static APIRefreshNvmeServerEvent __example__() {
        APIRefreshNvmeServerEvent event = new APIRefreshNvmeServerEvent();

        NvmeServerInventory nvmeServerInventory = new NvmeServerInventory();
        nvmeServerInventory.setUuid(uuid());
        nvmeServerInventory.setName("nvme-server-10.0.0.201");
        nvmeServerInventory.setState(StorageDeviceState.Enabled.toString());
        nvmeServerInventory.setIp("10.0.0.201");
        nvmeServerInventory.setPort(3260);
        nvmeServerInventory.setCreateDate(new Timestamp(DocUtils.date));
        nvmeServerInventory.setLastOpDate(new Timestamp(DocUtils.date));

        event.setInventory(nvmeServerInventory);
        event.setSuccess(true);
        return event;
    }
}
