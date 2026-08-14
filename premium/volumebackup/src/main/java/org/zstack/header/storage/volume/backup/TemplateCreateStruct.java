package org.zstack.header.storage.volume.backup;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.tag.SystemTagInventory;

import java.util.List;

/**
 * Created by kayo on 2018/9/6.
 */
public class TemplateCreateStruct {
    private String backupUuid;
    private String backupStorageUuid;
    private String name;
    private String description;
    private String guestOsType;
    private String platform;
    private String architecture;
    private boolean system;
    private SessionInventory session;
    private String resourceUuid;
    private List<String> vmSystemTags;
    private boolean virtio;

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<String> getVmSystemTags() {
        return vmSystemTags;
    }

    public void setVmSystemTags(List<String> vmSystemTags) {
        this.vmSystemTags = vmSystemTags;
    }

    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }
}