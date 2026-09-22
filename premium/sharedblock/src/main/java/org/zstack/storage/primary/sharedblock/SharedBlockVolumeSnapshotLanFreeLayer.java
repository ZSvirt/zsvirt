package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.List;

public class SharedBlockVolumeSnapshotLanFreeLayer {
    private Integer layerIndex;
    private Integer parentLayerIndex;
    private String sourceInstallPath;
    private String format;
    private Long lvSizeBytes;
    private List<SharedBlockLvRange> ranges = new ArrayList<>();

    public Integer getLayerIndex() {
        return layerIndex;
    }

    public void setLayerIndex(Integer layerIndex) {
        this.layerIndex = layerIndex;
    }

    public Integer getParentLayerIndex() {
        return parentLayerIndex;
    }

    public void setParentLayerIndex(Integer parentLayerIndex) {
        this.parentLayerIndex = parentLayerIndex;
    }

    public String getSourceInstallPath() {
        return sourceInstallPath;
    }

    public void setSourceInstallPath(String sourceInstallPath) {
        this.sourceInstallPath = sourceInstallPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getLvSizeBytes() {
        return lvSizeBytes;
    }

    public void setLvSizeBytes(Long lvSizeBytes) {
        this.lvSizeBytes = lvSizeBytes;
    }

    public List<SharedBlockLvRange> getRanges() {
        return ranges;
    }

    public void setRanges(List<SharedBlockLvRange> ranges) {
        this.ranges = ranges;
    }

    public static SharedBlockVolumeSnapshotLanFreeLayer __example__() {
        SharedBlockVolumeSnapshotLanFreeLayer layer =
                new SharedBlockVolumeSnapshotLanFreeLayer();
        layer.setLayerIndex(0);
        layer.setSourceInstallPath(
                "sharedblock://3d56b3d5109f4bc5b92ac6e9e159b821/" +
                        "7b6f7d4f87f64a33af0a23385e54c2d1");
        layer.setFormat("qcow2");
        SharedBlockLvRange range = SharedBlockLvRange.__example__();
        layer.setRanges(java.util.Collections.singletonList(range));
        layer.setLvSizeBytes(
                range.getLvOffsetBytes() + range.getLengthBytes());
        return layer;
    }
}
