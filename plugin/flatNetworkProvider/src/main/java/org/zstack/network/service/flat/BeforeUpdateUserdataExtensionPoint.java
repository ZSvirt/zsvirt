package org.zstack.network.service.flat;

public interface BeforeUpdateUserdataExtensionPoint {
    void beforeApplyUserdata(String vmUuid, FlatUserdataBackend.UserdataTO to);
}
