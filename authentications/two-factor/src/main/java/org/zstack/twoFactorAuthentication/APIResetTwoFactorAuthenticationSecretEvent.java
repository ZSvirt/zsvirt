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
        event.setInventory(TwoFactorAuthenticationSecretInventory.__example__());
        return event;
    }
}
