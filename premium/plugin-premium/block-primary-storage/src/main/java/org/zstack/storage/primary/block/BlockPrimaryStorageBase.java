package org.zstack.storage.primary.block;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;
import org.zstack.storage.backup.imagestore.CommitVolumeAsImageFlowChain;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.backup.imagestore.GetImageChainInfoMsg;
import org.zstack.storage.backup.imagestore.GetImageChainInfoReply;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.block.message.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.buildErrIfCanceled;
import static org.zstack.utils.CollectionDSL.list;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/2 14:46
 */
public class BlockPrimaryStorageBase extends PrimaryStorageBase {
    private final static CLogger logger = Utils.getLogger(BlockPrimaryStorageBase.class);

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private BlockPrimaryStorageImageCacheCleaner imageCacheCleaner;

    @Autowired
    protected BlockPrimaryStorageFactory blockPrimaryStorageFactory;

    public BlockPrimaryStorageBase() {
    }

    public BlockPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
        return;
    }

    class ImageCache {
        ImageSpec image;
        String destHostUuid;
        BlockScsiLunVO destLun;
        String requiredPrimaryStorageUuid;

        private void doDownload(final ReturnValueCompletion<ImageCacheVO> completion) {

            BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(requiredPrimaryStorageUuid);
            if (blockPrimaryStorageDeviceBackend == null) {
                completion.fail(operr("primaryStorageUuid is mandatory when download image cache"));
                return;
            }
            ImageCacheVO cache = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                    .eq(ImageCacheVO_.imageUuid, image.getInventory().getUuid())
                    .find();
            final Boolean[] cacheReady = {false};
            final Boolean[] cacheCheckBitsFailed = {false};

            String lunName = generateImageCacheLunName(image.getInventory().getUuid());
            if (cache != null) {
                logger.debug(String.format("find image cache record[name:%s, actual size:%s], check its bits first", lunName, String.valueOf(image.getInventory().getActualSize())));
                blockPrimaryStorageDeviceBackend.checkBits(lunName, image.getInventory().getActualSize(), new ReturnValueCompletion<Boolean>(completion) {
                    @Override
                    public void success(Boolean imageIsReady) {
                        if(imageIsReady) {
                            logger.debug(String.format("found image[uuid: %s, name: %s] in the block primary storage[uuid:%s]",
                                    image.getInventory().getUuid(), lunName, self.getUuid()));
                            cacheReady[0] = true;
                            completion.success(cache);
                            return;
                        }
                        dbf.remove(cache);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        cacheCheckBitsFailed[0] = true;
                        completion.fail(errorCode);
                    }
                });
            }

            if (cacheReady[0] == true) {
                return;
            }

            if (cacheCheckBitsFailed[0] == true) {
                logger.debug(String.format("fail to check image:%s bits", lunName));
                return;
            }

            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("download-image-%s-to-block-storage-%s", image.getInventory().getUuid(), self.getUuid());
                }

                private void download(final SyncTaskChain chain) {
                    final FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();

                    String needPullImageChainToken = "needPullImageChain";
                    String downloadPathToken = "downloadPath";
                    String imageCacheInstallPathToken = "imageCacheInstallPath";
                    String temporaryImageCacheLunToken = "temporaryImageCacheLun";
                    String voToken = "vo";
                    String mountPath = BlockPrimaryStorageGlobalConfig.BLOCK_PRIMARY_STORAGE_TMP_FOLDER_FOR_IMAGE_CACHE.value() + image.getInventory().getUuid();
                    BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(destHostUuid);

                    Map data = new HashMap();
                    data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);
                    data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, blockPrimaryStorageDeviceBackend);
                    data.put(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy, VolumeProvisioningStrategy.ThinProvisioning);
                    data.put(needPullImageChainToken, false);

                    fchain.setData(data);
                    fchain.setName(String.format("prepare-image-cache-on-block-ps-%s", requiredPrimaryStorageUuid));

                    fchain.then(new NoRollbackFlow() {
                        String __name__ = "check image chain length";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            GetImageChainInfoMsg msg = new GetImageChainInfoMsg();
                            msg.setBackupStorageUuid(image.getSelectedBackupStorage().getBackupStorageUuid());
                            msg.setInstallPath(image.getInventory().getBackupStorageRefs().get(0).getInstallPath());
                            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    }

                                    GetImageChainInfoReply r = reply.castReply();
                                    if (r.getChain().size() > 1) {
                                        data.put(needPullImageChainToken, true);
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    }).then(new NoRollbackFlow() {
                        String __name__ = "prepare temporary image cache lun info";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO tempLunForImageCacheDownloading = new BlockScsiLunVO();
                            tempLunForImageCacheDownloading.setName("tmp-" + generateLunName(image.getInventory().getUuid()));
                            tempLunForImageCacheDownloading.setSize(getLunNeededSize(image.getInventory()) + Long.valueOf(BlockPrimaryStorageGlobalConfig.BLOCK_PRIMARY_STORAGE_EXTRA_LUN_SIZE_FOR_IMAGE_CACHE.value()));
                            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, tempLunForImageCacheDownloading);
                            trigger.next();
                        }

                        @Override
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });
                    fchain.then(new CreateLunFlow(){
                        @Override
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });
                    fchain.then(new MapLunToHostFlow(){
                        @Override
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });
                    fchain.then(new Flow() {
                        String __name__ = "mount temporary image cache lun";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                            BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                            blockPrimaryStorageKvmCommandDispatcher.mountTempImageCacheLun(blockScsiLunVO.getInstallPath(), mountPath, destHostUuid, new Completion(trigger) {
                                @Override
                                public void success() {
                                    data.put(downloadPathToken, mountPath + "/" + image.getInventory().getUuid() + ".qcow2");
                                    data.put(temporaryImageCacheLunToken, blockScsiLunVO);
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
                            BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                            blockPrimaryStorageKvmCommandDispatcher.umountPath(mountPath, destHostUuid, new Completion(trigger) {
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
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });

                    //Create image cache lun, if no need to pull image chain, then skip below flows
                    fchain.then(new NoRollbackFlow() {
                        String __name__ = "set image cache lun info";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiTemplateLunVO = new BlockScsiLunVO();
                            if (destLun == null) {
                                blockScsiTemplateLunVO.setName(generateImageCacheLunName(image.getInventory().getUuid()));
                                blockScsiTemplateLunVO.setSize(getLunNeededSize(image.getInventory()));
                            } else {
                                blockScsiTemplateLunVO = destLun;
                            }
                            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiTemplateLunVO);
                            trigger.next();
                        }
                    }).then(new CreateLunFlow() {
                        // if specific lun to download, no need to create new lun
                        @Override
                        public boolean skip(Map map) {
                            return destLun != null;
                        }
                    }).then(new MapLunToHostFlow());

                    fchain.then(new Flow() {
                        String __name__ = "download-from-backup-storage";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                            String downloadPath = blockScsiLunVO.getInstallPath();
                            if (data.get(needPullImageChainToken).equals(true)) {
                                downloadPath = data.get(downloadPathToken).toString();
                            }
                            data.put(imageCacheInstallPathToken, blockScsiLunVO.getInstallPath());
                            BackupStorageBlockKvmDownloader downloader = getBackupStorageKvmDownloader(image.getSelectedBackupStorage().getBackupStorageUuid());
                            downloader.downloadBits(image.getSelectedBackupStorage().getInstallPath(), downloadPath, destHostUuid, new Completion(trigger) {
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
                        public void rollback(FlowRollback trigger, Map data) {
                            trigger.rollback();
                        }
                    });

                    fchain.then(new NoRollbackFlow() {
                        String __name__ = "convert image to block device";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                            BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                            blockPrimaryStorageKvmCommandDispatcher.convertImageToLun(mountPath + "/" +  image.getInventory().getUuid() + ".qcow2", blockScsiLunVO.getInstallPath(), destHostUuid, new Completion(trigger) {
                                @Override
                                public void success() {
                                    data.put(imageCacheInstallPathToken, blockScsiLunVO.getInstallPath());
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
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });

                    fchain.then(new NoRollbackFlow() {
                        String __name__ = "umount temporary image cache lun";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                            blockPrimaryStorageKvmCommandDispatcher.umountPath(mountPath, destHostUuid, new Completion(trigger) {
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
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    });

                    // Delete image cache lun map
                    fchain.then(new DeleteLunMapFlow());

                    // Delete temporary image cache lun
                    fchain.then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(temporaryImageCacheLunToken);
                            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
                            trigger.next();
                        }

                        @Override
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    }).then(new DeleteLunMapFlow() {
                        @Override
                        public boolean skip(Map map) {
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    }).then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(temporaryImageCacheLunToken);
                            blockPrimaryStorageDeviceBackend.deleteLun(blockScsiLunVO.getId(), new Completion(trigger) {
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
                            Boolean needPullImageChain = (Boolean) map.get(needPullImageChainToken);
                            return needPullImageChain.equals(false);
                        }
                    }).then(new NoRollbackFlow() {
                        String __name__ = "save-db";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ImageCacheVO cvo = new ImageCacheVO();
                            cvo.setMd5sum("not calculated");
                            cvo.setInstallUrl((String) data.get(imageCacheInstallPathToken));
                            cvo.setImageUuid(image.getInventory().getUuid());
                            cvo.setPrimaryStorageUuid(self.getUuid());
                            cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getInventory().getMediaType()));
                            cvo.setState(ImageCacheState.ready);
                            cvo.setSize(image.getInventory().getActualSize());
                            cvo = dbf.persistAndRefresh(cvo);
                            data.put(voToken, cvo);
                            trigger.next();
                        }
                    }).then(new NoRollbackFlow() {
                        String __name__ = "invoke-after-create-image-cache-extensions";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ImageCacheVO vo = (ImageCacheVO) data.get(voToken);
                            ImageCacheInventory inventory = ImageCacheInventory.valueOf(vo);

                            new While<>(pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class)).each((ext, whileCompletion) -> {
                                ext.saveEncryptAfterCreateImageCache(null, inventory, new Completion(whileCompletion) {
                                    @Override
                                    public void success() {
                                        whileCompletion.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        whileCompletion.addError(errorCode);
                                        whileCompletion.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (!errorCodeList.getCauses().isEmpty()) {
                                        String details = multiErr(errorCodeList.getCauses()).getReadableDetails();
                                        logger.warn(String.format(
                                                "failed to invoke after create image cache extensions (but still continue): %s",
                                                details));
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });

                    fchain.done(new FlowDoneHandler(completion, chain) {
                        @Override
                        public void handle(Map map) {
                            completion.success((ImageCacheVO) map.get(voToken));
                            chain.next();
                        }
                    });
                    fchain.error(new FlowErrorHandler(completion, chain) {
                        @Override
                        public void handle(ErrorCode errorCode, Map map) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                    fchain.start();
                }

                @Override
                public void run(final SyncTaskChain syncTaskChain) {
                    ImageCacheVO cache = Q.New(ImageCacheVO.class)
                            .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                            .eq(ImageCacheVO_.imageUuid, image.getInventory().getUuid())
                            .find();
                    final Boolean[] cacheReady = {false};
                    final Boolean[] cacheCheckBitsFailed = {false};

                    if (cache != null) {
                        String lunName = generateImageCacheLunName(image.getInventory().getUuid());
                        logger.debug(String.format("find image cache record[name:%s, actual size:%s], check its bits first", lunName, String.valueOf(image.getInventory().getActualSize())));
                        blockPrimaryStorageDeviceBackend.checkBits(lunName, image.getInventory().getActualSize(), new ReturnValueCompletion<Boolean>(completion) {
                            @Override
                            public void success(Boolean imageIsReady) {
                                if(imageIsReady) {
                                    logger.debug(String.format("found image[uuid: %s, name: %s] in the block primary storage[uuid:%s]",
                                            image.getInventory().getUuid(), lunName, self.getUuid()));
                                    cacheReady[0] = true;
                                    completion.success(cache);
                                    return;
                                }

                                IncreasePrimaryStorageCapacityMsg rmsg = new IncreasePrimaryStorageCapacityMsg();
                                rmsg.setDiskSize(cache.getSize());
                                rmsg.setPrimaryStorageUuid(cache.getPrimaryStorageUuid());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, cache.getPrimaryStorageUuid());
                                bus.send(rmsg);
                                dbf.remove(cache);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                cacheCheckBitsFailed[0] = true;
                                completion.fail(errorCode);
                            }
                        });
                    }

                    if (cacheReady[0] == true) {
                        syncTaskChain.next();
                        return;
                    }
                    if (cacheCheckBitsFailed[0] == true) {
                        logger.debug(String.format("fail to check image:%s bits", lunName));
                        syncTaskChain.next();
                        return;
                    }
                    download(syncTaskChain);
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private BackupStorageBlockKvmDownloader getBackupStorageKvmDownloader(String backupStorageUuid) {
        String bsType = Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, backupStorageUuid).select(BackupStorageVO_.type).findValue();

        for (BackupStorageBlockKvmFactory f : pluginRgty.getExtensionList(BackupStorageBlockKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createDownloader(getSelfInventory(), backupStorageUuid);
            }
        }
        throw new OperationFailureException(operr("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }

    private BackupStorageBlockKvmUploader getBackupStorageKvmUploader(String backupStorageUuid) {
        String bsType = Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, backupStorageUuid).select(BackupStorageVO_.type).findValue();

        for (BackupStorageBlockKvmFactory f : pluginRgty.getExtensionList(BackupStorageBlockKvmFactory.class)) {
            if (bsType.equals(f.getBackupStorageType())) {
                return f.createUploader(getSelfInventory(), backupStorageUuid);
            }
        }
        throw new OperationFailureException(operr("cannot find any BackupStorageKvmFactory for the type[%s]", bsType));
    }

    @Override
    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();

        ImageCache imageCache = new ImageCache();
        imageCache.image = msg.getTemplateSpec();
        imageCache.destHostUuid = msg.getHostUuid();
        imageCache.requiredPrimaryStorageUuid = msg.getPrimaryStorageUuid();

        imageCache.doDownload(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO cache) {
                reply.setImageCache(ImageCacheInventory.valueOf(cache));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void doCreateEmptyVolume(BlockScsiLunVO blockScsiLunVO, String primaryStorageUuid,  ReturnValueCompletion<BlockScsiLunVO> completion) {
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(primaryStorageUuid);
        VolumeProvisioningStrategy volumeProvisioningStrategy = blockPrimaryStorageFactory.getVolumeProvisioningStrategy(primaryStorageUuid);

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, blockPrimaryStorageDeviceBackend);
        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
        data.put(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy, volumeProvisioningStrategy);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName("create empty volume");
        chain.then(new CreateLunFlow());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                BlockScsiLunVO lun = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                completion.success(lun);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void createEmptyVolume(InstantiateVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();
        volume.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

        final BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        blockScsiLunVO.setSize(getLunNeededSize(msg.getVolume().getActualSize(), msg.getVolume().getSize()));
        blockScsiLunVO.setName(generateLunName(msg.getVolume().getUuid()));
        blockScsiLunVO.setUuid(Platform.getUuid());
        blockScsiLunVO.setVolumeUuid(msg.getVolume().getUuid());

        doCreateEmptyVolume(blockScsiLunVO, msg.getPrimaryStorageUuid(), new ReturnValueCompletion<BlockScsiLunVO>(msg) {
            @Override
            public void success(BlockScsiLunVO returnValue) {
                volume.setInstallPath(returnValue.getInstallPath());
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final InstantiateVolumeOnPrimaryStorageReply greply = new InstantiateVolumeOnPrimaryStorageReply();

        if (msg.getDestHost() == null) {
            greply.setError(operr("the block primary storage[uuid:%s, name:%s] can not find any " +
                    "available host in attached clusters for instantiating the volume", self.getUuid(), self.getName()));
            bus.reply(msg, greply);
            return;
        }

        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        blockScsiLunVO.setName(generateLunName(msg.getVolume().getUuid()));
        blockScsiLunVO.setUuid(Platform.getUuid());
        blockScsiLunVO.setVolumeUuid(msg.getVolume().getUuid());

        final BlockScsiLunVO blockScsiTemplateLunVO = new BlockScsiLunVO();
        blockScsiTemplateLunVO.setName(generateImageCacheLunName(msg.getTemplateSpec().getInventory().getUuid()));

        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());
        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(msg.getDestHost().getUuid(), msg.getPrimaryStorageUuid());
        VolumeProvisioningStrategy volumeProvisioningStrategy = blockPrimaryStorageFactory.getVolumeProvisioningStrategy(msg.getPrimaryStorageUuid());

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, blockPrimaryStorageDeviceBackend);
        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
        data.put(BlockPrimaryStorageConstants.Params.TemplateLun, blockScsiTemplateLunVO);
        data.put(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy, volumeProvisioningStrategy);
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.setData(data);
        chain.then(new NoRollbackFlow() {
            String __name__ = "download volume template to primary storage";
            @Override
            public void run(FlowTrigger trigger, Map data) {

                if (blockPrimaryStorageHostRefVO == null) {
                    trigger.fail(operr(String.format("Fail to get host initiator ref, please reconnect this host:%s", msg.getDestHost().getUuid())));
                    return;
                }
                DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
                dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                dmsg.setHostUuid(msg.getDestHost().getUuid());
                dmsg.setTemplateSpec(msg.getTemplateSpec());

                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        });
        chain.then(new CreateLunFromTemplateFlow());
        chain.then(new NoRollbackFlow() {
            String __name__ = "create init snapshot for: " + msg.getVolume().getUuid();
            @Override
            public void run(FlowTrigger trigger, Map map) {
                BlockScsiLunVO newLun = (BlockScsiLunVO) map.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                blockPrimaryStorageDeviceBackend.takeSnapshot(newLun, new ReturnValueCompletion<TakeSnapshotOnHypervisorReply>(trigger) {
                    @Override
                    public void success(TakeSnapshotOnHypervisorReply takeSnapshotOnHypervisorReply) {
                        newLun.setLunInitSnapshotID(Integer.valueOf(takeSnapshotOnHypervisorReply.getSnapshotInstallPath()));
                        SQL.New(BlockScsiLunVO.class)
                                .eq(BlockScsiLunVO_.volumeUuid, blockScsiLunVO.getVolumeUuid())
                                .set(BlockScsiLunVO_.lunInitSnapshotID, newLun.getLunInitSnapshotID())
                                .update();
                        map.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, newLun);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.then(new MapLunToHostFlow());
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                BlockScsiLunVO newLun = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(newLun.getInstallPath());
                vol.setFormat(msg.getVolume().getFormat());
                greply.setVolume(vol);
                bus.reply(msg, greply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                greply.setError(errCode);
                bus.reply(msg, greply);
            }
        }).start();
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {

        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolume().getUuid())
                .find();

        DeleteVolumeLunOnPrimaryStorageMsg deleteVolumeLunOnPrimaryStorageMsg = new DeleteVolumeLunOnPrimaryStorageMsg();
        deleteVolumeLunOnPrimaryStorageMsg.setLunName(blockScsiLunVO.getName());
        deleteVolumeLunOnPrimaryStorageMsg.setLunId(blockScsiLunVO.getId());
        deleteVolumeLunOnPrimaryStorageMsg.setBitsUuid(msg.getVolume().getUuid());
        deleteVolumeLunOnPrimaryStorageMsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(deleteVolumeLunOnPrimaryStorageMsg, BlockPrimaryStorageConstants.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(deleteVolumeLunOnPrimaryStorageMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                DeleteVolumeOnPrimaryStorageReply reply1 = new DeleteVolumeOnPrimaryStorageReply();
                if (!reply.isSuccess()) {
                    reply1.setError(reply.getError());
                    bus.reply(msg, reply1);
                } else {
                    bus.reply(msg, reply1);
                }
            }
        });
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        final CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();

        List<String> hostUuidList = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.uuid, msg.getVolumeInventory().getAttachedVmUuids())
                .listValues();
        if (hostUuidList.size() == 0) {
            reply.setError(operr(String.format("volume: %s is not attached, fail to create image cache for volume", msg.getVolumeInventory().getUuid())));
            bus.reply(msg, reply);
            return;
        }
        String imageUuid = msg.getVolumeInventory().getRootImageUuid();
        String volumeUuid = msg.getVolumeInventory().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-snapshot-and-image-from-volume-%s", volumeUuid));
        chain.enableProgressReport();
        chain.preCheck(data -> buildErrIfCanceled());
        chain.then(new ShareFlow() {
            VolumeSnapshotInventory snapshot;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(volumeUuid);

                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName("Snapshot-" + volumeUuid);
                        cmsg.setDescription("Take snapshot for " + volumeUuid);
                        cmsg.setVolumeUuid(volumeUuid);
                        cmsg.setAccountUuid(volumeAccountUuid);

                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply createVolumeSnapshotReply = (CreateVolumeSnapshotReply)r;
                                snapshot = createVolumeSnapshotReply.getInventory();
                                trigger.next();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-cache-from-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ImageCache cache = new ImageCache();
                        cache.image = new ImageSpec();
                        cache.image.setInventory(msg.getImageInventory());
                        cache.destHostUuid = hostUuidList.get(0);
                        cache.requiredPrimaryStorageUuid = msg.getPrimaryStorageUuid();
                        cache.doDownload(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                            @Override
                            public void success(ImageCacheVO cache) {
                                reply.setActualSize(cache.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%s]", imageUuid, volumeUuid));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to create template from volume[uuid:%s], because %s", volumeUuid, errCode));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        VolumeVO volumeVO = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeSnapshot().getVolumeUuid())
                .find();

        List<String> hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.uuid, volumeVO.getAttachedVmUuids())
                .listValues();

        if (hostUuid.size() == 0) {
            reply.setError(operr(String.format("cant not find dest host for volume:%s", msg.getVolumeSnapshot().getVolumeUuid())));
            bus.reply(msg, reply);
            return;
        }

        VolumeSnapshotVO volumeSnapshotVO = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getVolumeSnapshot().getUuid())
                .find();

        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        String imageLunName = generateImageCacheLunName(msg.getImageInventory().getUuid());
        blockScsiLunVO.setName(imageLunName);
        blockScsiLunVO.setSize(msg.getVolumeSnapshot().getSize());
        blockScsiLunVO.setId(Integer.valueOf(volumeSnapshotVO.getPrimaryStorageInstallPath()));

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());
        bkd.createLunFromSnapshot(blockScsiLunVO, imageLunName, blockPrimaryStorageFactory.getVolumeProvisioningStrategy(msg.getPrimaryStorageUuid()), new ReturnValueCompletion<BlockScsiLunVO>(msg) {
            @Override
            public void success(BlockScsiLunVO blockScsiLunVO) {
                ImageCacheVO cvo = Q.New(ImageCacheVO.class)
                         .eq(ImageCacheVO_.imageUuid, msg.getImageInventory().getUuid())
                         .find();

                if (cvo != null) {
                    bus.reply(msg, reply);
                    return;
                }

                cvo = new ImageCacheVO();
                cvo.setMd5sum("not calculated");
                cvo.setInstallUrl(blockScsiLunVO.getInstallPath());
                cvo.setImageUuid(msg.getImageInventory().getUuid());
                cvo.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(msg.getImageInventory().getMediaType()));
                cvo.setState(ImageCacheState.ready);
                cvo.setSize(msg.getImageInventory().getActualSize());
                dbf.persistAndRefresh(cvo);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();

        String volumeUuid = msg.getVolumeInventory().getUuid();
        String imageUuid = msg.getImageInventory().getUuid();
        String newVolumeName = generateImageCacheLunName(imageUuid);

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());

        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, volumeUuid)
                .find();


        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
        data.put(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy, VolumeProvisioningStrategy.ThinProvisioning);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName(String.format("create-image-from-volume-%s", volumeUuid));
        chain.then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLunFromTemplate(blockScsiLunVO, newVolumeName, blockPrimaryStorageFactory.getVolumeProvisioningStrategy(msg.getPrimaryStorageUuid()), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        BlockScsiLunVO newBlockScsiLunVO = new BlockScsiLunVO();
                        newBlockScsiLunVO.setSize(returnValue.getSize());
                        newBlockScsiLunVO.setLunType(returnValue.getLunType());
                        newBlockScsiLunVO.setId(returnValue.getId());
                        newBlockScsiLunVO.setWwn(returnValue.getWwn());
                        newBlockScsiLunVO.setName(newVolumeName);
                        newBlockScsiLunVO.setUuid(Platform.getUuid());
                        logger.debug(String.format("successfully clone volume:%s", volumeUuid));

                        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, newBlockScsiLunVO);
                        dbf.persist(newBlockScsiLunVO);
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
                BlockScsiLunVO newBlockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                if (newBlockScsiLunVO.getId() == null || newBlockScsiLunVO.getId().equals(0)) {
                    trigger.rollback();
                    return;
                }
                bkd.deleteLun(newBlockScsiLunVO.getId(), new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            String __name__ = "find-host-to-map-image-cache-lun";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> ref = Q.New(PrimaryStorageClusterRefVO.class)
                        .select(PrimaryStorageClusterRefVO_.clusterUuid)
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                        .listValues();

                List<HostVO> hostVOList = Q.New(HostVO.class)
                        .in(HostVO_.clusterUuid, ref)
                        .list();
                if (hostVOList == null || hostVOList.size() == 0) {
                    trigger.fail(operr("fail to find a host to map for volume %s", msg.getVolumeInventory().getUuid()));
                    return;
                }

                Collections.shuffle(hostVOList);
                String hostUuid = hostVOList.get(0).getUuid();

                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(hostUuid, msg.getPrimaryStorageUuid());
                data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);
                trigger.next();
            }
        }).then(new MapLunToHostFlow());
        chain.then(new NoRollbackFlow() {
            String __name__ = "upload-volume-to-bs";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BackupStorageAskInstallPathMsg bmsg = new BackupStorageAskInstallPathMsg();
                bmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                bmsg.setImageMediaType(msg.getImageInventory().getMediaType());
                bmsg.setImageUuid(msg.getImageInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(bmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                MessageReply br = bus.call(bmsg);
                if (!br.isSuccess()) {
                    trigger.fail(br.getError());
                    return;
                }

                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = (BlockPrimaryStorageHostRefVO) data.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef);

                BackupStorageBlockKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                uploader.uploadBits(msg.getImageInventory().getUuid(), msg.getBackupStorageUuid(), blockScsiLunVO.getInstallPath(), blockPrimaryStorageHostRefVO.getHostUuid(), new ReturnValueCompletion<String>(trigger) {
                    @Override
                    public void success(String s) {
                        reply.setTemplateBackupStorageInstallPath(s);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new DeleteLunMapFlow());
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%s]", imageUuid, volumeUuid));
                reply.setFormat(msg.getImageInventory().getFormat());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.warn(String.format("failed to create template from volume[uuid:%s], because %s", volumeUuid, errCode));
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();

        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolumeUuid())
                .find();

        //If install path can not be found in db, something bad is happened, need return as failure.
        if (blockScsiLunVO == null || StringUtils.isEmpty(blockScsiLunVO.getInstallPath()) ) {
            reply.setError(operr("fail to find install path for downloading volume: %s, please prepare it before downloading", msg.getVolumeUuid()));
            bus.reply(msg, reply);
            return;
        }

        String hostUuid = msg.getHostUuid();
        if (StringUtils.isEmpty(hostUuid)) {
            List<String> ref = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                    .listValues();

            List<HostVO> hostVOList = Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, ref)
                    .list();
            if (hostVOList == null || hostVOList.size() == 0) {
                reply.setError(operr("fail to find a host to download volume %s", msg.getVolumeUuid()));
                bus.reply(msg, reply);
                return;
            }
            Collections.shuffle(hostVOList);
            hostUuid = hostVOList.get(0).getUuid();
        }

        ImageSpec imageSpec = new ImageSpec();
        imageSpec.setInventory(msg.getImage());
        imageSpec.setSelectedBackupStorage(msg.getBackupStorageRef());

        ImageCache imageCache = new ImageCache();
        imageCache.image = imageSpec;
        imageCache.destHostUuid = hostUuid;
        imageCache.requiredPrimaryStorageUuid = msg.getPrimaryStorageUuid();
        imageCache.destLun = blockScsiLunVO;
        imageCache.doDownload(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO returnValue) {
                dbf.remove(returnValue);
                reply.setInstallPath(blockScsiLunVO.getInstallPath());
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {

        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        blockScsiLunVO.setName(generateLunName(msg.getVolumeUuid()));
        blockScsiLunVO.setSize(getLunNeededSize(msg.getImage()));
        blockScsiLunVO.setVolumeUuid(msg.getVolumeUuid());
        blockScsiLunVO.setUuid(Platform.getUuid());
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());
        VolumeProvisioningStrategy volumeProvisioningStrategy = blockPrimaryStorageFactory.getVolumeProvisioningStrategy(msg.getPrimaryStorageUuid());

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, blockPrimaryStorageDeviceBackend);
        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
        data.put(BlockPrimaryStorageConstants.Params.VolumeProvisioningStrategy, volumeProvisioningStrategy);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName("get install path for data volume download");
        chain.then(new CreateLunFlow());
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                BlockScsiLunVO lun = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                reply.setInstallPath(lun.getInstallPath());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    @Override
    protected void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {

        final BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        blockScsiLunVO.setName(generateImageCacheLunName(msg.getIsoSpec().getInventory().getUuid()));
        blockScsiLunVO.setVolumeUuid(msg.getIsoSpec().getInventory().getUuid());
        blockScsiLunVO.setSize(getLunNeededSize(msg.getIsoSpec().getInventory()));
        blockScsiLunVO.setUuid(Platform.getUuid());

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());

        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(msg.getDestHostUuid(), msg.getPrimaryStorageUuid());

        List<String> hostUuidList = new ArrayList<>();

        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();

        final ImageCache imageCache = new ImageCache();
        imageCache.image = msg.getIsoSpec();
        imageCache.destHostUuid = msg.getDestHostUuid();
        imageCache.requiredPrimaryStorageUuid = msg.getPrimaryStorageUuid();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-iso-volume-%s", msg.getIsoSpec().getInventory().getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (blockPrimaryStorageHostRefVO == null) {
                    trigger.fail(operr("Fail to get host initiator ref, please reconnect this host:%s", msg.getDestHostUuid()));
                    return;
                }
                hostUuidList.add(msg.getDestHostUuid());
                logger.debug(String.format("create iso lun:%s", JSONObjectUtil.toJsonString(blockScsiLunVO)));

                imageCache.doDownload(new ReturnValueCompletion<ImageCacheVO>(msg) {
                    @Override
                    public void success(ImageCacheVO returnValue) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                bkd.getImageCacheLun(blockScsiLunVO.getName(), new ReturnValueCompletion<BlockScsiLunVO>(flowTrigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        blockScsiLunVO.setId(returnValue.getId());
                        blockScsiLunVO.setWwn(returnValue.getWwn());
                        blockScsiLunVO.setSize(returnValue.getSize());
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "map iso lun to host";
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                blockPrimaryStorageFactory.lunMapOperationInQueue()
                        .name(msg.getIsoSpec().getInventory().getUuid())
                        .asyncBackup(msg)
                        .run(chain -> bkd.createLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new ReturnValueCompletion<BlockScsiLunVO>(chain, flowTrigger) {
                            @Override
                            public void success(BlockScsiLunVO returnValue) {
                                blockScsiLunVO.setTarget(returnValue.getTarget());
                                blockScsiLunVO.setLunMapId(returnValue.getLunMapId());
                                flowTrigger.next();
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                flowTrigger.fail(errorCode);
                                chain.next();
                            }
                        })
                        );
            }

            @Override
            public void rollback(FlowRollback flowRollback, Map map) {
                bkd.deleteLunMap(blockScsiLunVO, blockPrimaryStorageHostRefVO, new Completion(flowRollback) {
                    @Override
                    public void success() {
                        flowRollback.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowRollback.rollback();
                    }
                });

            }
        }).then(new Flow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.discoverLun(msg.getDestHostUuid(), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });

            }

            @Override
            public void rollback(FlowRollback flowRollback, Map map) {
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.logoutTarget(msg.getDestHostUuid(), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(flowRollback) {
                    @Override
                    public void success() {
                        logger.debug(String.format("successfully logout lun:%s on host: %s", JSONObjectUtil.toJsonString(blockScsiLunVO), msg.getDestHostUuid()));
                        flowRollback.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowRollback.rollback();
                    }
                });

            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                reply.setInstallPath(blockScsiLunVO.getInstallPath());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability cap = new VolumeSnapshotCapability();

        String volumeType = msg.getVolume().getType();
        if (VolumeType.Data.toString().equals(volumeType) || VolumeType.Root.toString().equals(volumeType)) {
            cap.setSupport(true);
            cap.setArrangementType(VolumeSnapshotCapability.VolumeSnapshotArrangementType.INDIVIDUAL);
        } else if (VolumeType.Memory.toString().equals(volumeType)) {
            cap.setSupport(false);
        } else {
            throw new CloudRuntimeException(String.format("unknown volume type %s", volumeType));
        }

        reply.setCapability(cap);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        String lunName = Q.New(BlockScsiLunVO.class)
                .select(BlockScsiLunVO_.name)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolumeUuid())
                .findValue();
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());

        bkd.getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(reply) {
            @Override
            public void success(BlockScsiLunVO returnValue) {
                reply.setActualSize(returnValue.getUsedSize());
                reply.setSize(returnValue.getSize());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
            }
        });

        bus.reply(msg, reply);
    }

    @Override
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        String lunName = Q.New(BlockScsiLunVO.class)
                .select(BlockScsiLunVO_.name)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolumeUuid())
                .findValue();
        EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        bkd.getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(reply) {
            @Override
            public void success(BlockScsiLunVO returnValue) {
                reply.setActualSize(returnValue.getUsedSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
        bus.reply(msg, reply);
        logger.warn("Not supported at current edition");
    }

    @Override
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        // Block storage volume is independent lun, no need to flatten.
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());

        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        bkd.deleteSnapshot(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolume().getUuid())
                .find();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("revert snapshot on block storage");
        chain.then(new NoRollbackFlow() {
            String __name__ = "send revert snapshot cmd to storage";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.revertSnapshot(blockScsiLunVO, msg.getSnapshot(), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        reply.setSize(returnValue.getSize());
                        reply.setNewVolumeInstallPath(msg.getVolume().getInstallPath());
                        SQL.New(BlockScsiLunVO.class)
                            .eq(BlockScsiLunVO_.volumeUuid, msg.getVolume().getUuid())
                            .set(BlockScsiLunVO_.size, returnValue.getSize())
                            .set(BlockScsiLunVO_.usedSize, returnValue.getUsedSize())
                            .update();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        trigger.fail(errorCode);
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            String __name__ = "rescan lun after revert snapshot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> vmUuidList = msg.getVolume().getAttachedVmUuids();
                new While<>(vmUuidList).each((String vmUuid, WhileCompletion whileCompletion) -> {
                    VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .find();
                    String hostUuid = vmInstanceVO.getHostUuid() != null ? vmInstanceVO.getHostUuid() : vmInstanceVO.getLastHostUuid();
                    if (hostUuid == null) {
                        whileCompletion.done();
                        return;
                    }
                    BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                    blockPrimaryStorageKvmCommandDispatcher.rescanLun(hostUuid, msg.getVolume().getInstallPath(), new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    protected void handle(ResizeVolumeOnPrimaryStorageMsg msg) {
        ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());

        final VolumeInventory volume = msg.getVolume();
        volume.setSize(msg.getSize());
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolume().getUuid())
                .find();

        blockScsiLunVO.setSize(msg.getSize());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("resize volume on block primary storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                bkd.resizeLun(blockScsiLunVO, msg.getSize(), new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                String vmInstanceUuid = msg.getVolume().getVmInstanceUuid();
                if (vmInstanceUuid == null || vmInstanceUuid.isEmpty()) {
                    flowTrigger.next();
                    return;
                }

                Tuple hostInfo = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                        .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                        .findTuple();
                String hostUuid = hostInfo.get(0, String.class);
                String lastHost = hostInfo.get(1, String.class);
                if (hostUuid == null || hostUuid.equals("")) {
                    hostUuid = lastHost;
                }
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.resizeVolume(hostUuid, blockScsiLunVO, new ReturnValueCompletion<BlockPrimaryStorageKvmCommandDispatcher.ResizeVolumeRsp>(flowTrigger) {
                    @Override
                    public void success(BlockPrimaryStorageKvmCommandDispatcher.ResizeVolumeRsp resizeVolumeRsp) {
                        dbf.update(blockScsiLunVO);
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });

            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map map) {
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }
        }).start();
    }

    @Override
    protected void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolume().getUuid())
                .find();

        VolumeSnapshotInventory snapshotInventory = new VolumeSnapshotInventory();
        snapshotInventory.setPrimaryStorageInstallPath(String.valueOf(blockScsiLunVO.getLunInitSnapshotID()));
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());

        ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
        bkd.revertSnapshot(blockScsiLunVO, snapshotInventory, new ReturnValueCompletion<BlockScsiLunVO>(msg) {
            @Override
            public void success(BlockScsiLunVO returnValue) {
                reply.setNewVolumeInstallPath(blockScsiLunVO.getInstallPath());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(AskInstallPathForNewSnapshotMsg msg) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        bus.reply(msg, new CheckVolumeSnapshotOperationOnPrimaryStorageReply());
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void connectHostsToPS(List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS, Boolean isNewAdded, Completion completion) {
        if (blockPrimaryStorageHostRefVOS.isEmpty()) {
            completion.success();
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        logger.debug(String.format("connect-ps-get-bkd-%s", bkd.getVendorName()));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("connect-all-host-in-cluster-to-ps"));

        final BlockScsiLunVO heartbeatLunVO = generateHeartbeatLun(self.getUuid());
        final List<InitBlockPrimaryStorageOnHostConnectedMsg> msgs = new ArrayList<>();
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("sync-heartbeat-info");
            @Override
            public void run(FlowTrigger trigger, Map data) {
                blockPrimaryStorageFactory.createHeartbeatLunIfNotExist(self.getUuid(), bkd, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        heartbeatLunVO.setWwn(returnValue.getWwn());
                        heartbeatLunVO.setTarget(returnValue.getTarget());
                        heartbeatLunVO.setLunMapId(returnValue.getLunMapId());
                        heartbeatLunVO.setId(returnValue.getId());
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "connect-all-host-to-ps";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                blockPrimaryStorageHostRefVOS.forEach(blockPrimaryStorageHostRefVO -> {
                    InitBlockPrimaryStorageOnHostConnectedMsg msg = new InitBlockPrimaryStorageOnHostConnectedMsg();
                    msg.setNewAdded(isNewAdded);
                    msg.setPrimaryStorageUuid(blockPrimaryStorageHostRefVO.getPrimaryStorageUuid());
                    msg.setHostUuid(blockPrimaryStorageHostRefVO.getHostUuid());
                    msg.setInitiatorName(blockPrimaryStorageHostRefVO.getInitiatorName());
                    msg.setMetadata(blockPrimaryStorageHostRefVO.getMetadata());
                    bus.makeTargetServiceIdByResourceUuid(msg, BlockPrimaryStorageConstants.SERVICE_ID, blockPrimaryStorageHostRefVO.getPrimaryStorageUuid());
                    msgs.add(msg);
                });

                bus.send(msgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        MessageReply r = replies.stream().filter(reply -> reply.isSuccess() == Boolean.FALSE).findAny().orElse(null);
                        if (r == null) {
                            trigger.next();
                        } else {
                            trigger.fail(r.getError());
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("connect-ps-start-to-sync-physical-capacity");
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncPhysicalCapacity(self.getUuid(), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion){
        logger.debug(String.format("connect-ps-%s-attached-cluster-%s", self.getUuid(), self.getAttachedClusterRefs().toString()));
        List<String> attachedClusterUuids = self.getAttachedClusterRefs().stream()
                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                .collect(Collectors.toList());

        if (attachedClusterUuids.isEmpty()) {
            if (!param.isNewAdded()) {
                completion.fail(err(PrimaryStorageErrors.DISCONNECTED,"Block primary[uuid: %s] has not attached to any clusters", self.getUuid()));
                return;
            }
            completion.success();
            return;
        }

        List<HostVO> hostVOs = getHostsToConnectPS(Collections.singletonList(attachedClusterUuids));
        List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS = blockPrimaryStorageFactory.saveBlockPrimaryStorageHostsRefIfNotExist(hostVOs, self.getUuid());

        if (blockPrimaryStorageHostRefVOS.isEmpty()) {
            completion.fail(err(PrimaryStorageErrors.DISCONNECTED, "Fail to connect block primary[uuid: %s], because no connected host", self.getUuid()));
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("connect block primary chain");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                bkd.pingHook(new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                connectHostsToPS(blockPrimaryStorageHostRefVOS, param.isNewAdded(), new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map map) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                completion.fail(errorCode);
            }
        }).start();
    }

    private List<HostVO> getHostsToConnectPS(List<String> clusterUuidList) {
        List<HostVO> hosts = Q.New(HostVO.class)
                .in(HostVO_.clusterUuid, clusterUuidList)
                .eq(HostVO_.status, HostStatus.Connected)
                .notIn(HostVO_.state, list(HostState.PreMaintenance, HostState.Maintenance))
                .list();
        return hosts;
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        List<HostVO> hostVOs = getHostsToConnectPS(Collections.singletonList(clusterUuid));
        List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS = blockPrimaryStorageFactory.saveBlockPrimaryStorageHostsRefIfNotExist(hostVOs, self.getUuid());

        if (blockPrimaryStorageHostRefVOS.isEmpty()) {
            completion.fail(err(PrimaryStorageErrors.DISCONNECTED,
                    "Failed to attach block primary[uuid: %s] to cluster[uuid: %], because no connected host in cluster",
                    self.getUuid(), clusterUuid));
            return;
        }

        connectHostsToPS(blockPrimaryStorageHostRefVOS, true, new Completion(completion) {
            @Override
            public void success() {
                changeStatus(PrimaryStorageStatus.Connected);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (self.getStatus().equals(PrimaryStorageStatus.Connecting)) {
                    changeStatus(PrimaryStorageStatus.Disconnected);
                }
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void pingHook(Completion completion) {
        List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, self.getUuid())
                .listValues();
        if (clusterUuids.size() == 0) {
            completion.success();
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("ping block primary storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                bkd.pingHook(new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "sync ps capacity";
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                syncPhysicalCapacity(self.getUuid(), new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                completion.fail(errorCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map map) {
                completion.success();
            }
        }).start();
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof SetupSelfFencerOnKvmHostMsg) {
            handle((SetupSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof CancelSelfFencerOnKvmHostMsg) {
            handle((CancelSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PurgeSnapshotOnPrimaryStorageMsg) {
            handle((PurgeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromNbdToPrimaryStorageMsg) {
            handle((DownloadBitsFromNbdToPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if ((msg instanceof CleanUpTrashOnPrimaryStroageMsg)) {
            handle((CleanUpTrashOnPrimaryStroageMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if ((msg instanceof SelectBackupStorageMsg)) {
            handle((SelectBackupStorageMsg) msg);
        } else if ((msg instanceof CommitVolumeAsImageMsg)) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof CommitVolumeAsImageOnPrimaryStorageMsg) {
            handle((CommitVolumeAsImageOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateBlockPrimaryStorageMsg) {
            handle((APIUpdateBlockPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateBlockPrimaryStorageMsg msg) {
        APIUpdateBlockPrimaryStorageEvent event = new APIUpdateBlockPrimaryStorageEvent(msg.getId());
        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                .find();

        if (msg.getVendorName() != null) {
            blockPrimaryStorageVO.setVendorName(msg.getVendorName());
        }
        if (msg.getMetadata() != null) {
            blockPrimaryStorageVO.setMetadata(msg.getMetadata());
        }

        dbf.update(blockPrimaryStorageVO);

        BlockPrimaryStorageInventory inventory = BlockPrimaryStorageInventory.valueOf(
                (BlockPrimaryStorageVO) Q.New(BlockPrimaryStorageVO.class).eq(BlockPrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).find()
        );
        event.setInventory(inventory);
        bus.publish(event);
    }


    private void handle(UploadBitsToBackupStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(CancelSelfFencerOnKvmHostMsg msg) {
        final CancelSelfFencerOnKvmHostReply reply = new CancelSelfFencerOnKvmHostReply();
        KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param = msg.getParam();
        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
        blockPrimaryStorageKvmCommandDispatcher.setKvmHaCancelSelfFencer(param, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SetupSelfFencerOnKvmHostMsg msg) {
        final SetupSelfFencerOnKvmHostReply reply = new SetupSelfFencerOnKvmHostReply();
        KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param = msg.getParam();
        BlockScsiLunVO heartbeatLun = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.name, generateHeartbeatLunName(self.getUuid()))
                .find();
        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
        blockPrimaryStorageKvmCommandDispatcher.setKvmHaSetupSelfFencer(param, heartbeatLun.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(DownloadBitsFromNbdToPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(PurgeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply dreply = new DeleteImageCacheOnPrimaryStorageReply();
        String imageCacheName = generateImageCacheLunName(msg.getImageUuid());
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();

        String imageCacheNotFoundToken = "imageCacheNotFound";
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete image cache on block primary storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getLunByName(imageCacheName, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        blockScsiLunVO.setId(returnValue.getId());
                        blockScsiLunVO.setName(returnValue.getName());
                        data.put(imageCacheNotFoundToken, false);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        data.put(imageCacheNotFoundToken, true);
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public boolean skip(Map map) {
                Boolean immageCacheNotFound = (Boolean) map.get(imageCacheNotFoundToken);
                return immageCacheNotFound.equals(true);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.deleteLun(blockScsiLunVO.getId(), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                BlockScsiLunVO imagecache = Q.New(BlockScsiLunVO.class)
                        .eq(BlockScsiLunVO_.name, imageCacheName)
                        .find();
                dbf.remove(imagecache);
                bus.reply(msg, dreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                dreply.setError(errCode);
                bus.reply(msg, dreply);
            }
        }).start();
    }

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        reply.setError(operr("backing up snapshots to backup storage is a depreciated feature, which doesn't support on block primary storage"));
        bus.reply(msg, reply);
    }

    private void handle(GetDownloadBitsFromKVMHostProgressMsg msg) {
        // v2v feature need implement this msg, just return as not support
        GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
        reply.setError(operr("backing up snapshots to backup storage is a depreciated feature, which doesn't support on block primary storage"));
        bus.reply(msg, reply);
    }

    private void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {

        CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
        String volumeName = generateLunName(msg.getVolumeUuid());
        blockScsiLunVO.setName(volumeName);
        blockScsiLunVO.setVolumeUuid(msg.getVolumeUuid());
        blockScsiLunVO.setSize(msg.getSnapshot().getSize());
        blockScsiLunVO.setId(Integer.valueOf(msg.getSnapshot().getPrimaryStorageInstallPath()));

        BlockScsiLunVO newBlockScsiLunVO = new BlockScsiLunVO();
        newBlockScsiLunVO.setVolumeUuid(msg.getVolumeUuid());
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("create-volume-from-volume-snapshot-on-block-primary-storage");
        chain.then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLunFromSnapshot(blockScsiLunVO, volumeName, blockPrimaryStorageFactory.getVolumeProvisioningStrategy(msg.getPrimaryStorageUuid()), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        reply.setInstallPath(returnValue.getInstallPath());
                        reply.setSize(returnValue.getSize());
                        reply.setActualSize(returnValue.getUsedSize());
                        newBlockScsiLunVO.setUuid(Platform.getUuid());
                        newBlockScsiLunVO.setName(volumeName);
                        newBlockScsiLunVO.setLunType(returnValue.getLunType());
                        newBlockScsiLunVO.setSize(returnValue.getSize());
                        newBlockScsiLunVO.setId(returnValue.getId());
                        newBlockScsiLunVO.setWwn(returnValue.getWwn());
                        dbf.persist(newBlockScsiLunVO);
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
                if (blockScsiLunVO.getId() == null || blockScsiLunVO.getId().equals(0)) {
                    trigger.rollback();
                    return;
                }
                dbf.remove(blockScsiLunVO);
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg,reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(CheckSnapshotMsg msg) {
        //implemented as ceph ps did
        CheckSnapshotReply reply = new CheckSnapshotReply();
        bus.reply(msg, reply);
    }

    private void handle(CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        // For imagesotre, we just need to push image to store.
        final CommitVolumeAsImageOnPrimaryStorageReply reply = new CommitVolumeAsImageOnPrimaryStorageReply();
        reply.setBackupStorageUuid(msg.getBackupStorageUuid());

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        BlockScsiLunVO blockScsiTemplateLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, msg.getVolumeUuid())
                .find();
        BlockScsiLunVO newBlockScsiLunVO = new BlockScsiLunVO();
        newBlockScsiLunVO.setName(generateImageCacheLunName(msg.getImageUuid()));

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, newBlockScsiLunVO);
        data.put(BlockPrimaryStorageConstants.Params.TemplateLun, blockScsiTemplateLunVO);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("commit-volume-%s-as-image", msg.getVolumeUuid()));
        chain.enableProgressReport();
        chain.setData(data);
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("find-host-for-committing-volume-%s", msg.getVolumeUuid());
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> clusterList = Q.New(PrimaryStorageClusterRefVO.class)
                        .select(PrimaryStorageClusterRefVO_.clusterUuid)
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                        .listValues();
                if (clusterList == null || clusterList.isEmpty()) {
                    trigger.fail(operr("fail to find cluster for commit volume on ps:%s", msg.getPrimaryStorageUuid()));
                    return;
                }
                List<String> hostList = Q.New(HostVO.class)
                        .select(HostVO_.uuid)
                        .eq(HostVO_.clusterUuid, clusterList.get(0))
                        .listValues();

                if (hostList == null || hostList.isEmpty()) {
                    trigger.fail(operr("fail to find host for commit volume:%s", msg.getVolumeUuid()));
                    return;
                }
                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(hostList.get(0));
                data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);
                logger.debug(String.format("successfully find host:%s for committing volume:%s", hostList.get(0), msg.getVolumeUuid()));
                trigger.next();
            }
        });
        chain.then(new CreateLunFromTemplateFlow());
        chain.then(new MapLunToHostFlow());
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("upload template volume:%s to backup storage", JSONObjectUtil.toJsonString(newBlockScsiLunVO));
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = (BlockPrimaryStorageHostRefVO) data.get(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef);
                BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                BackupStorageBlockKvmUploader uploader = getBackupStorageKvmUploader(msg.getBackupStorageUuid());
                uploader.uploadBits(msg.getImageUuid(), msg.getBackupStorageUuid(), blockScsiLunVO.getInstallPath(), blockPrimaryStorageHostRefVO.getHostUuid(), new ReturnValueCompletion<String>(trigger) {
                    @Override
                    public void success(String returnValue) {
                        logger.debug(String.format("return value %s", returnValue));
                        reply.setBackupStorageInstallPath(returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.then(new DeleteLunMapFlow());
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        CommitVolumeAsImageFlowChain.getFlow(msg).run();
    }

    private void handle(TakeSnapshotMsg msg) {
        TakeSnapshotReply reply = new TakeSnapshotReply();
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(self.getUuid());
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, sp.getVolumeUuid())
                .find();
        blockScsiLunVO.setName(generateSnapshotName(msg.getStruct().getCurrent().getUuid()));
        bkd.takeSnapshot(blockScsiLunVO, new ReturnValueCompletion<TakeSnapshotOnHypervisorReply>(msg) {
            @Override
            public void success(TakeSnapshotOnHypervisorReply returnValue) {
                sp.setSize(returnValue.getSize());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(returnValue.getSnapshotInstallPath());
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                reply.setInventory(sp);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();
        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        reply.setInventory(new CommitVolumeAsImageFlowChain().selectBackupStorage(vol, msg.getRequiredSize(), msg.getRequiredBackupStorageTypes()));
        bus.reply(msg, reply);
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        String primaryStorageUuid = self.getUuid();
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(primaryStorageUuid);
        blockPrimaryStorageDeviceBackend.getPhysicalCapacityUsage(new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
            @Override
            public void success(PhysicalCapacityUsage physicalCapacityUsage) {
                completion.success(physicalCapacityUsage);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void syncPhysicalCapacity(String primaryStorageUuid, Completion completion) {
        logger.debug(String.format("start to sync primary storage:%s physical capacity", primaryStorageUuid));

        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = getBlockPrimaryStorageDeviceBackend(primaryStorageUuid);
        blockPrimaryStorageDeviceBackend.getPhysicalCapacityUsage(new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
            @Override
            public void success(PhysicalCapacityUsage physicalCapacityUsage) {
                new PrimaryStorageCapacityUpdater(primaryStorageUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
                    @Override
                    public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                        cap.setTotalCapacity(physicalCapacityUsage.totalPhysicalSize);
                        cap.setTotalPhysicalCapacity(physicalCapacityUsage.totalPhysicalSize);
                        cap.setAvailablePhysicalCapacity(physicalCapacityUsage.availablePhysicalSize);
                        cap.setAvailableCapacity(physicalCapacityUsage.availablePhysicalSize);
                        return  cap;
                    }
                });
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private BlockPrimaryStorageDeviceBackend getBlockPrimaryStorageDeviceBackend(String primaryStorageUuid) {
        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, primaryStorageUuid)
                .find();
        if (blockPrimaryStorageVO == null) {
            throw new CloudRuntimeException(String.format("No block primary storage found of uuid:%s, just return", primaryStorageUuid));
        }
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);
        return  blockPrimaryStorageDeviceBackend;
    }

    public String generateHeartbeatLunName(String primaryStorageUuid) {
        return blockPrimaryStorageFactory.generateHeartbeatLunName(primaryStorageUuid);
    }

    private String generateImageCacheLunName(String imageUuid) {
        return blockPrimaryStorageFactory.generateImageCacheLunName(imageUuid, self.getUuid());
    }

    private String generateLunName(String volumeUuid) {
        return BlockPrimaryStorageConstants.BLOCK_VOLUME_LUN_NAME_PREFIX + volumeUuid;
    }

    private String generateSnapshotName(String uuid) {
        return BlockPrimaryStorageConstants.BLOCK_SNAPSHOT_LUN_NAME_PREFIX + uuid;
    }

    private Long getLunNeededSize(ImageInventory image) {
        Long size = image.getSize();
        Long actualSize = image.getActualSize();
        return getLunNeededSize(actualSize, size);
    }

    private Long getLunNeededSize(Long actualSize, Long size) {
        Long neededSize = size > actualSize ? size : actualSize;
        return neededSize;
    }

    private BlockScsiLunVO generateHeartbeatLun(String primaryStorageUuid) {
        return blockPrimaryStorageFactory.generateHeartbeatLun(primaryStorageUuid);
    }

    private BlockPrimaryStorageHostRefVO getBlockPrimaryStorageHostRefVO(HostInventory hostInventory) {
        return getBlockPrimaryStorageHostRefVO(hostInventory, self.getUuid());
    }

    private BlockPrimaryStorageHostRefVO getBlockPrimaryStorageHostRefVO(HostInventory hostInventory, String primaryStorageUuid) {
        if(hostInventory == null || StringUtils.isEmpty(primaryStorageUuid)) {
            return null;
        }
        return getBlockPrimaryStorageHostRefVO(hostInventory.getUuid(), primaryStorageUuid);
    }

    private BlockPrimaryStorageHostRefVO getBlockPrimaryStorageHostRefVO(String hostUuid) {
        if (StringUtils.isEmpty(hostUuid)) {
            return null;
        }
        return getBlockPrimaryStorageHostRefVO(hostUuid, self.getUuid());
    }

    private BlockPrimaryStorageHostRefVO getBlockPrimaryStorageHostRefVO(String hostUuid, String primaryStorageUuid) {
        return  blockPrimaryStorageFactory.getBlockPrimaryStorageHostRefVO(hostUuid, primaryStorageUuid);
    }
}