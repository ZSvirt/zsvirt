package org.zstack.header.sshkeypair;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSshKeyPairEvent extends APIEvent {
    private SshKeyPairInventory inventory;

    public APICreateSshKeyPairEvent() {
    }

    public APICreateSshKeyPairEvent(String msgId){ super(msgId); }

    public SshKeyPairInventory getInventory() {
        return inventory;
    }
    public void setInventory(SshKeyPairInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSshKeyPairEvent __example__() {
        APICreateSshKeyPairEvent event = new APICreateSshKeyPairEvent();
        SshKeyPairInventory inv = new SshKeyPairInventory();

        inv.setUuid(uuid());
        inv.setName("ssh-key-pair");
        inv.setUuid(uuid());
        inv.setPublicKey("");
        event.setInventory(inv);
        return event;
    }
}
