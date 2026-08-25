package org.zstack.header.migration;

import java.util.List;

public interface MigrationTlsSanCollector {
    List<String> collectSanForHost(String hostUuid);
}
