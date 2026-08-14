package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryVolumeResponse extends ExponQueryResponse {
    private List<VolumeModule> volumes;

    public List<VolumeModule> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeModule> volumes) {
        this.volumes = volumes;
    }
}
