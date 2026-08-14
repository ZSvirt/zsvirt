package org.zstack.sdk;

import org.zstack.sdk.TwoFactorAuthenticationSecretInventory;

public class ResetTwoFactorAuthenticationSecretResult {
    public TwoFactorAuthenticationSecretInventory inventory;
    public void setInventory(TwoFactorAuthenticationSecretInventory inventory) {
        this.inventory = inventory;
    }
    public TwoFactorAuthenticationSecretInventory getInventory() {
        return this.inventory;
    }

}
