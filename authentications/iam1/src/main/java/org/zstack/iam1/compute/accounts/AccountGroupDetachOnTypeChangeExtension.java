package org.zstack.iam1.compute.accounts;

import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.AccountTypeChangedExtensionPoint;
import org.zstack.header.identity.role.RoleAccountRefVO;
import org.zstack.header.identity.role.RoleAccountRefVO_;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO;
import org.zstack.iam1.entity.accounts.AccountGroupAccountRefVO_;

/**
 * Extension implementation to detach account from all groups when account type is changed.
 */
public class AccountGroupDetachOnTypeChangeExtension implements AccountTypeChangedExtensionPoint {

    @Override
    public ErrorCode preAccountTypeChange(String accountUuid, AccountType oldType, AccountType newType) {
        return null;
    }

    @Override
    public void beforeAccountTypeChange(String accountUuid, AccountType oldType, AccountType newType) {
    }

    @Override
    public void afterAccountTypeChange(String accountUuid, AccountType newType) {
        // Detach account from all groups when changed to SystemAdmin
        if (newType != AccountType.SystemAdmin) {
            return;
        }
        detachAccountFromAllGroups(accountUuid);
    }

    private void detachAccountFromAllGroups(String accountUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                // Delete account from group refs
                sql(AccountGroupAccountRefVO.class)
                        .eq(AccountGroupAccountRefVO_.accountUuid, accountUuid)
                        .delete();

                // Delete role refs
                sql(RoleAccountRefVO.class)
                        .eq(RoleAccountRefVO_.accountUuid, accountUuid)
                        .delete();

                // Delete share resource refs
                sql(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                        .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                        .delete();
            }
        }.execute();
    }
}
