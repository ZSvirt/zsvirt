package org.zstack.nas;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@RestResponse(allTo = "inventories")
public class APIQueryNasMountTargetReply extends APIQueryReply {
    private List<NasMountTargetInventory> inventories = new ArrayList<>();

    public List<NasMountTargetInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NasMountTargetInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryNasMountTargetReply __example__() {
        APIQueryNasMountTargetReply reply = new APIQueryNasMountTargetReply();

        NasMountTargetInventory mount = new NasMountTargetInventory();
        mount.setUuid(uuid());
        mount.setNasFileSystemUuid(uuid());
        mount.setName("name");
        mount.setType("aliyun");
        mount.setMountDomain("test.aliyun.com");

        reply.setInventories(asList(mount));
        return reply;
    }
}
