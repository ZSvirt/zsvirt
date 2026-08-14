package org.zstack.sdk;

import org.zstack.sdk.HostIommuStatusType;

public class GetHostIommuStatusResult {
    public HostIommuStatusType status;
    public void setStatus(HostIommuStatusType status) {
        this.status = status;
    }
    public HostIommuStatusType getStatus() {
        return this.status;
    }

}
