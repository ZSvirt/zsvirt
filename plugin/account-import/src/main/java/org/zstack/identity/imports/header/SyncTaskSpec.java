package org.zstack.identity.imports.header;

import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;

/**
 * {@link SyncTaskSpec} -> {@link ImportAccountSpec} and {@link ImportAccountItem} ->
 * {@link ImportAccountResult}
 */
public class SyncTaskSpec {
    public String sourceUuid;
    public String sourceType;
    public SyncCreatedAccountStrategy createAccountStrategy;
    public SyncDeletedAccountStrategy deleteAccountStrategy;

    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public SyncCreatedAccountStrategy getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(SyncCreatedAccountStrategy createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public SyncDeletedAccountStrategy getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(SyncDeletedAccountStrategy deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
    }
}
