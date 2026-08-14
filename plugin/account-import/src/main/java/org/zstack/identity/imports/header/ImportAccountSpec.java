package org.zstack.identity.imports.header;

import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncUpdateAccountStateStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportAccountSpec {
    private String sourceUuid;
    private String sourceType;
    public List<ImportAccountItem> accountList = new ArrayList<>();
    private boolean createIfNotExist = true;
    private SyncCreatedAccountStrategy syncCreateStrategy = SyncCreatedAccountStrategy.NoAction;
    private SyncUpdateAccountStateStrategy syncUpdateStrategy = SyncUpdateAccountStateStrategy.NoAction;

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

    public List<ImportAccountItem> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<ImportAccountItem> accountList) {
        this.accountList = accountList;
    }

    public boolean isCreateIfNotExist() {
        return createIfNotExist;
    }

    public void setCreateIfNotExist(boolean createIfNotExist) {
        this.createIfNotExist = createIfNotExist;
    }

    public SyncCreatedAccountStrategy getSyncCreateStrategy() {
        return syncCreateStrategy;
    }

    public void setSyncCreateStrategy(SyncCreatedAccountStrategy syncCreateStrategy) {
        this.syncCreateStrategy = syncCreateStrategy;
    }

    public SyncUpdateAccountStateStrategy getSyncUpdateStrategy() {
        return syncUpdateStrategy;
    }

    public void setSyncUpdateStrategy(SyncUpdateAccountStateStrategy syncUpdateStrategy) {
        this.syncUpdateStrategy = syncUpdateStrategy;
    }
}
