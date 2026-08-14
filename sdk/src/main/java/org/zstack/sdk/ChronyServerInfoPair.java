package org.zstack.sdk;

import org.zstack.sdk.ChronyServerInfo;
import org.zstack.sdk.ChronyServerInfo;

public class ChronyServerInfoPair  {

    public ChronyServerInfo internal;
    public void setInternal(ChronyServerInfo internal) {
        this.internal = internal;
    }
    public ChronyServerInfo getInternal() {
        return this.internal;
    }

    public ChronyServerInfo external;
    public void setExternal(ChronyServerInfo external) {
        this.external = external;
    }
    public ChronyServerInfo getExternal() {
        return this.external;
    }

}
