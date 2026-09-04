package org.zstack.twoFactorAuthentication;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;


@RestResponse(allTo = "inventories")
public class APIQueryTwoFactorAuthenticationReply extends APIQueryReply {
    private List<TwoFactorAuthenticationSecretInventory> inventories;

    public List<TwoFactorAuthenticationSecretInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TwoFactorAuthenticationSecretInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryTwoFactorAuthenticationReply __example__() {
        APIQueryTwoFactorAuthenticationReply reply = new APIQueryTwoFactorAuthenticationReply();

        TwoFactorAuthenticationSecretInventory inventory = new TwoFactorAuthenticationSecretInventory();
        inventory.setUuid(uuid());
        inventory.setSecret("ABCDEFGH12345678");
        inventory.setAccountUuid(uuid());
        inventory.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated.toString());

        reply.setInventories(list(inventory));
        return reply;
    }

}
