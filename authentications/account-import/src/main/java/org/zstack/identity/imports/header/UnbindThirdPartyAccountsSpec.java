package org.zstack.identity.imports.header;

import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;

import java.util.ArrayList;
import java.util.List;

public class UnbindThirdPartyAccountsSpec {
    private String sourceUuid;
    private String sourceType;
    private List<String> accountUuidList = new ArrayList<>();
    private boolean removeBindingOnly;
    private SyncDeletedAccountStrategy syncDeleteStrategy;

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

    public List<String> getAccountUuidList() {
        return accountUuidList;
    }

    public void setAccountUuidList(List<String> accountUuidList) {
        this.accountUuidList = accountUuidList;
    }

    public boolean isRemoveBindingOnly() {
        return removeBindingOnly;
    }

    public void setRemoveBindingOnly(boolean removeBindingOnly) {
        this.removeBindingOnly = removeBindingOnly;
    }

    public SyncDeletedAccountStrategy getSyncDeleteStrategy() {
        return syncDeleteStrategy;
    }

    public void setSyncDeleteStrategy(SyncDeletedAccountStrategy syncDeleteStrategy) {
        this.syncDeleteStrategy = syncDeleteStrategy;
    }
}
