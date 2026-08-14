package org.zstack.storage.backup;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.database.backup.DatabaseType;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.zsha2.ZSha2Helper;
import org.zstack.utils.zsha2.ZSha2Info;

import static org.zstack.core.Platform.operr;

public class MultiDatabaseRecoverChecker implements DatabaseRecoverChecker {
    @Override
    public String getType() {
        return DatabaseType.multiDatabase.toString();
    }

    @Override
    public void check() {
        ZSha2Info info = ZSha2Helper.getInfo();

        ShellResult result = ShellUtils.runAndReturn(String.format(
                "timeout 10 ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no root@%s 'echo 1 > /dev/null'",
                info.getPeerip()));
        if (!result.isReturnCode(0)){
            throw new OperationFailureException(operr("cannot ssh peer node via sshkey, please check connection"));
        }
    }
}
