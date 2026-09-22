package org.zstack.storage.primary.block;

import org.zstack.core.db.Q;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.ha.spi.HaHostChecker;
import org.zstack.header.core.Completion;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/9/7 10:58
 */
public class HaBlockHostChecker implements HaHostChecker {
    private static final CLogger logger = Utils.getLogger(HaBlockHostChecker.class);

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {

        if (!struct.vmInstance.getRootVolume().getInstallPath().startsWith(BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME)) {
            logger.debug("root volume is not on block primary storage, no way to check");
            completion.noWay();
            return;
        }

        String primaryStorageUuid = struct.vmInstance.getRootVolume().getPrimaryStorageUuid();

        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, primaryStorageUuid)
                .find();

        if (blockPrimaryStorageVO == null) {
            logger.debug(String.format("no block storage related to the failure, ps uuid %s", primaryStorageUuid));
            completion.noWay();
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = BlockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);

        String heartbeatLunName = BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_NAME_PREFIX + struct.vmInstance.getRootVolume().getPrimaryStorageUuid();
        BlockScsiLunVO heartbeatLun = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.name, heartbeatLunName)
                .find();

        bkd.checkLunSession(heartbeatLun, struct.hostUuid, new Completion(completion) {
            @Override
            public void success() {
                completion.success(null);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }

    @Override
    public int getWeight() {
        return 5;
    }
}
