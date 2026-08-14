package org.zstack.expon.sdk.volume;

import java.util.List;

public class QueryVolumeSnapshotResponse extends QueryVolumeResponse {
    private List<VolumeSnapshotModule> snaps;

    public List<VolumeSnapshotModule> getSnaps() {
        return snaps;
    }

    public void setSnaps(List<VolumeSnapshotModule> snaps) {
        this.snaps = snaps;
    }
}
