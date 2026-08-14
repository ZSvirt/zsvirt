package org.zstack.header.volume;

import java.util.List;

public interface VolumeCreateMessage {
    String getDiskOfferingUuid();

    void setDiskOfferingUuid(String diskOfferingUuid);

    String getPrimaryStorageUuid();

    void setPrimaryStorageUuid(String primaryStorageUuid);

    long getDiskSize();

    void setDiskSize(long diskSize);

    List<String> getSystemTags();

    void setSystemTags(List<String> systemTags);

    void addSystemTag(String tag);
}
