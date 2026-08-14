package org.zstack.header.storage.volume.backup;

import org.zstack.header.identity.SessionInventory;

public interface CreateVolumeTemplateFromBackupMessage {
    String getBackupUuid();
    String getBackupStorageUuid();
    String getName();
    String getDescription();
    String getGuestOsType();
    String getPlatform();
    String getArchitecture();
    boolean isSystem();
    SessionInventory getSession();
    String getResourceUuid();
    Boolean getVirtio();
}
