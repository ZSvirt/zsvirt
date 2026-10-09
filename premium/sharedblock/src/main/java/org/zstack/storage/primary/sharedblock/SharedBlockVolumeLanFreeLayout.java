package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.List;

public class SharedBlockVolumeLanFreeLayout {
    private String volumeUuid;
    private String volumeInstallPath;
    private String format;
    private Long virtualSizeBytes;
    private Long lvSizeBytes;
    private List<SharedBlockLvRange> ranges = new ArrayList<>();

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getVirtualSizeBytes() {
        return virtualSizeBytes;
    }

    public void setVirtualSizeBytes(Long virtualSizeBytes) {
        this.virtualSizeBytes = virtualSizeBytes;
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

    public static SharedBlockVolumeLanFreeLayout __example__() {
        SharedBlockVolumeLanFreeLayout layout = new SharedBlockVolumeLanFreeLayout();
        layout.setVolumeUuid("8c731f6489034a90a33c463e584a2602");
        layout.setVolumeInstallPath(
                "sharedblock://7b6f7d4f87f64a33af0a23385e54c2d1/volume-1");
        layout.setFormat("qcow2");
        layout.setVirtualSizeBytes(10737418240L);
        layout.setLvSizeBytes(205520896L);
        layout.setRanges(java.util.Collections.singletonList(
                SharedBlockLvRange.__example__()));
        return layout;
    }
}
