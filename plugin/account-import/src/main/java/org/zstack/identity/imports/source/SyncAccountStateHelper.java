package org.zstack.identity.imports.source;

import org.springframework.lang.NonNull;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountState;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.entity.SyncUpdateAccountStateStrategy;

import static org.zstack.header.identity.AccountState.*;

/**
 * Account state machine
 */
public class SyncAccountStateHelper {
    private SyncCreatedAccountStrategy syncCreateStrategy;
    private SyncDeletedAccountStrategy syncDeleteStrategy;
    private SyncUpdateAccountStateStrategy syncUpdateStrategy;

    public void setSyncCreateStrategy(SyncCreatedAccountStrategy syncCreateStrategy) {
        this.syncCreateStrategy = syncCreateStrategy;
    }

    public void setSyncDeleteStrategy(SyncDeletedAccountStrategy syncDeleteStrategy) {
        this.syncDeleteStrategy = syncDeleteStrategy;
    }

    public void setSyncUpdateStrategy(SyncUpdateAccountStateStrategy syncUpdateStrategy) {
        this.syncUpdateStrategy = syncUpdateStrategy;
    }

    /**
     * @return
     *   null if account should not be created
     */
    public AccountState transformForNewCreateAccount(@NonNull AccountState stateInAccountSource) {
        switch (syncCreateStrategy) {
        case NoAction:
            return null;
        case CreateDisabledAccount:
            return Disabled;
        case CreateAccount: default:
            return stateInAccountSource;
        }
    }

    /**
     * @param originalState
     *   Account original state, not null
     * @param stateInAccountSource
     *   Account state in remote account source, maybe in ldap server
     * @return
     *   Account state which should update to. Return null if account should be deleted
     */
    public AccountState transform(@NonNull AccountState originalState, @NonNull AccountState stateInAccountSource) {
        if (syncUpdateStrategy == SyncUpdateAccountStateStrategy.NoAction || originalState == stateInAccountSource) {
            return originalState;
        }
        if (syncUpdateStrategy == SyncUpdateAccountStateStrategy.KeepSameWithSource) {
            return stateInAccountSource;
        }

        if (stateInAccountSource == Staled) {
            throw new CloudRuntimeException("not support stateInAccountSource[Staled]");
        }
        if (originalState == Staled) {
            return transformForNewCreateAccount(stateInAccountSource);
        }

        return Disabled;
    }

    /**
     * @return
     *   null if account should be deleted
     */
    public AccountState transformForDeletedAccount(@NonNull AccountState originalState) {
        switch (syncDeleteStrategy) {
        case NoAction:
            return originalState;
        case DeleteAccount:
            return null;
        case StaleAccount: default:
            return Staled;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (syncCreateStrategy != null) {
            builder.append("syncCreateStrategy=").append(syncCreateStrategy).append(' ');
        }
        if (syncUpdateStrategy != null) {
            builder.append("syncUpdateStrategy=").append(syncUpdateStrategy).append(' ');
        }
        if (syncDeleteStrategy != null) {
            builder.append("syncDeleteStrategy=").append(syncDeleteStrategy).append(' ');
        }
        return builder.toString();
    }
}
