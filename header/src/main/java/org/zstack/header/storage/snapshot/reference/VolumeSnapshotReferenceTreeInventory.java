package org.zstack.header.storage.snapshot.reference;


import org.zstack.header.search.Inventory;
import org.zstack.header.vo.ResourceInventory;

import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeSnapshotReferenceTreeVO.class)
public class VolumeSnapshotReferenceTreeInventory extends ResourceInventory {

    private String primaryStorageUuid;
    private String hostUuid;
    private String rootImageUuid;
    private String rootVolumeUuid;
    private String rootVolumeSnapshotUuid;
    private String rootVolumeSnapshotTreeUuid;
    private String rootInstallUrl;

    public String getRootImageUuid() {
        return rootImageUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
    }

    public String getRootVolumeSnapshotUuid() {
        return rootVolumeSnapshotUuid;
    }

    public void setRootVolumeSnapshotUuid(String rootVolumeSnapshotUuid) {
        this.rootVolumeSnapshotUuid = rootVolumeSnapshotUuid;
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public String getRootVolumeSnapshotTreeUuid() {
        return rootVolumeSnapshotTreeUuid;
    }

    public void setRootVolumeSnapshotTreeUuid(String rootVolumeSnapshotTreeUuid) {
        this.rootVolumeSnapshotTreeUuid = rootVolumeSnapshotTreeUuid;
    }

    public String getRootInstallUrl() {
        return rootInstallUrl;
    }

    public void setRootInstallUrl(String rootInstallUrl) {
        this.rootInstallUrl = rootInstallUrl;
    }

    public static VolumeSnapshotReferenceTreeInventory valueOf(VolumeSnapshotReferenceTreeVO vo) {
        VolumeSnapshotReferenceTreeInventory inv = new VolumeSnapshotReferenceTreeInventory();
        inv.setUuid(vo.getUuid());
        inv.setRootImageUuid(vo.getRootImageUuid());
        inv.setRootVolumeSnapshotUuid(vo.getRootVolumeSnapshotUuid());
        inv.setRootVolumeUuid(vo.getRootVolumeUuid());
        inv.setRootVolumeSnapshotTreeUuid(vo.getRootVolumeSnapshotTreeUuid());
        inv.setRootInstallUrl(vo.getRootInstallUrl());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setHostUuid(vo.getHostUuid());
        return inv;
    }

    public static List<VolumeSnapshotReferenceTreeInventory> valueOf(List<VolumeSnapshotReferenceTreeVO> vos) {
        return vos.stream().map(VolumeSnapshotReferenceTreeInventory::valueOf).collect(Collectors.toList());
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
