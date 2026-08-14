package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetVolumeBoundPathResponse extends ExponResponse {
    List<String> path;

    public void setPath(List<String> path) {
        this.path = path;
    }

    public List<String> getPath() {
        return path;
    }
}
