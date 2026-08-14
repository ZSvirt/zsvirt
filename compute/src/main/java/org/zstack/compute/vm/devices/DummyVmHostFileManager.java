package org.zstack.compute.vm.devices;

import org.zstack.core.db.SQL;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class DummyVmHostFileManager implements VmHostFileManager {
    private static final CLogger logger = Utils.getLogger(DummyVmHostFileManager.class);

    @Override
    public void cleanVmHostBackupFile(String resourceUuid) {
        logger.debug(String.format("clean VmHostBackupFileVO[resourceUuid=%s]", resourceUuid));
        SQL.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, resourceUuid)
                .delete();
    }
}
