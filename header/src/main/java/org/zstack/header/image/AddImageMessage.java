package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;

import java.util.List;

/**
 * Created by MaJin on 2021/3/16.
 */
public interface AddImageMessage {
    void setName(String name);

    String getName();

    void setDescription(String description);

    String getDescription();

    void setResourceUuid(String resourceUuid);

    String getResourceUuid();

    String getMediaType();

    String getGuestOsType();

    void setGuestOsType(String guestOsType);

    boolean isVirtio();

    void setVirtio(boolean virtio);

    String getArchitecture();

    void setArchitecture(String architecture);

    String getPlatform();

    void setPlatform(String platform);

    List<String> getBackupStorageUuids();

    void setBackupStorageUuids(List<String> backupStorageUuids);

    boolean isSystem();

    void setSession(SessionInventory session);

    SessionInventory getSession();

    List<String> getSystemTags();

    void setSystemTags(List<String> systemTags);

    void addSystemTag(String systemTag);

    List<String> getUserTags();
}
