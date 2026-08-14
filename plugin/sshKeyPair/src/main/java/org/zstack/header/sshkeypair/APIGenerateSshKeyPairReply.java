package org.zstack.header.sshkeypair;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIGenerateSshKeyPairReply extends APIReply {

    private SshPrivateKeyPairInventory inventory;

    public SshPrivateKeyPairInventory getInventory() {
        return inventory;
    }
    public void setInventory(SshPrivateKeyPairInventory inventory) {
        this.inventory = inventory;
    }


    public static APIGenerateSshKeyPairReply __example__() {
        APIGenerateSshKeyPairReply event = new APIGenerateSshKeyPairReply();
        SshPrivateKeyPairInventory inv = new SshPrivateKeyPairInventory();

        inv.setUuid(uuid());
        inv.setName("ssh-key-pair");
        inv.setUuid(uuid());
        inv.setPublicKey("ssh-public-key");
        inv.setPrivateKey("ssh-private-key");
        event.setInventory(inv);
        return event;
    }
}
