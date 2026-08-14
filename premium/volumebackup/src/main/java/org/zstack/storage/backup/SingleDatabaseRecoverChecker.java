package org.zstack.storage.backup;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.storage.database.backup.DatabaseType;

import static org.zstack.core.Platform.operr;

public class SingleDatabaseRecoverChecker implements DatabaseRecoverChecker {
    @Override
    public String getType() {
        return DatabaseType.singleDatabase.toString();
    }

    @Override
    public void check() {
        if (Q.New(ManagementNodeVO.class).count() > 1){
            throw new OperationFailureException(operr("please stop other node first!"));
        }
    }
}
