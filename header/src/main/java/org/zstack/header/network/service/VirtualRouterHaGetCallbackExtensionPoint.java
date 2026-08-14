package org.zstack.header.network.service;

import java.util.List;

public interface VirtualRouterHaGetCallbackExtensionPoint {
    List<VirtualRouterHaCallbackStruct> getCallback();
}
