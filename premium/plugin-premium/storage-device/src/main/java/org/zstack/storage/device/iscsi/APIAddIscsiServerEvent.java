package org.zstack.storage.device.iscsi;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/2
 */

@RestResponse(allTo = "inventory")
public class APIAddIscsiServerEvent extends APIEvent {
    private IscsiServerInventory inventory;

    public IscsiServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(IscsiServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddIscsiServerEvent(String apiId) {
        super(apiId);
    }

    public APIAddIscsiServerEvent() {
        super(null);
    }

    public static APIAddIscsiServerEvent __example__() {
        APIAddIscsiServerEvent event = new APIAddIscsiServerEvent();

        IscsiServerInventory iscsiServerInventory = new IscsiServerInventory();
        iscsiServerInventory.setUuid(uuid());
        iscsiServerInventory.setName("iscsi-server-10.0.0.201");
        iscsiServerInventory.setState(StorageDeviceState.Enabled.toString());
        iscsiServerInventory.setIp("10.0.0.201");
        iscsiServerInventory.setPort(3260);
        iscsiServerInventory.setChapUserName("username");
        iscsiServerInventory.setChapUserPassword("password");
        iscsiServerInventory.setCreateDate(new Timestamp(DocUtils.date));
        iscsiServerInventory.setLastOpDate(new Timestamp(DocUtils.date));

        event.setInventory(iscsiServerInventory);
        event.setSuccess(true);
        return event;
    }
}
