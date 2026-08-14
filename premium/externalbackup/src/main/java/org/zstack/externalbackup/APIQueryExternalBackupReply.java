package org.zstack.externalbackup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/12/4.
 */

@RestResponse(allTo = "inventories")
public class APIQueryExternalBackupReply extends APIQueryReply {
    private List<ExternalBackupInventory> inventories;

    public List<ExternalBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExternalBackupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryExternalBackupReply __example__() {
        APIQueryExternalBackupReply reply = new APIQueryExternalBackupReply();

        ExternalBackupInventory inv = new ExternalBackupInventory();
        inv.uuid = uuid(ExternalBackupVO.class);
        inv.name = "mybackup";
        inv.state = ExternalBackupState.Ready;
        inv.installPath = "/var/zbox-129ed716d2/zstack-backup/mybackup-3.9.0-6ecb68135490414793fc7d1233254a18";
        inv.totalSize = SizeUnit.GIGABYTE.toByte(500);
        inv.type = "zbox";
        inv.version = "3.9.0";
        reply.inventories = Collections.singletonList(inv);
        return reply;
    }
}
