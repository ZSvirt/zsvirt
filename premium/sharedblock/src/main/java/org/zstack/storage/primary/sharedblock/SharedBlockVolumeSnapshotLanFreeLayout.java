package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.List;

public class SharedBlockVolumeSnapshotLanFreeLayout {
    private String volumeSnapshotUuid;
    private String volumeUuid;
    private Long virtualSizeBytes;
    private List<SharedBlockVolumeSnapshotLanFreeLayer> layers =
            new ArrayList<>();

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public Long getVirtualSizeBytes() {
        return virtualSizeBytes;
    }

    public void setVirtualSizeBytes(Long virtualSizeBytes) {
        this.virtualSizeBytes = virtualSizeBytes;
    }

    public List<SharedBlockVolumeSnapshotLanFreeLayer> getLayers() {
        return layers;
    }

    public void setLayers(List<SharedBlockVolumeSnapshotLanFreeLayer> layers) {
        this.layers = layers;
    }

    public static SharedBlockVolumeSnapshotLanFreeLayout __example__() {
        SharedBlockVolumeSnapshotLanFreeLayout layout =
                new SharedBlockVolumeSnapshotLanFreeLayout();
        layout.setVolumeSnapshotUuid(
                "7b6f7d4f87f64a33af0a23385e54c2d1");
        layout.setVolumeUuid(
                "8c731f6489034a90a33c463e584a2602");
        layout.setVirtualSizeBytes(107374182400L);
        layout.setLayers(java.util.Collections.singletonList(
                SharedBlockVolumeSnapshotLanFreeLayer.__example__()));
        return layout;
    }
}
