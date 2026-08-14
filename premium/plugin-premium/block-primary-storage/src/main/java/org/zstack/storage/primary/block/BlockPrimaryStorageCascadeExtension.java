package org.zstack.storage.primary.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/12/20 17:46
 */
public class BlockPrimaryStorageCascadeExtension extends AbstractAsyncCascadeExtension {
    private final static CLogger logger = Utils.getLogger(BlockPrimaryStorageCascadeExtension.class);

    private static final String NAME = BlockPrimaryStorageVO.class.getSimpleName();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected BlockPrimaryStorageFactory blockPrimaryStorageFactory;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, final Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        final List<BlockPrimaryStorageVO> blockPrimaryStorageVOS = blockPrimaryStoragesFromAction(action);
        new While<>(blockPrimaryStorageVOS).each((BlockPrimaryStorageVO bpvo, WhileCompletion whileCompletion) -> {
            BlockPrimaryStorageDeviceBackend bkd = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(bpvo);
            if(bkd == null) {
                whileCompletion.done();
                return;
            }
            String heartbeatLunName = blockPrimaryStorageFactory.generateHeartbeatLunName(bpvo.getUuid());
            BlockScsiLunVO heartbeatLun = Q.New(BlockScsiLunVO.class)
                    .eq(BlockScsiLunVO_.name, heartbeatLunName)
                    .find();
            if (heartbeatLun == null) {
                whileCompletion.done();
                return;
            }
            bkd.deleteLun(heartbeatLun.getId(), new Completion(whileCompletion) {
                @Override
                public void success() {
                    dbf.remove(heartbeatLun);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("fail to delete heartbeat lun, because of: %s, but we have to ignore this to finish delete flow", errorCode.toString()));
                    dbf.remove(heartbeatLun);
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });
    }

    private List<BlockPrimaryStorageVO> blockPrimaryStoragesFromAction(CascadeAction action) {
        if (PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<PrimaryStorageInventory> primaryStorageInventories = action.getParentIssuerContext();
            List<String> psUuids = primaryStorageInventories.stream().map(PrimaryStorageInventory::getUuid).collect(Collectors.toList());
            List<BlockPrimaryStorageVO> blockPrimaryStorageVOS = Q.New(BlockPrimaryStorageVO.class)
                    .in(BlockPrimaryStorageVO_.uuid, psUuids)
                    .list();
            return  blockPrimaryStorageVOS;
        }
        return null;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(PrimaryStorageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
