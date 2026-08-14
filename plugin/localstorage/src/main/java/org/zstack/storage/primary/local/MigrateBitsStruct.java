package org.zstack.storage.primary.local;

import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 11/16/2015.
 */
public class MigrateBitsStruct {
    public static class ResourceInfo {
        private LocalStorageResourceRefInventory resourceRef;
        private String path;

        public LocalStorageResourceRefInventory getResourceRef() {
            return resourceRef;
        }

        public void setResourceRef(LocalStorageResourceRefInventory resourceRef) {
            this.resourceRef = resourceRef;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
    private List<ResourceInfo> infos = new ArrayList<ResourceInfo>();
    private String srcHostUuid;
    private String destHostUuid;

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public List<ResourceInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<ResourceInfo> infos) {
        this.infos = infos;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }
}
