package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeQos;
import org.zstack.header.volume.VolumeStats;

import java.util.Collection;
import java.util.List;

public class BaseVolumeInfo extends VolumeStats {
    // optional, zsUUid
    String uuid;
    // optional
    VolumeQos qos;

    protected String primaryStorageUuid;
    protected String protocol;
    // volume or image
    protected String type;
    protected boolean shareable;

    public BaseVolumeInfo(VolumeStats stats) {
        super(stats.getInstallPath(), stats.getActualSize());
    }

    public BaseVolumeInfo() {
        super();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    public boolean isShareable() {
        return shareable;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static BaseVolumeInfo valueOf(VolumeInventory vol) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = vol.getUuid();
        info.setActualSize(vol.getActualSize());
        info.setInstallPath(vol.getInstallPath());
        info.setProtocol(vol.getProtocol());
        info.setQos(VolumeQos.valueOf(vol.getVolumeQos()));
        info.setShareable(vol.isShareable());
        info.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
        info.setType("volume");
        return info;
    }

    public static BaseVolumeInfo valueOf(ImageCacheInventory image, String protocol) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = image.getImageUuid();
        info.setActualSize(image.getSize());
        info.setInstallPath(image.getInstallUrl());
        info.setProtocol(protocol);
        info.setShareable(true);
        info.setPrimaryStorageUuid(image.getPrimaryStorageUuid());
        info.setType("image");
        return info;
    }

    public static BaseVolumeInfo valueOf(VmInstanceSpec.CdRomSpec image) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = image.getImageUuid();
        info.setInstallPath(image.getInstallPath());
        info.setProtocol(image.getProtocol());
        info.setShareable(true);
        info.setPrimaryStorageUuid(image.getPrimaryStorageUuid());
        info.setType("image");
        return info;
    }

    public static BaseVolumeInfo valueOf(VmCdRomVO image) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = image.getIsoUuid();
        info.setInstallPath(image.getIsoInstallPath());
        info.setProtocol(image.getProtocol());
        info.setShareable(true);
        info.setType("image");
        return info;
    }

    public static List<BaseVolumeInfo> valueOf(Collection<VolumeInventory> vols) {
        return vols.stream().map(BaseVolumeInfo::valueOf).collect(java.util.stream.Collectors.toList());
    }

}
