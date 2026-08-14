package org.zstack.storage.primary.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/4/14 17:00
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateLunFromTemplateFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(CreateLunFromTemplateFlow.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRegistry;

    private BlockPrimaryStorageDeviceBackend bkd;
    private BlockScsiLunVO blockScsiLunVO;
    private BlockScsiLunVO templateLun;
    private VolumeProvisioningStrategy volumeProvisioningStrategy;

    @Override
    public void run(FlowTrigger outTrigger, Map outData) {

        bkd = (BlockPrimaryStorageDeviceBackend) outData.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend);
        blockScsiLunVO = (BlockScsiLunVO) outData.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
        volumeProvisioningStrategy = (VolumeProvisioningStrategy) outData.get(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy);
        templateLun = (BlockScsiLunVO) outData.get(BlockPrimaryStorageConstants.Params.TemplateLun);
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("create lun from a template lun");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "query image cache on block primary storage";
                    @Override
                    public void run(FlowTrigger trigger, Map map) {
                        logger.debug(String.format("get template lun:%s", JSONObjectUtil.toJsonString(templateLun)));
                        bkd.getImageCacheLun(templateLun.getName(), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                            @Override
                            public void success(BlockScsiLunVO returnValue) {
                                if (returnValue == null) {
                                    trigger.fail(operr(String.format("failed to find template lun by name:%s", templateLun.getName())));
                                    return;
                                }
                                templateLun.setId(returnValue.getId());
                                templateLun.setWwn(returnValue.getWwn());
                                templateLun.setSize(returnValue.getSize());
                                templateLun.setLunType(returnValue.getLunType());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "create-volume-from-cache";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bkd.createLunFromTemplate(templateLun, blockScsiLunVO.getName(), volumeProvisioningStrategy, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                            @Override
                            public void success(BlockScsiLunVO returnValue) {
                                blockScsiLunVO.setWwn(returnValue.getWwn());
                                blockScsiLunVO.setId(returnValue.getId());
                                blockScsiLunVO.setLunType(returnValue.getLunType());
                                blockScsiLunVO.setSize(returnValue.getSize());
                                if(StringUtils.isEmpty(blockScsiLunVO.getUuid())) {
                                    blockScsiLunVO.setUuid(Platform.getUuid());
                                }
                                outData.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
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
                        bkd.deleteLun(blockScsiLunVO.getId(), new Completion(trigger) {
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