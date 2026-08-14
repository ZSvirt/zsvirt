package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

public class GetVolumeResponse extends ExponResponse {
    private VolumeModule volumeDetail;

    public VolumeModule getVolumeDetail() {
        return volumeDetail;
    }

    public void setVolumeDetail(VolumeModule volumeDetail) {
        this.volumeDetail = volumeDetail;
    }
}
