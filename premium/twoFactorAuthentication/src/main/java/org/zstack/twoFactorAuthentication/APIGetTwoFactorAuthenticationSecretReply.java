package org.zstack.twoFactorAuthentication;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIGetTwoFactorAuthenticationSecretReply extends APIReply {
    private TwoFactorAuthenticationSecretInventory inventory;

    public TwoFactorAuthenticationSecretInventory getInventory() {
        return inventory;
    }

    public void setInventory(TwoFactorAuthenticationSecretInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIGetTwoFactorAuthenticationSecretReply __example__() {
        APIGetTwoFactorAuthenticationSecretReply reply = new APIGetTwoFactorAuthenticationSecretReply();
        TwoFactorAuthenticationSecretInventory inventory= new TwoFactorAuthenticationSecretInventory();
        inventory.setUuid(uuid());
        inventory.setSecret("ABCDEFGH12345678");
        inventory.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated.toString());
        inventory.setAccountUuid(uuid());
        reply.setInventory(inventory);
        return reply;
    }

}
