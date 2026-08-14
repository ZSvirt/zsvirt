package org.zstack.expon.sdk.vhost;

import org.zstack.expon.sdk.ExponResponse;

public class CreateVhostControllerResponse extends ExponResponse {
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
