package org.zstack.nas;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestResponse(allTo = "inventory")
public class APICreateNasFileSystemEvent extends APIEvent {
    private NasFileSystemInventory inventory;

    public APICreateNasFileSystemEvent(String apiId) {
        super(apiId);
    }

    public APICreateNasFileSystemEvent() {
        super(null);
    }

    public NasFileSystemInventory getInventory() {
        return inventory;
    }

    public void setInventory(NasFileSystemInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateNasFileSystemEvent __example__() {
        APICreateNasFileSystemEvent event = new APICreateNasFileSystemEvent();

        NasFileSystemInventory nas = new NasFileSystemInventory();
        nas.setUuid(uuid());
        nas.setName("name");
        nas.setFileSystemId("1f617as893");
        nas.setProtocol(NasProtocolType.NFS);
        nas.setType("aliyun");

        event.setInventory(nas);
        return event;
    }
}
