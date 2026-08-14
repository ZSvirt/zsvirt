package org.zstack.header.storage.backup;

import java.util.List;

/**
 * Created by MaJin on 2019/3/21.
 */
public interface PrimaryStoragePriorityGetter {
    class PrimaryStoragePriority {
        public final int defaultPriority = 10;
        public List<PriorityMap> psPriority;
    }

    class PriorityMap {
        public String PS;
        public Integer priority;
    }

    PrimaryStoragePriority getPrimaryStoragePriority(String imageUuid, String requiredBackupStorageUuid);
}
