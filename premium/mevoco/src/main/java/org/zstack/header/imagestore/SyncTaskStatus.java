package org.zstack.header.imagestore;

/**
 * Created by mingjian.deng on 2017/9/13.
 */
public enum SyncTaskStatus {
    TsWaiting("waiting"),
    TsRunning("running"),
    TsSuccess("success"),
    TsFailed("failed");

    String type;

    SyncTaskStatus(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static SyncTaskStatus get(String type) {
        for (SyncTaskStatus tmp: SyncTaskStatus.values()) {
            if (tmp.toString().equalsIgnoreCase(type)) {
                return tmp;
            }
        }
        return SyncTaskStatus.valueOf(type);
    }
}
