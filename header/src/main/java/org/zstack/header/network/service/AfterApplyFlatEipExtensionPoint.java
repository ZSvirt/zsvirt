package org.zstack.header.network.service;

import java.util.List;

public interface AfterApplyFlatEipExtensionPoint {
    void AfterApplyFlatEip(List<String> vipUuids, String hostUuid);
}
