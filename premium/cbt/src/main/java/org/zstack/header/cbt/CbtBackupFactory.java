package org.zstack.header.cbt;

import org.zstack.storage.cbt.CbtBackupHypervisorBackend;

public interface CbtBackupFactory {
    String getHypervisorType();

    CbtBackupHypervisorBackend getHypervisorBackend();
}
