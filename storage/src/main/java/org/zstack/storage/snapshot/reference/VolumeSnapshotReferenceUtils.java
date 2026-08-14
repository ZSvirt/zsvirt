package org.zstack.storage.snapshot.reference;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.ImageCacheVolumeRefVO;
import org.zstack.header.storage.primary.ImageCacheVolumeRefVO_;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.reference.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class VolumeSnapshotReferenceUtils {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotReferenceUtils.class);

    private static Function<String, String> getResourceLocateHostUuidGetter = resourceUuid -> null;

    // FIXME: replace it with location url
    public static void setGetResourceLocateHostUuidGetter(Function<String, String> getter) {
        VolumeSnapshotReferenceUtils.getResourceLocateHostUuidGetter = getter;
    }

    public static List<String> getAllReferenceVolumeUuids(VolumeSnapshotTree.SnapshotLeaf currentLeaf) {
        List<String> descendantUuids = currentLeaf.getDescendants().stream()
                .map(VolumeSnapshotInventory::getUuid)
                .collect(Collectors.toList());
        return Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.referenceVolumeUuid)
                .in(VolumeSnapshotReferenceVO_.volumeSnapshotUuid, descendantUuids)
                .notNull(VolumeSnapshotReferenceVO_.referenceVolumeUuid)  // TODO REMOVE IT
                .listValues();
    }

    public static Map<String, List<String>> getDirectReferencedSnapshotUuidsGroupByTree(String volumeUuid) {
        List<Tuple> ts = SQL.New("select ref.volumeSnapshotUuid, snapshot.treeUuid" +
                        " from VolumeSnapshotReferenceVO ref, VolumeSnapshotVO snapshot" +
                        " where ref.volumeUuid = :volumeUuid" +
                        " and ref.volumeSnapshotUuid = snapshot.uuid" +
                        " and ref.referenceVolumeUuid is not null" + // TODO REMOVE IT
                        " group by ref.volumeSnapshotUuid", Tuple.class)
                .param("volumeUuid", volumeUuid)
                .list();

        return ts.stream().collect(Collectors.groupingBy(
                tuple -> tuple.get(1, String.class),
                Collectors.mapping(tuple -> tuple.get(0, String.class), Collectors.toList())
        ));
    }

    // suppose that inner snapshot path is like: volumePath@snapshotId
    public static boolean isVolumeDirectlyReferenceByOthers(VolumeInventory volume) {
        return Q.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.volumeUuid, volume.getUuid())
                .like(VolumeSnapshotReferenceVO_.volumeSnapshotInstallUrl, volume.getInstallPath() + "%%")
                .isExists();
    }

    public static String getVolumeInstallUrlBackingOtherVolume(String volumeUuid) {
        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(volumeUuid);
        if (ref != null) {
            return ref.getReferenceInstallUrl();
        }

        return null;
    }

    // get volume snapshotUuids referenced by other volumes directly or indirectly
    // FIXME split different primary storage snapshot reference
    public static Set<String> getVolumeAllSnapshotsReferencedByOtherVolumes(String volumeUuid) {
        Set<String> refSnapshotUuids = getVolumeReferenceRef(volumeUuid).stream()
                .map(VolumeSnapshotReferenceVO::getVolumeSnapshotUuid)
                .collect(Collectors.toSet());
        if (refSnapshotUuids.isEmpty()) {
            return Collections.emptySet();
        }

        List<VolumeSnapshotVO> allSnapshots = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).list();
        // snapshotUuid in VolumeSnapshotReferenceVO may not exist in VolumeSnapshotVO,
        // maybe because the snapshot has been deleted db only after volume migration,
        // so we need to filter them out
        refSnapshotUuids.retainAll(allSnapshots.stream().map(VolumeSnapshotVO::getUuid).collect(Collectors.toSet()));
        if (refSnapshotUuids.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> allRefSnapshotUuids = new HashSet<>(refSnapshotUuids);
        Map<String, VolumeSnapshotTree> trees = allSnapshots.stream()
                .collect(Collectors.groupingBy(VolumeSnapshotVO::getTreeUuid, Collectors.toList()))
                .values().stream()
                .collect(Collectors.toMap(
                        snaps -> snaps.get(0).getTreeUuid(),
                        VolumeSnapshotTree::fromVOs
                ));

        List<VolumeSnapshotVO> refSnapshots = allSnapshots.stream()
                .filter(sp -> refSnapshotUuids.contains(sp.getUuid()))
                .collect(Collectors.toList());
        for (VolumeSnapshotVO refSnapshot : refSnapshots) {
            Set<String> ancestorUuids = trees.get(refSnapshot.getTreeUuid())
                    .findSnapshot(refSnapshot.getUuid())
                    .getAncestors().stream()
                    .map(VolumeSnapshotInventory::getUuid)
                    .collect(Collectors.toSet());
            allRefSnapshotUuids.addAll(ancestorUuids);
        }

        return allRefSnapshotUuids;
    }

    public static List<VolumeInventory> getReferenceVolume(String volumeUuid) {
        return SQL.New("select vol from VolumeVO vol, VolumeSnapshotReferenceVO ref" +
                        " where ref.volumeUuid = :volumeUuid" +
                        " and ref.referenceVolumeUuid = vol.uuid", VolumeVO.class)
                .param("volumeUuid", volumeUuid)
                .list().stream().map(it -> VolumeInventory.valueOf((VolumeVO)it)).collect(Collectors.toList());
    }

    public static void handleChainVolumeSnapshotReferenceAfterFlatten(VolumeInventory volume) {
        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(volume.getUuid());
        if (ref == null) {
            return;
        }

        if (ref.getReferenceType().equals(VolumeVO.class.getSimpleName())) {
            // it means volume has no snapshot, so the reference is leaf node, just delete it.
            deleteSnapshotRefLeafInTree(null, ref);
            return;
        }

        // it means volume has snapshot, reference type will be snapshot.
        String hasBackingSnapshotTreeUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.treeUuid)
                .eq(VolumeSnapshotTreeVO_.uuid, ref.getReferenceUuid())
                .findValue();

        if (isVolumeSnapshotTreeReferOtherVolumes(volume.getUuid(), hasBackingSnapshotTreeUuid)) {
            return;
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.state, VolumeSnapshotState.Disabled)
                .eq(VolumeSnapshotVO_.treeUuid, hasBackingSnapshotTreeUuid)
                .update();
        logger.warn(String.format("disable snapshot tree[volumeUuid: %s, uuid:%s] which is backing other volume[uuid:%s] " +
                "because of volume flatten.", volume.getUuid(), hasBackingSnapshotTreeUuid, ref.getVolumeUuid()));

        // FIXME: mark reference deleted and delete ps backing bits after volume expunge
        deleteSnapshotRef(ref);
    }

    public static void handleStorageVolumeSnapshotReferenceAfterFlatten(VolumeInventory volume) {
        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volume.getUuid())
                .eq(VolumeSnapshotReferenceVO_.referenceInstallUrl, volume.getInstallPath())
                .find();
        if (ref != null) {
            deleteSnapshotRefLeafInTree(null, ref);
        }
    }

    public static VolumeSnapshotReferenceVO buildSnapshotReferenceForNewVolumeIfNeed(VolumeInventory volume, String imageUuid) {
        VolumeSnapshotReferenceVO templateRef = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceUuid, imageUuid)
                .limit(1).find();
        if (templateRef == null) {
            return null;
        }

        // Deprecated
        if (!PrimaryStorageGlobalProperty.USE_SNAPSHOT_AS_INCREMENTAL_CACHE) {
            DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
            VolumeSnapshotReferenceVO volRef = templateRef.clone();
            volRef.setReferenceVolumeUuid(volume.getUuid());
            return dbf.persist(volRef);
        }

        VolumeSnapshotVO baseSnapshot = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, templateRef.getVolumeSnapshotUuid()).find();
        String baseImageUuid = Q.New(VolumeVO.class).select(VolumeVO_.rootImageUuid)
                .eq(VolumeVO_.uuid, baseSnapshot.getVolumeUuid())
                .findValue();
        return buildSnapshotReferenceForNewVolume(volume, baseSnapshot, baseImageUuid);
    }

    public static VolumeSnapshotReferenceVO buildSnapshotReferenceForNewVolume(VolumeInventory volume, VolumeSnapshotVO baseSnapshot, String baseImageUuid) {
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        VolumeSnapshotReferenceVO ref = new VolumeSnapshotReferenceVO();
        ref.setReferenceVolumeUuid(volume.getUuid());
        ref.setReferenceUuid(volume.getUuid());
        ref.setReferenceType(VolumeVO.class.getSimpleName());
        ref.setReferenceInstallUrl(volume.getInstallPath());
        ref.setVolumeSnapshotInstallUrl(baseSnapshot.getPrimaryStorageInstallPath());
        ref.setDirectSnapshotInstallUrl(baseSnapshot.getPrimaryStorageInstallPath());
        ref.setVolumeSnapshotUuid(baseSnapshot.getUuid());
        ref.setDirectSnapshotUuid(baseSnapshot.getUuid());
        ref.setVolumeUuid(baseSnapshot.getVolumeUuid());
        /*
        if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(baseSnapshot.getType())) {
            logger.debug(String.format("create volume snapshot reference[volumeSnapshotUuid:%s, referVolumeUuid:%s]",
                    ref.getVolumeSnapshotUuid(), ref.getReferenceVolumeUuid()));
            return dbf.persist(ref);
        }
        */

        VolumeSnapshotReferenceVO parentRef = getVolumeBackingRef(baseSnapshot.getVolumeUuid());

        if (parentRef != null && parentRef.getReferenceType().equals(VolumeSnapshotVO.class.getSimpleName())) {
            String parentSnapshotTreeUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.treeUuid)
                    .eq(VolumeSnapshotVO_.uuid, parentRef.getReferenceUuid())
                    .findValue();
            // different tree
            if (!baseSnapshot.getTreeUuid().equals(parentSnapshotTreeUuid)) {
                parentRef = null;
            }
        }

        VolumeSnapshotReferenceTreeVO tree;
        if (parentRef == null) {
            if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(baseSnapshot.getType())) {
                tree = getOrBuildStorageSnapshotRefTree(baseSnapshot, baseImageUuid);
            } else {
                tree = getOrBuildChainSnapshotRefTree(baseSnapshot, baseImageUuid);
            }
        } else {
            tree = Q.New(VolumeSnapshotReferenceTreeVO.class).eq(VolumeSnapshotReferenceTreeVO_.uuid, parentRef.getTreeUuid()).find();
            ref.setParentId(parentRef.getId());
        }

        ref.setTreeUuid(tree.getUuid());
        logger.debug(String.format("create volume snapshot reference[volumeSnapshotUuid:%s, referVolumeUuid:%s, treeUuid:%s]",
                ref.getVolumeSnapshotUuid(), ref.getReferenceVolumeUuid(), ref.getTreeUuid()));
        return dbf.persist(ref);
    }

    private static VolumeSnapshotReferenceTreeVO getOrBuildStorageSnapshotRefTree(VolumeSnapshotVO baseSnapshot, String baseImageUuid) {
        return new SQLBatchWithReturn<VolumeSnapshotReferenceTreeVO>() {
            @Override
            protected VolumeSnapshotReferenceTreeVO scripts() {
                VolumeVO vol = databaseFacade.getEntityManager().find(VolumeVO.class, baseSnapshot.getVolumeUuid(), LockModeType.PESSIMISTIC_WRITE);
                VolumeSnapshotReferenceTreeVO tree = Q.New(VolumeSnapshotReferenceTreeVO.class)
                        .eq(VolumeSnapshotReferenceTreeVO_.rootVolumeUuid, baseSnapshot.getVolumeUuid())
                        .eq(VolumeSnapshotReferenceTreeVO_.primaryStorageUuid, baseSnapshot.getPrimaryStorageUuid())
                        .find();
                if (tree != null) {
                    return tree;
                }

                tree = new VolumeSnapshotReferenceTreeVO();
                tree.setUuid(Platform.getUuid());
                tree.setRootImageUuid(baseImageUuid);
                tree.setRootVolumeUuid(baseSnapshot.getVolumeUuid());
                tree.setRootInstallUrl(vol.getInstallPath());
                tree.setPrimaryStorageUuid(baseSnapshot.getPrimaryStorageUuid());
                return persist(tree);
            }
        }.execute();
    }

    private static VolumeSnapshotReferenceTreeVO getOrBuildChainSnapshotRefTree(VolumeSnapshotVO baseSnapshot, String baseImageUuid) {
        String hostUuid = getResourceLocateHostUuidGetter.call(baseSnapshot.getVolumeUuid());
        return new SQLBatchWithReturn<VolumeSnapshotReferenceTreeVO>() {
            @Override
            protected VolumeSnapshotReferenceTreeVO scripts() {
                databaseFacade.getEntityManager().find(VolumeSnapshotTreeVO.class, baseSnapshot.getTreeUuid(), LockModeType.PESSIMISTIC_WRITE);
                Q q = Q.New(VolumeSnapshotReferenceTreeVO.class)
                        .eq(VolumeSnapshotReferenceTreeVO_.rootVolumeSnapshotTreeUuid, baseSnapshot.getTreeUuid())
                        .eq(VolumeSnapshotReferenceTreeVO_.primaryStorageUuid, baseSnapshot.getPrimaryStorageUuid());

                if (hostUuid != null) {
                    q.eq(VolumeSnapshotReferenceTreeVO_.hostUuid, hostUuid);
                }

                VolumeSnapshotReferenceTreeVO tree = q.find();
                if (tree != null) {
                    return tree;
                }

                Tuple t = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, baseSnapshot.getTreeUuid()).isNull(VolumeSnapshotVO_.parentUuid)
                        .select(VolumeSnapshotVO_.uuid, VolumeSnapshotVO_.primaryStorageInstallPath).findTuple();

                tree = new VolumeSnapshotReferenceTreeVO();
                tree.setUuid(Platform.getUuid());
                tree.setRootImageUuid(baseImageUuid);
                tree.setRootVolumeSnapshotTreeUuid(baseSnapshot.getTreeUuid());
                tree.setRootVolumeUuid(baseSnapshot.getVolumeUuid());
                tree.setRootVolumeSnapshotUuid(t.get(0, String.class));
                tree.setRootInstallUrl(t.get(1, String.class));
                tree.setPrimaryStorageUuid(baseSnapshot.getPrimaryStorageUuid());
                tree.setHostUuid(hostUuid);
                return persist(tree);
            }
        }.execute();
    }

    public static void rollbackSnapshotReferenceForNewVolume(String volumeUuid) {
        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(volumeUuid);
        if (ref != null) {
            deleteSnapshotRef(ref);
        }
    }

    public static void updateReferenceAfterFirstSnapshot(VolumeSnapshotVO snapshot) {
        if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(snapshot.getType())) {
            return;
        }

        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(snapshot.getVolumeUuid());
        if (ref != null && snapshot.getPrimaryStorageInstallPath().equals(ref.getReferenceInstallUrl())) {
            SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                    .set(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getUuid())
                    .set(VolumeSnapshotReferenceVO_.referenceType, VolumeSnapshotVO.class.getSimpleName())
                    .update();
            logger.debug(String.format("update volume snapshot reference[referVolumeSnapshotUuid:%s, referVolumeUuid:%s] for first snapshot",
                    snapshot.getUuid(), snapshot.getVolumeUuid()));
        }
    }

    public static void updateReferenceAfterMarkSnapshotAsVolume(VolumeSnapshotVO snapshot) {
        if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(snapshot.getType())) {
            return;
        }

        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(snapshot.getVolumeUuid());
        if (ref != null && snapshot.getPrimaryStorageInstallPath().equals(ref.getReferenceInstallUrl())) {
            SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                    .set(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getVolumeUuid())
                    .set(VolumeSnapshotReferenceVO_.referenceType, VolumeVO.class.getSimpleName())
                    .update();
            logger.debug(String.format("update volume snapshot reference[referVolumeSnapshotUuid:%s, referVolumeUuid:%s] after mark snapshot as volume",
                    snapshot.getUuid(), snapshot.getVolumeUuid()));
        }
    }

    public static void cleanTemporaryImageReference(List<ImageInventory> images) {
        if (!PrimaryStorageGlobalProperty.USE_SNAPSHOT_AS_INCREMENTAL_CACHE || CollectionUtils.isEmpty(images)) {
            return;
        }

        images.stream().filter(it -> it.getMediaType().equals(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())).findFirst().ifPresent(image -> {
            List<Long> cacheIds = Q.New(ImageCacheVO.class).select(ImageCacheVO_.id).eq(ImageCacheVO_.imageUuid, image.getUuid()).listValues();
            if (!cacheIds.isEmpty()) {
                SQL.New(ImageCacheVolumeRefVO.class).in(ImageCacheVolumeRefVO_.imageCacheId, cacheIds).delete();
                SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.getUuid()).delete();
                SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.referenceUuid, image.getUuid()).delete();
            }
        });
    }

    public static List<Long> filterStaleImageCache(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        return SQL.New("select c.id from ImageCacheVO c" +
                        " where c.id in (:ids)" +
                        " and c.imageUuid not in (select tree.rootImageUuid from VolumeSnapshotReferenceTreeVO tree" +
                        " where tree.rootImageUuid is not null)", Long.class)
                .param("ids", ids)
                .list();
    }

    public static void handleSnapshotDeletion(VolumeSnapshotVO snapshot) {
        if (snapshot == null || snapshot.getParentUuid() != null
                || VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(snapshot.getType())) {
            return;
        }

        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getUuid())
                .find();
        if (ref != null) {
            deleteSnapshotRefLeafInTree(null, ref);
        }
    }

    public static void handleVolumeDeletion(VolumeInventory volume) {
        if (volume == null) {
            return;
        }

        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(volume.getUuid());
        if (ref != null) {
            deleteAndRedirectSnapshotRef(ref);
        }
    }

    /***
     * handle volume or snapshot install path change, it MUST BE called in a TRANSACTION context, and the caller should
     * ensure the volume or snapshot install path will be changed in this transaction. the whole API process MUST BE
     * idempotent and safely interrupted without adverse effects.
     * @param volumeUuid the volume install url changed, contains volume and snapshot
     * @param oldAndNewPaths key is old path, value is new path
     */
    public static void handleVolumeInstallUrlChange(String volumeUuid, Map<String, String> oldAndNewPaths) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new CloudRuntimeException("handleVolumeInstallUrlChange must be called in a transaction context, " +
                    "and you should ensure the volume or snapshot install path will be changed in this transaction." +
                    "the whole API process MUST BE idempotent and safely interrupted without adverse effects.");
        }

        if (MapUtils.isEmpty(oldAndNewPaths)) {
            return;
        }

        List<VolumeInventory> refVols = getReferenceVolume(volumeUuid);
        if (!refVols.isEmpty()) {
            List<String> infos = refVols.stream().map(v -> String.format("uuid:%s, name:%s", v.getUuid(), v.getName())).collect(Collectors.toList());
            throw new OperationFailureException(operr(" volume[uuid: %s] has been referenced by other volumes [%s], " +
                    "can not change install path before flatten them and their descendants ", volumeUuid, infos.toString()));
        }

        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(volumeUuid);
        if (ref == null) {
            return;
        }
        if (oldAndNewPaths.containsKey(ref.getReferenceInstallUrl())) {
            SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                    .set(VolumeSnapshotReferenceVO_.referenceInstallUrl, oldAndNewPaths.get(ref.getReferenceInstallUrl()))
                    .update();

            logger.debug(String.format("update volume snapshot reference[referUuid:%s, referVolumeUuid:%s] install path from %s to %s",
                    ref.getReferenceUuid(), ref.getReferenceVolumeUuid(), ref.getReferenceInstallUrl(), oldAndNewPaths.get(ref.getReferenceInstallUrl())));
        } else {
            logger.warn(String.format("volume snapshot reference[referUuid:%s, referVolumeUuid:%s] install path %s not in oldAndNewPaths",
                    ref.getReferenceUuid(), ref.getReferenceVolumeUuid(), ref.getReferenceInstallUrl()));
        }
    }

    public static void handleVolumeOverwrite(VolumeInventory originVolume, String newVolumeUuid) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new CloudRuntimeException("handleVolumeUuidChange must be called in a transaction context, " +
                    "and you should ensure the volume or snapshot install path will be changed in this transaction." +
                    "the whole API process MUST BE idempotent and safely interrupted without adverse effects.");
        }

        VolumeSnapshotReferenceVO ref = getVolumeBackingRef(originVolume.getUuid());
        if (ref != null && ref.getReferenceUuid().equals(originVolume.getUuid())
                && ref.getReferenceInstallUrl().equals(originVolume.getInstallPath())
                && ref.getReferenceType().equals(VolumeVO.class.getSimpleName())) {
            SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                    .set(VolumeSnapshotReferenceVO_.referenceVolumeUuid, newVolumeUuid)
                    .update();
        }
    }

    private static void deleteAndRedirectSnapshotRef(VolumeSnapshotReferenceVO ref) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeSnapshotReferenceTreeVO tree = databaseFacade.getEntityManager().find(VolumeSnapshotReferenceTreeVO.class, ref.getTreeUuid(), LockModeType.PESSIMISTIC_WRITE);

                VolumeSnapshotReferenceVO finalRef =  databaseFacade.getEntityManager().find(VolumeSnapshotReferenceVO.class, ref.getId());
                List<Long> childrenRefIds = Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.id)
                        .eq(VolumeSnapshotReferenceVO_.treeUuid, finalRef.getTreeUuid())
                        .eq(VolumeSnapshotReferenceVO_.parentId, finalRef.getId())
                        .listValues();

                if (!childrenRefIds.isEmpty()) {
                    // redirect children refs to parent
                    sql(VolumeSnapshotReferenceVO.class).in(VolumeSnapshotReferenceVO_.id, childrenRefIds)
                            .set(VolumeSnapshotReferenceVO_.parentId, finalRef.getParentId())
                            .set(VolumeSnapshotReferenceVO_.volumeUuid, finalRef.getVolumeUuid())
                            .set(VolumeSnapshotReferenceVO_.volumeSnapshotUuid, finalRef.getVolumeSnapshotUuid())
                            .set(VolumeSnapshotReferenceVO_.volumeSnapshotInstallUrl, finalRef.getVolumeSnapshotInstallUrl())
                            .update();
                    logger.debug(String.format("redirect snapshot ref[ids:%s, volumeUuid: from %s to %s, parentId: to %s]",
                            childrenRefIds, finalRef.getReferenceVolumeUuid(), finalRef.getVolumeUuid(), finalRef.getParentId()));
                    deleteSnapshotRef(finalRef);
                } else {
                    deleteSnapshotRefLeafInTree(tree, finalRef);
                }

            }
        }.execute();
    }

    private static void deleteBitsOnPs(VolumeSnapshotReferenceTreeVO treeVO, VolumeSnapshotReferenceVO ref) {
        List<VolumeSnapshotReferenceVO> treeRefs = Q.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.treeUuid, treeVO.getUuid()).list();

        List<VolumeSnapshotReferenceInventory> otherLeafs;
        if (ref.getParentId() == null) {
            otherLeafs = treeRefs.stream().filter(it -> it.getParentId() == null).map(VolumeSnapshotReferenceInventory::valueOf).collect(Collectors.toList());
        } else {
            otherLeafs = treeRefs.stream().filter(it -> ref.getParentId().equals(it.getParentId())).map(VolumeSnapshotReferenceInventory::valueOf).collect(Collectors.toList());
        }
        otherLeafs.removeIf(it -> it.getId() == ref.getId());

        DeleteVolumeSnapshotReferenceLeafMsg msg = new DeleteVolumeSnapshotReferenceLeafMsg();
        msg.setLeaf(VolumeSnapshotReferenceInventory.valueOf(ref));
        msg.setOtherLeafs(otherLeafs);
        msg.setTree(treeVO.toInventory());
        VolumeVO deletedVolume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, ref.getReferenceVolumeUuid()).find();
        msg.setDeletedVolume(deletedVolume.toInventory());
        CloudBus bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, treeVO.getUuid());
        bus.send(msg);
    }

    private static void deleteSnapshotRef(VolumeSnapshotReferenceVO ref) {
        SQL.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                .hardDelete();
        logger.debug(String.format("remove volume snapshot reference[referenceVolumeUuid: %s].", ref.getReferenceVolumeUuid()));

        if (!Q.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.treeUuid, ref.getTreeUuid()).isExists()) {
            logger.debug(String.format("volume snapshot reference tree[uuid:%s] has no leaf, delete it", ref.getTreeUuid()));
            SQL.New(VolumeSnapshotReferenceTreeVO.class).eq(VolumeSnapshotReferenceTreeVO_.uuid, ref.getTreeUuid()).hardDelete();
        }
    }

    private static void deleteSnapshotRefLeafInTree(VolumeSnapshotReferenceTreeVO tree, VolumeSnapshotReferenceVO ref) {
        boolean referenceRedirected = !ref.getDirectSnapshotUuid().equals(ref.getVolumeSnapshotUuid());
        boolean backingVolumeDeletedInDb = SQL.New("select vol.uuid from VolumeVO vol, VolumeSnapshotReferenceTreeVO tree" +
                        " where vol.uuid = :volUuid" +
                        " and tree.uuid = :treeUuid" +
                        " and vol.primaryStorageUuid = tree.primaryStorageUuid", String.class)
                .param("volUuid", ref.getVolumeUuid())
                .param("treeUuid", ref.getTreeUuid())
                .find() == null;

        if (referenceRedirected || backingVolumeDeletedInDb) {
            if (tree == null) {
                tree = Q.New(VolumeSnapshotReferenceTreeVO.class).eq(VolumeSnapshotReferenceTreeVO_.uuid, ref.getTreeUuid()).find();
            }

            deleteBitsOnPs(tree, ref);
        }

        deleteSnapshotRef(ref);
    }

    // for chain snapshot
    private static boolean isVolumeSnapshotTreeReferOtherVolumes(String volumeUuid, String snapshotTreeUuid) {
        List<String> refSnapshotUuids = Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.volumeSnapshotUuid)
                .eq(VolumeSnapshotReferenceVO_.volumeUuid, volumeUuid)
                .listValues();

        if (refSnapshotUuids.isEmpty()) {
            return false;
        }

        return Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.treeUuid, snapshotTreeUuid)
                .in(VolumeSnapshotVO_.uuid, refSnapshotUuids)
                .isExists();
    }

    private static VolumeSnapshotReferenceVO getVolumeBackingRef(String volumeUuid) {
        return Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volumeUuid)
                .find();
    }

    private static List<VolumeSnapshotReferenceVO> getVolumeReferenceRef(String volumeUuid) {
        return Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.volumeUuid, volumeUuid)
                .list();
    }
}
