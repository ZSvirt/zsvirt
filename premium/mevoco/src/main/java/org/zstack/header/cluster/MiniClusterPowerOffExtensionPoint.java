package org.zstack.header.cluster;

import org.zstack.header.core.NoErrorCompletion;

import java.util.List;

public interface MiniClusterPowerOffExtensionPoint {
    void preparePowerOffHost(List<String> hostUuids, NoErrorCompletion completion);

    void beforePowerOffHost(List<String> hostUuids, NoErrorCompletion completion);
}
