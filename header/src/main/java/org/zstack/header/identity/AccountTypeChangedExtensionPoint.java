package org.zstack.header.identity;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Extension point for account type change operations.
 * Plugins can implement this interface to perform custom logic
 * when an account type is changed (e.g., promote to admin or demote to normal).
 */
public interface AccountTypeChangedExtensionPoint {
    ErrorCode preAccountTypeChange(String accountUuid, AccountType oldType, AccountType newType);

    void beforeAccountTypeChange(String accountUuid, AccountType oldType, AccountType newType);

    void afterAccountTypeChange(String accountUuid, AccountType newType);
}
