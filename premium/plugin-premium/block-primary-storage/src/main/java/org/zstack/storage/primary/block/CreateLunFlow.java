package org.zstack.storage.primary.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudOperationError;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.storage.primary.block.BlockPrimaryStorageConstants;
import org.zstack.storage.primary.block.BlockScsiLunVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/4/14 17:00
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateLunFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CreateLunFlow.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    private BlockPrimaryStorageDeviceBackend bkd;
    private BlockScsiLunVO blockScsiLunVO;
    private VolumeProvisioningStrategy volumeProvisioningStrategy;

    @Override
    public void run(FlowTrigger trigger, Map data) {

        bkd = (BlockPrimaryStorageDeviceBackend) data.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend);
        blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
        volumeProvisioningStrategy = (VolumeProvisioningStrategy) data.get(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy);

        if(StringUtils.isEmpty(blockScsiLunVO.getUuid())) {
            blockScsiLunVO.setUuid(Platform.getUuid());
        }

        bkd.createLun(blockScsiLunVO, volumeProvisioningStrategy, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
            @Override
            public void success(BlockScsiLunVO returnValue) {
                logger.debug(String.format("successfully create lun:%s on backend device.", JSONObjectUtil.toJsonString(returnValue)));
                blockScsiLunVO.setWwn(returnValue.getWwn());
                blockScsiLunVO.setId(returnValue.getId());
                blockScsiLunVO.setLunType(returnValue.getLunType());
                logger.debug(String.format("successfully persist vo:%s.", JSONObjectUtil.toJsonString(blockScsiLunVO)));
                data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);

                dbf.persist(blockScsiLunVO);
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
        if (!blockScsiLunVO.getId().equals(0)) {
            bkd.deleteLun(blockScsiLunVO.getId(), new Completion(trigger) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully roll back to delete lun %s", String.valueOf(blockScsiLunVO.getId())));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("failed to roll back to delete lun %s", String.valueOf(blockScsiLunVO.getId())));
                }
            });
            dbf.remove(blockScsiLunVO);
        }
        trigger.rollback();
    }
}