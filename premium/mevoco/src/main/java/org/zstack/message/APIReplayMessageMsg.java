package org.zstack.message;

import org.zstack.header.message.APIMessage;

/**
 * Created by MaJin on 2020/10/21.
 */

public class APIReplayMessageMsg extends APIMessage {
    private String locationUuid;
    private String locationType;

    public String getLocationUuid() {
        return locationUuid;
    }

    public void setLocationUuid(String locationUuid) {
        this.locationUuid = locationUuid;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
