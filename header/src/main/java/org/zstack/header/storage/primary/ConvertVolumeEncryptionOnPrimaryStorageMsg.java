package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConvertVolumeEncryptionOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String hostUuid;
    private VolumeInventory volume;
    private boolean targetEncrypted;
    private List<VolumeEncryptionConversionItem> items = new ArrayList<>();

    @Override
    public String getPrimaryStorageUuid() {
        return volume.getPrimaryStorageUuid();
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public boolean isTargetEncrypted() {
        return targetEncrypted;
    }

    public void setTargetEncrypted(boolean targetEncrypted) {
        this.targetEncrypted = targetEncrypted;
    }

    public List<VolumeEncryptionConversionItem> getItems() {
        return items;
    }

    public void setItems(List<VolumeEncryptionConversionItem> items) {
        this.items = items;
    }

    public static class VolumeEncryptionConversionItem implements Serializable {
        private String resourceUuid;
        private String resourceType;
        private String sourceInstallPath;
        private String targetInstallPath;
        private String targetBackingInstallPath;

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getSourceInstallPath() {
            return sourceInstallPath;
        }

        public void setSourceInstallPath(String sourceInstallPath) {
            this.sourceInstallPath = sourceInstallPath;
        }

        public String getTargetInstallPath() {
            return targetInstallPath;
        }

        public void setTargetInstallPath(String targetInstallPath) {
            this.targetInstallPath = targetInstallPath;
        }

        public String getTargetBackingInstallPath() {
            return targetBackingInstallPath;
        }

        public void setTargetBackingInstallPath(String targetBackingInstallPath) {
            this.targetBackingInstallPath = targetBackingInstallPath;
        }
    }
}
