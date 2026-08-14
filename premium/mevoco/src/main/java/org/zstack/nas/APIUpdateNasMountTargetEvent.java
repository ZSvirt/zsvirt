package org.zstack.nas;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateNasMountTargetEvent extends APIEvent {
    private NasMountTargetInventory inventory;

    public APIUpdateNasMountTargetEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateNasMountTargetEvent() {
        super(null);
    }

    public NasMountTargetInventory getInventory() {
        return inventory;
    }

    public void setInventory(NasMountTargetInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateNasMountTargetEvent __example__() {
        APIUpdateNasMountTargetEvent event = new APIUpdateNasMountTargetEvent();

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