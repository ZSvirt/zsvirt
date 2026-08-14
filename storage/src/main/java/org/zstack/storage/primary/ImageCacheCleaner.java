package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/7/18.
 */
public abstract class ImageCacheCleaner {
    private static final CLogger logger = Utils.getLogger(ImageCacheCleaner.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ResourceDestinationMaker destMaker;
    @Autowired
    private PluginRegistry pluginRgty;

    protected Future<Void> gcThread;

    protected abstract String getPrimaryStorageType();
    protected List<String> listPrimaryStoragesBySelfType() {
        return Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, getPrimaryStorageType())
                .listValues();
    };

    protected void startGC() {
        cleanupIntervalConfig().installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startGCThread();
            }
        });

        startGCThread();
    }

    protected GlobalConfig cleanupIntervalConfig() {
        return PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL;
    }

    public void cleanup(boolean needDestinationCheck) {
        cleanup(null, needDestinationCheck);
    }

    public void cleanup(String psUuid, boolean needDestinationCheck) {
        ImageCacheCleaner self = this;
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return self.getClass().getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCleanup(psUuid, needDestinationCheck, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("clean-up-image-cache-on-%s", psUuid);
            }
        });
    }

    protected void cleanUpVolumeCache(String psUuid, boolean needDestinationCheck, NoErrorCompletion completion) {
        List<ImageCacheShadowVO> shadowVOs = createShadowImageCacheVOs(psUuid);
        if (shadowVOs == null || shadowVOs.isEmpty()) {
            completion.done();
            return;
        }

        new While<>(shadowVOs).each((vo, whileCompletion) -> {
            if (needDestinationCheck && !destMaker.isManagedByUs(vo.getImageUuid())) {
                whileCompletion.done();
                return;
            }

            DeleteImageCacheOnPrimaryStorageMsg msg = new DeleteImageCacheOnPrimaryStorageMsg();
            msg.setImageUuid(vo.getImageUuid());
            msg.setInstallPath(vo.getInstallUrl());
            msg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to delete the stale image cache[%s] on the primary storage[%s], %s," +
                                "will re-try later", vo.getInstallUrl(), vo.getPrimaryStorageUuid(), reply.getError()));
                        whileCompletion.done();
                        return;
                    }

                    logger.debug(String.format("successfully deleted the stale image cache[%s] on the primary storage[%s]",
                            vo.getInstallUrl(), vo.getPrimaryStorageUuid()));
                    dbf.remove(vo);
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void cleanUpImageCache(String psUuid, NoErrorCompletion completion) {
        PrimaryStorageVO ps = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        logger.info(String.format("cleanup image cache on PrimaryStorage [%s]", ps.getUuid()));
        List<BackupStoragePrimaryStorageExtensionPoint> extensions = pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class);

        new While<>(extensions).each((ext, whileCompletion) -> ext.cleanupPrimaryCacheForBS(PrimaryStorageInventory.valueOf(ps), null, new Completion(completion) {
            @Override
            public void success() {
                whileCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("failed to clean primary cache for backup storage, on primary storage[uuid:%s]", psUuid));
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    protected void doCleanup(String psUuid, boolean needDestinationCheck, NoErrorCompletion completion) {
        List<String> psUuids = new ArrayList<>();
        if (psUuid == null) {
            psUuids.addAll(listPrimaryStoragesBySelfType());
        } else {
            psUuids.add(psUuid);
        }

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("do-clean-up-image-cache-on-%s", psUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                cleanUpVolumeCache(psUuid, needDestinationCheck, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (psUuids.isEmpty()) {
                    logger.debug("cannot find any primary storage, skip image cache clean up");
                    trigger.next();
                    return;
                }

                new While<>(psUuids).each((uuid, compl) -> {
                    cleanUpImageCache(uuid, new NoErrorCompletion() {
                        @Override
                        public void done() {
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (psUuids.isEmpty()) {
                    logger.debug("cannot find any primary storage, skip sync primary storage capacity");
                    trigger.next();
                    return;
                }

                new While<>(psUuids).each((uuid, compl) -> {
                    SyncPrimaryStorageCapacityMsg msg = new SyncPrimaryStorageCapacityMsg();
                    msg.setPrimaryStorageUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.debug(String.format("failed to clean up image cache because: %s", errCode.getReadableDetails()));
                completion.done();
            }
        }).start();
    }

    private synchronized void startGCThread() {
        if (gcThread != null) {
            gcThread.cancel(true);
        }

        logger.debug(String.format("%s starts with the interval %s secs", this.getClass().getSimpleName(), PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.value(Long.class)));

        gcThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return cleanupIntervalConfig().value(Long.class);
            }

            @Override
            public String getName() {
                return "image-cache-cleanup-thread";
            }

            @Override
            public void run() {
                cleanup(true);
            }
        });
    }

    @Transactional
    protected boolean volumeFindMissingImageUuid(String psUuid) {
        String sql;
        if (psUuid == null) {
            sql = "select count(*) from VolumeVO vol, PrimaryStorageVO pri where vol.primaryStorageUuid = pri.uuid" +
                    " and vol.type = :volType and vol.rootImageUuid is null and pri.type = :psType";
        } else {
            sql = "select count(*) from VolumeVO vol, PrimaryStorageVO pri where vol.primaryStorageUuid = pri.uuid" +
                    " and vol.type = :volType and vol.rootImageUuid is null and pri.type = :psType and pri.uuid = :psUuid";
        }

        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("volType", VolumeType.Root);
        q.setParameter("psType", getPrimaryStorageType());
        if (psUuid != null) {
            q.setParameter("psUuid", psUuid);
        }

        Long count = q.getSingleResult();
        if (count != 0) {
            logger.warn(String.format("found %s volumes on the primary storage[type:%s] has NULL rootImageUuid. Please do following:\n" +
                    "1. zstack-ctl stop_node\n" +
                    "2. zstack-ctl start_node -DfixImageCacheUuid=true -DrootVolumeFindMissingImageUuid=true\n" +
                    "to fix the problem. For the data safety, we won't clean the image cache of the primary storage", count, getPrimaryStorageType()));
            return true;
        }

        return false;
    }

    /***
     *
     * @param psUuid image cache to be cleaned up on the primary storage, if null, clean up same type of primary storage
     * @return image cache ids whose image is expunged
     */
    @Transactional
    protected List<Long> getStaleImageCacheIds(String psUuid) {
        if (volumeFindMissingImageUuid(psUuid)) {
            return null;
        }

        String sql;
        if (psUuid == null) {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where c.primaryStorageUuid = pri.uuid and i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype";
        } else  {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where c.primaryStorageUuid = pri.uuid and i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype and pri.uuid = :psUuid";
        }

        TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("ptype", getPrimaryStorageType());
        if (psUuid != null) {
            cq.setParameter("psUuid", psUuid);
        }
        List<Long> deleted = cq.getResultList();

        if (psUuid == null) {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                    " c.primaryStorageUuid = pri.uuid and pri.type = :psType";
        } else {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                    " c.primaryStorageUuid = pri.uuid and pri.type = :psType and pri.uuid = :psUuid";
        }

        cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("psType", getPrimaryStorageType());
        if (psUuid != null) {
            cq.setParameter("psUuid", psUuid);
        }
        deleted.addAll(cq.getResultList());

        if (deleted.isEmpty()) {
            return null;
        }

        return VolumeSnapshotReferenceUtils.filterStaleImageCache(deleted);
    }


    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOsForNewDeletedAndOld(String psUuid) {
        // 1. image has been deleted
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUuid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        // 2. no volume refers to the image
        String sql = "select c.id from ImageCacheVO c" +
                " where c.imageUuid not in (select vol.rootImageUuid from VolumeVO vol where vol.rootImageUuid is not null)" +
                " and c.id in (:ids)";
        TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("ids", staleImageCacheIds);
        staleImageCacheIds = cq.getResultList();

        if (staleImageCacheIds.isEmpty()) {
            return null;
        }

        // 3. no volume snapshot tree refers to the image
        sql = "select c from ImageCacheVO c" +
                " where c.imageUuid not in (select tree.rootImageUuid from VolumeSnapshotTreeVO tree where tree.rootImageUuid is not null)" +
                " and c.id in (:ids)";
        TypedQuery<ImageCacheVO> tq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        tq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> stales = tq.getResultList();

        logger.debug(String.format("found %s stale images in cache on the primary storage[type:%s], they are about to be cleaned up",
                stales.size(), getPrimaryStorageType()));

        for (ImageCacheVO vo : stales) {
            dbf.getEntityManager().persist(new ImageCacheShadowVO(vo));
            dbf.getEntityManager().remove(vo);
        }

        sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        sq.setParameter("ptype", getPrimaryStorageType());
        return sq.getResultList();
    }

    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs(String psUuid) {
        List<ImageCacheShadowVO> newDeletedAndOld = createShadowImageCacheVOsForNewDeletedAndOld(psUuid);
        if (newDeletedAndOld == null) {
            // no new deleted images, let's check if there any old that failed to be deleted last time
            String sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
            TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
            sq.setParameter("ptype", getPrimaryStorageType());
            return sq.getResultList();
        } else {
            return newDeletedAndOld;
        }
    }
}
