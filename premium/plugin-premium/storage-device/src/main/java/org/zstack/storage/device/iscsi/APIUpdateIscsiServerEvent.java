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
public class APIUpdateIscsiServerEvent extends APIEvent {
    private IscsiServerInventory inventory;

    public IscsiServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(IscsiServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateIscsiServerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateIscsiServerEvent() {
        super(null);
    }

    public static APIUpdateIscsiServerEvent __example__() {
        APIUpdateIscsiServerEvent event = new APIUpdateIscsiServerEvent();

        IscsiServerInventory iscsiServerInventory = new IscsiServerInventory();
        iscsiServerInventory.setUuid(uuid());
        iscsiServerInventory.setState(StorageDeviceState.Enabled.toString());
        iscsiServerInventory.setName("test-iscsi-server");
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
