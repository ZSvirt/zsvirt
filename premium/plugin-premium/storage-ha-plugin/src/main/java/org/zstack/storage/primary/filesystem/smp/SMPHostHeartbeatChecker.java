package org.zstack.storage.primary.filesystem.smp;

import org.zstack.ha.HaHostChecker;
import org.zstack.storage.primary.filesystem.AbstractFileSystemHostHeartbeatChecker;
import org.zstack.core.db.Q;
import org.zstack.ha.CheckerStruct;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.smp.SMPConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @Author: DaoDao
 * @Date: 2023/3/8
 */
public class SMPHostHeartbeatChecker extends AbstractFileSystemHostHeartbeatChecker {
    private static final CLogger logger = Utils.getLogger(SMPHostHeartbeatChecker.class);

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(SMPConstants.SMP_TYPE);
    }

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        List<String> currentVmUsedPrimaryStorageUuids = findCurrentVmUsedPrimaryStorageUuids(struct);
        if (currentVmUsedPrimaryStorageUuids.isEmpty()) {
            logger.trace(String.format("the vm[uuid:%s] does not use the primary storage[uuid:%s]",
                    struct.getVmInstance().getUuid(), struct.getVmInstance().getRootVolume().getPrimaryStorageUuid()));
            completion.noWay();
            return;
        }

        List<String> hostUuids = findSiblingHosts(struct, currentVmUsedPrimaryStorageUuids);
        if (hostUuids.isEmpty()) {
            logger.debug(String.format("unable to connect to the primary storage[uuid:%s]: no available hosts",
                    struct.getVmInstance().getRootVolume().getPrimaryStorageUuid()));
            completion.notStable();
            return;
        }

        doFileSystemVmStatusCheck(struct, hostUuids.get(0), completion);
    }

    @Override
    public int getWeight() {
        return 5;
    }
}
