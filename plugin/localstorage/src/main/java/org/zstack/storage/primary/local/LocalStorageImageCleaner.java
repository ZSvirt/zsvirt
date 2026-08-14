package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
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
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.storage.primary.local.LocalStorageUtils.InstallPath;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by xing5 on 2016/7/20.
 */
public class LocalStorageImageCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LocalStorageImageCleaner.class);
    @Autowired
    private PluginRegistry pluginRgty;
    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    private boolean force;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOsForNewDeletedAndOld(String psUUid) {
        // 1. images has been deleted
        List<Long> staleImageCacheIds = new ArrayList<>();
        List<Long> imageDeletedCacheIds = getStaleImageCacheIds(psUUid);
        if (!CollectionUtils.isEmpty(imageDeletedCacheIds)) {
            staleImageCacheIds.addAll(imageDeletedCacheIds);
        }

        if (force){
            // 2. no vm used but image is not deleted
            // FIXME: it means nothing. think about host ref.
            String sql;
            if (psUUid == null) {
                sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select vm.imageUuid from VmInstanceVO vm) and" +
                        " c.primaryStorageUuid = pri.uuid and pri.type = :psType";
                staleImageCacheIds.addAll(SQL.New(sql).param("psType", getPrimaryStorageType()).list());
            } else {
                sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select vm.imageUuid from VmInstanceVO vm) and" +
                        " c.primaryStorageUuid = pri.uuid and pri.type = :psType and pri.uuid = :psUuid";
                staleImageCacheIds.addAll(SQL.New(sql).param("psType", getPrimaryStorageType()).param("psUuid", psUUid).list());
            }
        }

        if (staleImageCacheIds.isEmpty()) {
            return null;
        }

        staleImageCacheIds = staleImageCacheIds.stream().distinct().collect(Collectors.toList());
        String sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> deleted = cq.getResultList();

        Map<String, List<ImageCacheVO>> refMap = new HashMap<String, List<ImageCacheVO>>();
        for (ImageCacheVO c : deleted) {
            InstallPath p = new InstallPath();
            p.fullPath = c.getInstallUrl();
            p.disassemble();
            String hostUuid = p.hostUuid;

            List<ImageCacheVO> refs = refMap.computeIfAbsent(hostUuid, k -> new ArrayList<>());
            refs.add(c);
        }

        List<ImageCacheVO> stale = new ArrayList<>();
        for (Map.Entry<String, List<ImageCacheVO>> e : refMap.entrySet()) {
            String hostUuid = e.getKey();
            List<ImageCacheVO> refs = e.getValue();
            List<Long> cacheIds = transformAndRemoveNull(refs, ImageCacheVO::getId);

            sql = "select vol.rootImageUuid from VolumeVO vol where vol.rootImageUuid is not null and vol.status = :status";
            TypedQuery<String> query = dbf.getEntityManager().createQuery(sql, String.class);
            query.setParameter("status", VolumeStatus.NotInstantiated);
            List<String> filterIds = query.getResultList();

            // 3. no volume reference
            if (psUUid == null) {
                sql = "select c.id from ImageCacheVO c" +
                        " where c.imageUuid not in" +
                        " (select vol.rootImageUuid from VolumeVO vol, LocalStorageResourceRefVO ref" +
                        " where vol.uuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid" +
                        " and vol.rootImageUuid is not null)" +
                        " and c.id in (:ids)";
            } else {
                sql = "select c.id from ImageCacheVO c" +
                        " where c.imageUuid not in" +
                        " (select vol.rootImageUuid from VolumeVO vol, LocalStorageResourceRefVO ref" +
                        " where vol.uuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid" +
                        " and ref.primaryStorageUuid = :psUuid" +
                        " and vol.rootImageUuid is not null)" +
                        " and c.id in (:ids)";
            }
            TypedQuery<Long> iq = dbf.getEntityManager().createQuery(sql, Long.class);
            iq.setParameter("rtype", VolumeVO.class.getSimpleName());
            iq.setParameter("huuid", hostUuid);
            if (psUUid != null) {
                iq.setParameter("psUuid", psUUid);
            }
            iq.setParameter("ids", cacheIds);
            cacheIds = iq.getResultList();
            if (cacheIds.isEmpty()) {
                continue;
            }

            // 4. no volume snapshot tree reference
            if (psUUid == null) {
                sql = "select c from ImageCacheVO c" +
                        " where c.imageUuid not in" +
                        " (select tree.rootImageUuid from VolumeSnapshotTreeVO tree, LocalStorageResourceRefVO ref" +
                        " where tree.volumeUuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid" +
                        " and tree.rootImageUuid is not null)" +
                        " and c.id in (:ids)";
            } else {
                sql = "select c from ImageCacheVO c" +
                        " where c.imageUuid not in" +
                        " (select tree.rootImageUuid from VolumeSnapshotTreeVO tree, LocalStorageResourceRefVO ref" +
                        " where tree.volumeUuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.hostUuid = :huuid" +
                        " and ref.primaryStorageUuid = :psUuid" +
                        " and tree.rootImageUuid is not null)" +
                        " and c.id in (:ids)";
            }

            cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
            cq.setParameter("rtype", VolumeVO.class.getSimpleName());
            cq.setParameter("huuid", hostUuid);
            if (psUUid != null) {
                cq.setParameter("psUuid", psUUid);
            }
            cq.setParameter("ids", cacheIds);

            List<ImageCacheVO> results = cq.getResultList();
            results.removeIf(c -> filterIds.contains(c.getImageUuid()));

            stale.addAll(results);
        }

        if (stale.isEmpty()) {
            return null;
        }

        logger.debug(String.format("found %s stale images in cache on the primary storage[type:%s], they are about to be cleaned up",
                stale.size(), getPrimaryStorageType()));

        for (ImageCacheVO vo : stale) {
            dbf.getEntityManager().persist(new ImageCacheShadowVO(vo));
            dbf.getEntityManager().remove(vo);
        }

        sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        sq.setParameter("ptype", getPrimaryStorageType());
        return sq.getResultList();
    }

    private void cleanUpImageCache(String psUuid, NoErrorCompletion completion) {
        PrimaryStorageVO ps = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        logger.info(String.format("cleanup image cache on PrimaryStorage [%s]", ps.getUuid()));
        List<String> hostUuids = SQL.New("select h.uuid from LocalStorageHostRefVO ref, HostVO h " +
                "where ref.hostUuid = h.uuid and ref.primaryStorageUuid = :ps and h.status = :status").
                param("ps", psUuid).param("status", HostStatus.Connected).list();
        List<BackupStoragePrimaryStorageExtensionPoint> extensions = pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class);
        new While<>(extensions).each((ext, whileCompletion) ->
                new While<>(hostUuids).each((hostUuid, innerWhileCompletion) -> ext.cleanupPrimaryCacheForBS(PrimaryStorageInventory.valueOf(ps), hostUuid, new Completion(completion) {
            @Override
            public void success() {
                innerWhileCompletion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("Failed to clean primary cache for backup storage, on host[uuid:%s] and primary storage[uuid:%s]", hostUuid, psUuid));
                innerWhileCompletion.done();
            }
        })).run(new WhileDoneCompletion(whileCompletion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    @Override
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

            InstallPath p = new InstallPath();
            p.fullPath = vo.getInstallUrl();
            p.disassemble();

            if (!dbf.isExist(p.hostUuid, HostVO.class)){
                dbf.removeByPrimaryKey(vo.getId(), ImageCacheShadowVO.class);
                whileCompletion.done();
                return;
            }
            LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg = new LocalStorageDeleteImageCacheOnPrimaryStorageMsg();
            msg.setHostUuid(p.hostUuid);
            msg.setImageUuid(vo.getImageUuid());
            msg.setInstallPath(p.installPath);
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

    @Override
    protected void doCleanup(String psUuid, boolean needDestinationCheck, NoErrorCompletion completion) {
        List<String> psUuids = new ArrayList<>();
        if (psUuid == null) {
            psUuids.addAll(listPrimaryStoragesBySelfType());
        } else {
            psUuids.add(psUuid);
        }

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("do-clean-up-image-cache-on-local-storage-%s", psUuid));
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

    @Override
    public void cleanup(String psUuid, boolean needDestinationCheck) {
        ImageCacheCleaner self = this;
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return self.getClass().getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                logger.debug("start clean up cache");
                doCleanup(psUuid, needDestinationCheck, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("clean-up-image-cache-on-local-storage-%s", psUuid);
            }
        });
    }


}
