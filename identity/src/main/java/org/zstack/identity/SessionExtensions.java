package org.zstack.identity;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountState;
import org.zstack.header.identity.AfterUpdateAccountExtensionPoint;

/**
 * Created by Wenhao.Zhang on 2024/06/26
 */
public class SessionExtensions implements
        PasswordUpdateExtensionPoint,
        AfterUpdateAccountExtensionPoint {
    @Override
    public void afterUpdateAccount(AccountInventory account) {
        if (!AccountState.Enabled.toString().equals(account.getState())) {
            Session.logoutAccount(account.getUuid());
        }
    }

    @Override
    public void preUpdatePassword(String accountUuid, String currentPassword, String newPassword) {
        // do-nothing
    }

    @Override
    public void afterUpdatePassword(String accountUuid, String password) {
        Session.logoutAccount(accountUuid);
    }
}
