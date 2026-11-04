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
        reply.setInventory(TwoFactorAuthenticationSecretInventory.__example__());
        return reply;
    }
}
