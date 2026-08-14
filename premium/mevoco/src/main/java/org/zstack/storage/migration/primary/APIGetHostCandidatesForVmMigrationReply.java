package org.zstack.storage.migration.primary;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 *  *  *  *  *  * Created by LiangHanYu on 2020/8/26 17:17
 *   *   *   *   *   */
@RestResponse(allTo = "inventories")
public class APIGetHostCandidatesForVmMigrationReply extends APIReply {
    private List<HostInventory> inventories;

    public static APIGetHostCandidatesForVmMigrationReply __example__() {
        APIGetHostCandidatesForVmMigrationReply reply = new APIGetHostCandidatesForVmMigrationReply();
        HostInventory hostInv = new HostInventory();
        hostInv.setUuid(uuid());
        hostInv.setName("HOST-1");
        reply.setInventories(asList(hostInv));
        return reply;
    }

    public List<HostInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostInventory> inventories) {
        this.inventories = inventories;
    }
}
