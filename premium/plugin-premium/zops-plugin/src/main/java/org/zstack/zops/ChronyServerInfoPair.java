package org.zstack.zops;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public class ChronyServerInfoPair {
    private ChronyServerInfo internal;

    private ChronyServerInfo external;

    public ChronyServerInfo getInternal() {
        return internal;
    }

    public void setInternal(ChronyServerInfo host) {
        this.internal = host;
    }

    public ChronyServerInfo getExternal() {
        return external;
    }

    public void setExternal(ChronyServerInfo external) {
        this.external = external;
    }
}
