package org.zstack.storage.backup;

import java.util.List;

/**
 * Created by MaJin on 2019/12/9.
 */
public class DatabaseBackupLongJobParams {
    private List<String> alternativeBackupStorageUuids;
    private String name;
    private String description;

    public List<String> getAlternativeBackupStorageUuids() {
        return alternativeBackupStorageUuids;
    }

    public void setAlternativeBackupStorageUuids(List<String> alternativeBackupStorageUuids) {
        this.alternativeBackupStorageUuids = alternativeBackupStorageUuids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
