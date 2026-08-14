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

import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/4/13 18:29
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MapLunToHostFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(MapLunToHostFlow.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    private BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO;
    private BlockPrimaryStorageDeviceBackend bkd;
    private BlockScsiLunVO blockScsiLunVO;

    private Boolean isInvalidPSHostRef(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO) {
        if (blockPrimaryStorageHostRefVO == null
                || StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getHostUuid())
                || StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getMetadata())) {
            return true;
        }
        return false;
    }

    private Boolean isAllDataReadyToMapLunToHost(FlowTrigger trigger) {
        if (bkd == null) {
            trigger.fail(operr("can not execute map lun to host flow, because backend device is null"));
            return false;
        }
        if (blockPrimaryStorageHostRefVO == null) {
            trigger.fail(operr("can not execute map lun to host flow, because ps host ref is null"));
            return false;
        }
        if (StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getMetadata())) {
            trigger.fail(operr("can not execute map lun to host flow, because ps host ref metadata is empty"));
            return false;
        }
        if (blockScsiLunVO == null) {
            trigger.fail(operr("can not execute map lun to host flow, because ps host ref metadata is empty"));
            return false;
        }
        if (blockScsiLunVO.getId() == null || blockScsiLunVO.getId().equals(0)) {
            trigger.fail(operr("can not execute map lun to host flow, invalid lun id"));
            return false;
        }
        if (StringUtils.isEmpty(blockScsiLunVO.getLunType())) {
            trigger.fail(operr("can not execute map lun to host flow, invalid lun lun type"));
            return false;
        }
        return true;
    }

    @Override
    public void run(FlowTrigger outTrigger, Map outData) {
        blockPrimaryStorageHostRefVO = (BlockPrimaryStorageHostRefVO) outData.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef);
        bkd = (BlockPrimaryStorageDeviceBackend) outData.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend);
        blockScsiLunVO = (BlockScsiLunVO) outData.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
        if (!isAllDataReadyToMapLunToHost(outTrigger)) {
            return;
        }

        final Boolean[] alreadyMapped = {false};
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("map lun to host");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check whether lun has been mapped to host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.checkLunHasMappedToHost(blockPrimaryStorageHostRefVO, blockScsiLunVO, new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean returnValue) {
                        alreadyMapped[0] = returnValue;
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
        }).then(new Flow() {
            String __name__ = "create-lun-map";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        blockScsiLunVO.setTarget(returnValue.getTarget());
                        blockScsiLunVO.setLunMapId(returnValue.getLunMapId());
                        blockScsiLunVO.setWwn(returnValue.getWwn());
                        SQL.New(BlockScsiLunVO.class)
                                .eq(BlockScsiLunVO_.volumeUuid, blockScsiLunVO.getVolumeUuid())
                                .set(BlockScsiLunVO_.lunMapId, returnValue.getLunMapId())
                                .set(BlockScsiLunVO_.target, returnValue.getTarget())
                                .update();
                        outData.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
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
                bkd.deleteLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }

            @Override
            public boolean skip(Map data) {
                if (alreadyMapped[0] || isInvalidPSHostRef(blockPrimaryStorageHostRefVO)) {
                    return true;
                }
                return false;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "double check block scsi lun target";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getLunByName(blockScsiLunVO.getName(), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        blockScsiLunVO.setTarget(returnValue.getTarget());
                        outData.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
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
                if (alreadyMapped[0] && !StringUtils.isEmpty(blockScsiLunVO.getTarget())) {
                    return true;
                }
                return false;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "discovery-lun";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.discoverLun(blockPrimaryStorageHostRefVO.getHostUuid(), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(trigger) {
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
            public boolean skip(Map data) {
                return isInvalidPSHostRef(blockPrimaryStorageHostRefVO);
            }
        }).done(new FlowDoneHandler(outTrigger) {
            @Override
            public void handle(Map data) {
                outTrigger.next();
            }
        }).error(new FlowErrorHandler(outTrigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                outTrigger.fail(errCode);
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback rollback, Map outData) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("map lun to host");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        logger.debug(String.format("logout lun:%s on host: %s", blockScsiLunVO.getInstallPath(), blockPrimaryStorageHostRefVO.getHostUuid()));
                        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                        blockPrimaryStorageKvmCommandDispatcher.logoutTarget(blockPrimaryStorageHostRefVO.getHostUuid(), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(trigger) {
                            @Override
                            public void success() {
                                logger.debug(String.format("successfully logout lun:%s on host: %s", blockScsiLunVO.getInstallPath(), blockPrimaryStorageHostRefVO.getHostUuid()));
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
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bkd.deleteLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(rollback) {
                    @Override
                    public void handle(Map data) {
                        rollback.rollback();
                    }
                });

                error(new FlowErrorHandler(rollback) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        rollback.rollback();
                    }
                });
            }
        }).start();
    }
}