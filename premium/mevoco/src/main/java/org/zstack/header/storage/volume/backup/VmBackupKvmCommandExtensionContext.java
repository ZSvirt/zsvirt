package org.zstack.header.storage.volume.backup;

import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.volume.VolumeVO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VmBackupKvmCommandExtensionContext {
    private String hostUuid;
    private String vmInstanceUuid;
    private String backupStorageUuid;
    private Collection<VolumeVO> volumes;
    private Map<String, VolumeBackupVO> volumeBackups;
    private Map<String, Integer> deviceIds;
    private Map<String, Object> addons = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();
    private boolean fileTargetRequired;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Collection<VolumeVO> getVolumes() {
        return volumes;
    }

    public void setVolumes(Collection<VolumeVO> volumes) {
        this.volumes = volumes;
    }

    public Map<String, VolumeBackupVO> getVolumeBackups() {
        return volumeBackups;
    }

    public void setVolumeBackups(Map<String, VolumeBackupVO> volumeBackups) {
        this.volumeBackups = volumeBackups;
    }

    public Map<String, Integer> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(Map<String, Integer> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public Map<String, Object> getAddons() {
        return addons;
    }

    public void setAddons(Map<String, Object> addons) {
        this.addons = addons;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean isFileTargetRequired() {
        return fileTargetRequired;
    }

    public void setFileTargetRequired(boolean fileTargetRequired) {
        this.fileTargetRequired = fileTargetRequired;
    }
}
