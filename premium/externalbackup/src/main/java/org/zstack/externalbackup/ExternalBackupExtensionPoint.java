package org.zstack.externalbackup;

import org.zstack.header.core.NoErrorCompletion;

import java.util.List;

/**
 * Created by MaJin on 2020/8/19.
 */
public interface ExternalBackupExtensionPoint {
    void beforeBackup(ExternalBackupSpec spec, NoErrorCompletion completion);

    void sortBackupVm(List<CreateVmExternalBackupMessage> vmUuids);

    void failToBackup(ExternalBackupSpec spec, NoErrorCompletion completion);
}
