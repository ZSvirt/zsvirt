package org.zstack.twoFactorAuthentication;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


@RestResponse(allTo = "inventory")
public class APIResetTwoFactorAuthenticationSecretEvent extends APIEvent {

    private TwoFactorAuthenticationSecretInventory inventory;

    public APIResetTwoFactorAuthenticationSecretEvent() {
    }

    public APIResetTwoFactorAuthenticationSecretEvent(String apiId) {
        super(apiId);
    }

    public TwoFactorAuthenticationSecretInventory getInventory() {
        return inventory;
    }

    public void setInventory(TwoFactorAuthenticationSecretInventory inventory) {
        this.inventory = inventory;
    }

    public static APIResetTwoFactorAuthenticationSecretEvent __example__() {
        APIResetTwoFactorAuthenticationSecretEvent event = new APIResetTwoFactorAuthenticationSecretEvent();
        TwoFactorAuthenticationSecretInventory inventory= new TwoFactorAuthenticationSecretInventory();
        inventory.setUuid(uuid());
        inventory.setSecret("ABCDEFGH12345678");
        inventory.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated.toString());
        inventory.setAccountUuid(uuid());
        event.setInventory(inventory);

        return event;
    }

}
