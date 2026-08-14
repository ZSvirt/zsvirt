package org.zstack.identity.imports.entity;

import org.zstack.identity.imports.message.SyncThirdPartyAccountMsg;

/**
 * <p>When third party source syncing, how to deal with the newly created users
 *
 * <p>This enum is used by:
 * <li>{@link SyncThirdPartyAccountMsg#getCreateAccountStrategy()}
 */
public enum SyncCreatedAccountStrategy {
    /**
     * Do not create AccountVO.
     */
    NoAction,
    /**
     * Create AccountVO binding to the newly created users from third party import source.
     * this account is Disabled
     */
    CreateDisabledAccount,
    /**
     * Create AccountVO binding to the newly created users from third party import source.
     * this account is Enabled
     */
    CreateAccount,
}
