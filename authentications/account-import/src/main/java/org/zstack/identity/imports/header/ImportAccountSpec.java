package org.zstack.identity.imports.header;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncUpdateAccountStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportAccountSpec {
    private String sourceUuid;
    private String sourceType;
    public List<ImportAccountItem> accountList = new ArrayList<>();
    private SyncCreatedAccountStrategy syncCreateStrategy = SyncCreatedAccountStrategy.NoAction;
    private List<SyncUpdateAccountStrategy> syncUpdateStrategies = new ArrayList<>();

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

    public SyncCreatedAccountStrategy getSyncCreateStrategy() {
        return syncCreateStrategy;
    }

    public void setSyncCreateStrategy(SyncCreatedAccountStrategy syncCreateStrategy) {
        this.syncCreateStrategy = syncCreateStrategy;
    }

    public List<SyncUpdateAccountStrategy> getSyncUpdateStrategies() {
        return syncUpdateStrategies;
    }

    public void setSyncUpdateStrategies(List<SyncUpdateAccountStrategy> syncUpdateStrategies) {
        this.syncUpdateStrategies = syncUpdateStrategies;
    }

    public boolean isCreateIfNotExist() {
        return syncCreateStrategy != SyncCreatedAccountStrategy.NoAction;
    }

    public boolean hasUpdateAccountStrategy(SyncUpdateAccountStrategy... strategies) {
        if (CollectionUtils.isEmpty(this.syncUpdateStrategies)) {
            return false;
        }
        return Arrays.stream(strategies).anyMatch(it -> syncUpdateStrategies.contains(it));
    }
}
