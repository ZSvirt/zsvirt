package org.zstack.storage.primary.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/4/13 18:29
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteLunMapFlow extends NoRollbackFlow{
    private static final CLogger logger = Utils.getLogger(DeleteLunMapFlow.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    private BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO;
    private BlockPrimaryStorageDeviceBackend bkd;
    private BlockScsiLunVO blockScsiLunVO;
    private Boolean checkBeforeDeleteLunMap = false;

    private Boolean isInvalidPSHostRef(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO) {
        if (blockPrimaryStorageHostRefVO == null
                || StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getHostUuid())
                || StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getMetadata())) {

            logger.debug("find invalid ps host ref when delete lun map");
            return true;
        }
        return false;
    }

    @Override
    public void run(FlowTrigger outTrigger, Map outData) {
        blockPrimaryStorageHostRefVO = (BlockPrimaryStorageHostRefVO) outData.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef);
        bkd = (BlockPrimaryStorageDeviceBackend) outData.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend);
        blockScsiLunVO = (BlockScsiLunVO) outData.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
        checkBeforeDeleteLunMap = (Boolean) outData.get(BlockPrimaryStorageConstants.Params.CheckBeforeDeleteLunMap);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete lun map");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                final Boolean[] alreadyUmapped = {false};
                flow(new NoRollbackFlow() {
                    String __name__ = "logout-target-on-kvm-before-lunmap-deleted";
                    @Override
                    public void run(FlowTrigger trigger, Map map) {
                        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                        blockPrimaryStorageKvmCommandDispatcher.logoutTarget(blockPrimaryStorageHostRefVO.getHostUuid(), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.debug(String.format("fail to logout lun:%s on host: %s.",
                                        JSONObjectUtil.toJsonString(blockScsiLunVO), blockPrimaryStorageHostRefVO.getHostUuid()));
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public boolean skip(Map map) {
                        return isInvalidPSHostRef(blockPrimaryStorageHostRefVO);
                    }
                });

                if (checkBeforeDeleteLunMap == null || checkBeforeDeleteLunMap) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "check whether lun has been mapped to host";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            bkd.checkLunHasMappedToHost(blockPrimaryStorageHostRefVO, blockScsiLunVO, new ReturnValueCompletion<Boolean>(trigger) {
                                @Override
                                public void success(Boolean returnValue) {
                                    alreadyUmapped[0] = !returnValue;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public boolean skip(Map data) {
                            return isInvalidPSHostRef(blockPrimaryStorageHostRefVO);
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-lunmap-on-backend-storage-device";
                    @Override
                    public void run(FlowTrigger trigger, Map map) {
                        bkd.deleteLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public boolean skip(Map map) {
                        if (isInvalidPSHostRef(blockPrimaryStorageHostRefVO) || alreadyUmapped[0]) {
                            return true;
                        }
                        return false;
                    }
                });

                done(new FlowDoneHandler(outTrigger) {
                    @Override
                    public void handle(Map data) {
                        outTrigger.next();
                    }
                });

                error(new FlowErrorHandler(outTrigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        outTrigger.fail(errCode);
                    }
                });
            }
        }).start();
    }
}