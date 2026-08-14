package org.zstack.header.sshkeypair;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateSshKeyPairEvent extends APIEvent {
    private SshKeyPairInventory inventory;

    public APIUpdateSshKeyPairEvent() {
    }

    public APIUpdateSshKeyPairEvent(String msgId){ super(msgId); }

    public SshKeyPairInventory getInventory() {
        return inventory;
    }
    public void setInventory(SshKeyPairInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSshKeyPairEvent __example__() {
        APIUpdateSshKeyPairEvent event = new APIUpdateSshKeyPairEvent();
        SshKeyPairInventory inv = new SshKeyPairInventory();

        inv.setUuid(uuid());
        inv.setName("ssh-key-pair");
        inv.setUuid(uuid());
        inv.setPublicKey("");
        event.setInventory(inv);
        return event;
    }
}
