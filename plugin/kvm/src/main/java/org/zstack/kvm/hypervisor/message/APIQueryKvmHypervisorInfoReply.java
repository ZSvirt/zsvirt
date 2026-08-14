package org.zstack.kvm.hypervisor.message;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@RestResponse(allTo = "inventories")
public class APIQueryKvmHypervisorInfoReply extends APIQueryReply {
    private List<KvmHypervisorInfoInventory> inventories;

    public List<KvmHypervisorInfoInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<KvmHypervisorInfoInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryKvmHypervisorInfoReply __example__() {
        APIQueryKvmHypervisorInfoReply reply = new APIQueryKvmHypervisorInfoReply();
        KvmHypervisorInfoInventory inv = new KvmHypervisorInfoInventory();
        inv.setUuid(uuid());
        inv.setHypervisor("qemu-kvm");
        inv.setVersion("4.2.0-632.g6a6222b.el7");
        inv.setMatchState(HypervisorVersionState.Matched);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setSuccess(true);
        reply.setInventories(asList(inv));
        return reply;
    }
}
