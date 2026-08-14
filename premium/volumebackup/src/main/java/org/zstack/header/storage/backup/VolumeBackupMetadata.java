package org.zstack.header.storage.backup;

import org.zstack.header.volume.VolumeType;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/4/28.
 */
public class VolumeBackupMetadata {
    private String uuid;
    private String name;
    private String description;
    private String groupUuid;
    private String installPath;
    private String volumeUuid;
    private String metadata;
    private BackupMode mode;
    private Timestamp createTime;
    private VolumeType type;
    private Long size;
    private String vmInstanceUuid;
    private Map<String, String> attachments = new HashMap<>();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public BackupMode getMode() {
        return mode;
    }

    public void setMode(BackupMode mode) {
        this.mode = mode;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public VolumeType getType() {
        return type;
    }

    public void setType(VolumeType type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

    public static VolumeBackupMetadata valueOf(VolumeBackupVO vo, String bsUuid) {
        VolumeBackupMetadata metadata = new VolumeBackupMetadata();
        metadata.uuid = vo.getUuid();
        metadata.name = vo.getName();
        metadata.description = vo.getDescription();
        metadata.groupUuid = vo.getGroupUuid();
        metadata.installPath = vo.getBackupStorageRefs().stream()
                .filter(it -> it.getBackupStorageUuid().equals(bsUuid))
                .findFirst().orElse(new VolumeBackupStorageRefVO())
                .getInstallPath();
        metadata.volumeUuid = vo.getVolumeUuid();
        metadata.mode = vo.getMode();
        metadata.type = vo.getType();
        metadata.size = vo.getSize();
        metadata.metadata = vo.getMetadata();
        metadata.createTime = vo.getCreateDate();
        metadata.vmInstanceUuid = vo.getVmInstanceUuid();
        return metadata;
    }

    public static VolumeBackupMetadata valueOf(VolumeBackupInventory inv, String bsUuid) {
        VolumeBackupMetadata metadata = new VolumeBackupMetadata();
        metadata.uuid = inv.getUuid();
        metadata.name = inv.getName();
        metadata.description = inv.getDescription();
        metadata.groupUuid = inv.getGroupUuid();
        metadata.installPath = inv.getBackupStorageRefs().stream()
                .filter(it -> it.getBackupStorageUuid().equals(bsUuid))
                .findFirst().orElse(new VolumeBackupStorageRefInventory())
                .getInstallPath();
        metadata.volumeUuid = inv.getVolumeUuid();
        metadata.mode = inv.getMode();
        metadata.type = VolumeType.valueOf(inv.getType());
        metadata.size = inv.getSize();
        metadata.metadata = inv.getMetadata();
        metadata.createTime = inv.getCreateDate();
        metadata.vmInstanceUuid = inv.getVmInstanceUuid();
        return metadata;
    }

    public static List<VolumeBackupMetadata> valueOf(Collection<VolumeBackupVO> invs, String bsUuid) {
        return invs.stream().map(it -> valueOf(it, bsUuid)).collect(Collectors.toList());
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
