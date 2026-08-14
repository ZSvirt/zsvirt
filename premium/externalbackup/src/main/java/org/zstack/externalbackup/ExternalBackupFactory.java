package org.zstack.externalbackup;

import org.zstack.header.core.workflow.FlowChain;

/**
 * Created by MaJin on 2019/12/3.
 */
public interface ExternalBackupFactory {
    String getBackupType();

    ExternalBackupVO createExternalBackup(ExternalBackupVO vo, CreateExternalBackupMsg msg);

    ExternalBackupInventory getInventory(ExternalBackupVO vo);

    ExternalBackupVO getEntity(String uuid);

    ExternalBackup getExternalBackup(ExternalBackupVO vo);

    ExternalBackupSpec createBackupSpec(ExternalBackupVO vo, CreateExternalBackupMsg msg);

    FlowChain getCreateBackupFlowChain();

    FlowChain getRecoverBackupFlowChain();
}
