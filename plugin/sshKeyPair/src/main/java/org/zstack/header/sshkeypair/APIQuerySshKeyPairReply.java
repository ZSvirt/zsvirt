package org.zstack.header.sshkeypair;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQuerySshKeyPairReply extends APIQueryReply {
    private List<SshKeyPairInventory> inventories;

    public List<SshKeyPairInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SshKeyPairInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySshKeyPairReply __example__() {
        APIQuerySshKeyPairReply reply = new APIQuerySshKeyPairReply();
        SshKeyPairInventory inv = new SshKeyPairInventory();

        inv.setUuid(uuid());
        inv.setName("ssh-key-pair");
        inv.setUuid(uuid());
        inv.setPublicKey("");
        reply.setInventories(list(inv));
        return reply;
    }
}
