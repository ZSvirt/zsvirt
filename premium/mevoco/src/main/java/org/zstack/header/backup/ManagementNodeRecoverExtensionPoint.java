package org.zstack.header.backup;

import java.util.List;
import java.util.Map;

public interface ManagementNodeRecoverExtensionPoint {
    Map<String, List<NonBackupInfo>> getNonBackupInfos();
    String getServiceId();
}
