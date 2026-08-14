package org.zstack.nas;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestResponse(allTo = "inventories")
public class APIQueryNasFileSystemReply extends APIQueryReply {
    private List<NasFileSystemInventory> inventories = new ArrayList<>();

    public List<NasFileSystemInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NasFileSystemInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryNasFileSystemReply __example__() {
        APIQueryNasFileSystemReply reply = new APIQueryNasFileSystemReply();

        NasFileSystemInventory nas = new NasFileSystemInventory();
        nas.setUuid(uuid());
        nas.setName("name");
        nas.setFileSystemId("1f617as893");
        nas.setProtocol(NasProtocolType.NFS);
        nas.setType("aliyun");

        reply.setInventories(asList(nas));
        return reply;
    }
}
