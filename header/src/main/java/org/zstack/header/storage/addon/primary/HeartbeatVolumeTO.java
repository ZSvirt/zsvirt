package org.zstack.header.storage.addon.primary;

import java.util.List;

public abstract class HeartbeatVolumeTO extends ActiveVolumeTO {
    private Integer hostId;
    private Long heartbeatRequiredSpace;
    private List<String> coveringPaths;

    public Integer getHostId() {
        return hostId;
    }

    public void setHostId(Integer hostId) {
        this.hostId = hostId;
    }


    public Long getHeartbeatRequiredSpace() {
        return heartbeatRequiredSpace;
    }

    public void setHeartbeatRequiredSpace(Long heartbeatRequiredSpace) {
        this.heartbeatRequiredSpace = heartbeatRequiredSpace;
    }

    public List<String> getCoveringPaths() {
        return coveringPaths;
    }

    public void setCoveringPaths(List<String> coveringPaths) {
        this.coveringPaths = coveringPaths;
    }
}