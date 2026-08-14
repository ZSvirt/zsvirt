package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

public class CloneVolumeResponse extends ExponResponse {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
