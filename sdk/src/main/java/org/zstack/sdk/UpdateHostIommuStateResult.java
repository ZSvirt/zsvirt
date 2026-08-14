package org.zstack.sdk;

import org.zstack.sdk.HostIommuStateType;

public class UpdateHostIommuStateResult {
    public HostIommuStateType state;
    public void setState(HostIommuStateType state) {
        this.state = state;
    }
    public HostIommuStateType getState() {
        return this.state;
    }

}
