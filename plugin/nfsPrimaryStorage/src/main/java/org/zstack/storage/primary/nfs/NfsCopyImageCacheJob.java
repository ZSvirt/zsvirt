package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.util.Map;

/**
 * Created by GuoYi on 09/01/18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsCopyImageCacheJob implements Job {
    private static final CLogger logger = Utils.getLogger(NfsCopyImageCacheJob.class);

    @JobContext
    private HostInventory host;
    @JobContext
    private ImageCacheInventory srcImageCache;
    @JobContext
    private PrimaryStorageInventory srcPrimaryStorage;
    @JobContext
    private PrimaryStorageInventory dstPrimaryStorage;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public void run(final ReturnValueCompletion<Object> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("copy-imagecache-%s-from-nfsps-%s-to-nfsps-%s",
                srcImageCache.getImageUuid(), srcPrimaryStorage.getUuid(), dstPrimaryStorage.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean allocated = false;
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                        amsg.setRequiredPrimaryStorageUuid(dstPrimaryStorage.getUuid());
                        amsg.setSize(srcImageCache.getSize());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                        amsg.setNoOverProvisioning(true);
                        amsg.setImageUuid(srcImageCache.getImageUuid());
                        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    allocated = true;
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (allocated) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setDiskSize(srcImageCache.getSize());
                            imsg.setNoOverProvisioning(true);
                            imsg.setPrimaryStorageUuid(dstPrimaryStorage.getUuid());
                            bus.makeLocalServiceId(imsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(imsg);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "copy-imagecache-to-dstps";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String srcCacheFolderPath = getVolumeFolderPath(srcImageCache.getInstallUrl(),
                                srcPrimaryStorage.getMountPath(), srcPrimaryStorage.getMountPath());
                        String dstCacheFolderPath = getVolumeFolderPath(srcImageCache.getInstallUrl(),
                                srcPrimaryStorage.getMountPath(), dstPrimaryStorage.getMountPath());

                        NfsToNfsMigrateBitsMsg msg = new NfsToNfsMigrateBitsMsg();
                        msg.setHostUuid(host.getUuid());
                        msg.setSrcFolderPath(srcCacheFolderPath);
                        msg.setDstFolderPath(dstCacheFolderPath);
                        msg.setSrcPrimaryStorageUuid(srcPrimaryStorage.getUuid());
                        msg.setPrimaryStorageUuid(dstPrimaryStorage.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, dstPrimaryStorage.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    logger.info(String.format("Migrated image cache of %s from PS %s to PS %s.",
                                            srcImageCache.getImageUuid(),
                                            srcPrimaryStorage.getUuid(),
                                            dstPrimaryStorage.getUuid()
                                    ));
                                    trigger.next();
                                } else {
                                    logger.error(String.format("Failed to migrate image cache of %s from PS %s to PS %s.",
                                            srcImageCache.getImageUuid(),
                                            srcPrimaryStorage.getUuid(),
                                            dstPrimaryStorage.getUuid()
                                    ));
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        String dstCacheInstallPath = srcImageCache.getInstallUrl().replace(
                                srcPrimaryStorage.getMountPath(), dstPrimaryStorage.getMountPath()
                        );

                        ImageCacheVO cvo = new ImageCacheVO();
                        cvo.setImageUuid(srcImageCache.getImageUuid());
                        cvo.setInstallUrl(dstCacheInstallPath);
                        cvo.setMd5sum("no md5");
                        cvo.setPrimaryStorageUuid(dstPrimaryStorage.getUuid());
                        cvo.setSize(srcImageCache.getSize());
                        cvo.setMediaType(ImageMediaType.valueOf(srcImageCache.getMediaType()));
                        cvo = dbf.persistAndRefresh(cvo);
                        logger.debug(String.format("successfully copied cache of image[uuid:%s] to image cache[id:%s, path:%s]",
                                srcImageCache.getImageUuid(), cvo.getId(), cvo.getInstallUrl()));
                        completion.success(ImageCacheInventory.valueOf(cvo));
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }

    public ImageCacheInventory getSrcImageCache() {
        return srcImageCache;
    }

    public void setSrcImageCache(ImageCacheInventory srcImageCache) {
        this.srcImageCache = srcImageCache;
    }

    public PrimaryStorageInventory getSrcPrimaryStorage() {
        return srcPrimaryStorage;
    }

    public void setSrcPrimaryStorage(PrimaryStorageInventory srcPrimaryStorage) {
        this.srcPrimaryStorage = srcPrimaryStorage;
    }

    public PrimaryStorageInventory getDstPrimaryStorage() {
        return dstPrimaryStorage;
    }

    public void setDstPrimaryStorage(PrimaryStorageInventory dstPrimaryStorage) {
        this.dstPrimaryStorage = dstPrimaryStorage;
    }

    private String getVolumeFolderPath(String srcInstallPath, String srcPsMountPath, String dstPsMountPath) {
        File vol = new File(srcInstallPath);
        if (vol.getParentFile().getName().equals("snapshots")) {
            return vol.getParentFile().getParent().replace(srcPsMountPath, dstPsMountPath);
        } else {
            return vol.getParent().replaceFirst(srcPsMountPath, dstPsMountPath);
        }
    }
}
