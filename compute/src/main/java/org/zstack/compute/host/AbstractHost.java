package org.zstack.compute.host;

import org.zstack.header.host.Host;

public abstract class AbstractHost implements Host {
    protected abstract void deleteHook();
}
