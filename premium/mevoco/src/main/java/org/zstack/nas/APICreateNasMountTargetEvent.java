package org.zstack.nas;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/8.
 */
@RestResponse(allTo = "inventory")
public class APICreateNasMountTargetEvent extends APIEvent {
    private NasMountTargetInventory inventory;

    public APICreateNasMountTargetEvent(String apiId) {
        super(apiId);
    }

    public APICreateNasMountTargetEvent() {
        super(null);
    }

    public NasMountTargetInventory getInventory() {
        return inventory;
    }

    public void setInventory(NasMountTargetInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateNasMountTargetEvent __example__() {
        APICreateNasMountTargetEvent event = new APICreateNasMountTargetEvent();

        NasMountTargetInventory mount = new NasMountTargetInventory();
        mount.setUuid(uuid());
        mount.setNasFileSystemUuid(uuid());
        mount.setName("name");
        mount.setType("aliyun");
        mount.setMountDomain("test.aliyun.com");

        event.setInventory(mount);
        return event;
    }
}