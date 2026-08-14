package org.zstack.nas;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateNasFileSystemEvent extends APIEvent {
    private NasFileSystemInventory inventory;

    public APIUpdateNasFileSystemEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateNasFileSystemEvent() {
        super(null);
    }

    public NasFileSystemInventory getInventory() {
        return inventory;
    }

    public void setInventory(NasFileSystemInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateNasFileSystemEvent __example__() {
        APIUpdateNasFileSystemEvent event = new APIUpdateNasFileSystemEvent();

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
