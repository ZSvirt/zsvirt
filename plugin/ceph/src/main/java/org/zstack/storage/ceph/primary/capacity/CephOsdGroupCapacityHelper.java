package org.zstack.storage.ceph.primary.capacity;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.ceph.CephCapacity;
import org.zstack.storage.ceph.CephPoolCapacity;
import org.zstack.storage.ceph.primary.CephOsdGroupVO;
import org.zstack.storage.ceph.primary.CephOsdGroupVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.stopwatch.StopWatch;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:35 2022/8/2
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephOsdGroupCapacityHelper {
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected DatabaseFacade dbf;

    private String primaryStorageUuid;

    private static final CLogger logger = Utils.getLogger(CephOsdGroupCapacityHelper.class);

    public CephOsdGroupCapacityHelper() {
    }

    public CephOsdGroupCapacityHelper(String psUuid) {
        this.primaryStorageUuid = psUuid;
    }

    private List<CephPrimaryStoragePoolVO> getOsdGroupRelatedPools(String osdGroupUuid) {
        List<CephPrimaryStoragePoolVO> pools = Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .list();
        return pools.stream()
                .filter(v -> v.getOsdGroup() != null && v.getOsdGroup().getUuid().equals(osdGroupUuid))
                .sorted(Comparator.comparing(CephPrimaryStoragePoolVO::getTotalCapacity))
                .collect(Collectors.toList());
    }

    public void fillCapacityFromPool() {
        List<CephOsdGroupVO> osdgs = Q.New(CephOsdGroupVO.class)
                .eq(CephOsdGroupVO_.primaryStorageUuid, primaryStorageUuid)
                .list();
        for (CephOsdGroupVO osdg : osdgs) {
            List<CephPrimaryStoragePoolVO> pools = getOsdGroupRelatedPools(osdg.getUuid());
            if (CollectionUtils.isEmpty(pools)) {
                logger.warn(String.format("it seems that osd group[%s] has no related pools", osdg.getUuid()));
                continue;
            }
            CephPrimaryStoragePoolVO poolVO = pools.get(0);
            osdg.setAvailablePhysicalCapacity(poolVO.getAvailableCapacity());
            osdg.setTotalPhysicalCapacity(poolVO.getTotalCapacity());
            dbf.getEntityManager().merge(osdg);
        }
    }

    private String getPoolUuidFromInstallPath(String installPath) {
        String path = installPath.replaceFirst("ceph://", "");
        String poolName = path.substring(0, path.lastIndexOf("/"));

        String poolUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.uuid)
                .eq(CephPrimaryStoragePoolVO_.poolName, poolName)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .limit(1)
                .findValue();

        if (poolUuid == null) {
            logger.info(String.format("cannot find path[%s] related pool, maybe pool has been deleted", path));
            return null;
        }

        return poolUuid;
    }

    @DeadlockAutoRestart
    public void releaseAvailableCapWithRatio(String installPath, long size) {
        long ratioSize = ratioMgr.calculateByRatio(primaryStorageUuid, size);
        _release(installPath, ratioSize);
    }

    @DeadlockAutoRestart
    public void releaseAvailableCapacity(String installPath, long size) {
        _release(installPath, size);
    }

    @Transactional
    private void _release(String installPath, long size) {
        if (installPath == null) {
            logger.debug("no install path found, skip release ceph pool capacity");
            return;
        }

        String poolUuid = getPoolUuidFromInstallPath(installPath);
        if (poolUuid == null) {
            return;
        }

        CephPrimaryStoragePoolVO pool = dbf.findByUuid(poolUuid, CephPrimaryStoragePoolVO.class);
        if (pool.getOsdGroup() == null) {
            logger.warn(String.format("cannot find ceph pool [%s] related osdgroup", poolUuid));
            return;
        }
        CephOsdGroupVO osdGroupVO = pool.getOsdGroup();
        osdGroupVO.setAvailableCapacity(osdGroupVO.getAvailableCapacity() + size);
        if (osdGroupVO.getAvailableCapacity() > osdGroupVO.getTotalPhysicalCapacity()) {
            logger.warn(String.format("invalid pool[uuid:%s] capacity after release size %s, available capacity[%s] > total capacity[%s], " +
                            "try to reconnect ceph ps to recalculate pool capacity",
                    poolUuid, size, osdGroupVO.getAvailableCapacity(), osdGroupVO.getTotalPhysicalCapacity()));
        }

        logger.debug(String.format("ceph osd group[%s] release capacity: %s, updated: %s",
                osdGroupVO.getUuid(), size, osdGroupVO.getAvailableCapacity()));
        dbf.getEntityManager().merge(osdGroupVO);
    }

    @DeadlockAutoRestart
    public long reserveAvailableCapacity(String installPath, long size) {
        return _reserve(installPath, size);
    }

    @Transactional
    private long _reserve(String installPath, long size) {
        if (installPath == null) {
            logger.debug("no install path found, skip reserve ceph pool capacity");
            return 0;
        }

        String poolUuid = getPoolUuidFromInstallPath(installPath);
        if (poolUuid == null) {
            return 0;
        }

        CephPrimaryStoragePoolVO pool = dbf.findByUuid(poolUuid, CephPrimaryStoragePoolVO.class);
        if (pool.getOsdGroup() == null) {
            logger.warn(String.format("cannot find ceph pool [%s] related osdgroup", poolUuid));
            return 0;
        }
        CephOsdGroupVO osdGroupVO = dbf.findByUuid(pool.getOsdGroup().getUuid(), CephOsdGroupVO.class);
        long originAvailableCapacity = osdGroupVO.getAvailableCapacity();
        if (originAvailableCapacity < size) {
            throw new OperationFailureException(operr("required ceph pool[uuid:%s] cannot satisfy conditions [availableSize > %s bytes], " +
                    "current available size %s", poolUuid, size, originAvailableCapacity));
        }

        osdGroupVO.setAvailableCapacity(osdGroupVO.getAvailableCapacity() - size);
        dbf.getEntityManager().merge(osdGroupVO);

        logger.debug(String.format("ceph osd group[%s] reserve capacity: %s, origin: %s, updated: %s",
                osdGroupVO.getUuid(), size, originAvailableCapacity, osdGroupVO.getAvailableCapacity()));
        return originAvailableCapacity;
    }

    public void recalculateAvailableCapacity() {
        List<CephOsdGroupVO> osdGroups = Q.New(CephOsdGroupVO.class)
                .eq(CephOsdGroupVO_.primaryStorageUuid, primaryStorageUuid)
                .list();

        for (CephOsdGroupVO osdGroupVO : osdGroups) {
            Long size = calculateAvailableCapacityByRatio(osdGroupVO, primaryStorageUuid);
            logger.info(String.format("ceph[%s] get osd group[%s] available virtual size: %s", primaryStorageUuid, osdGroupVO.getOsds(), size));
            osdGroupVO.setAvailableCapacity(size);
            dbf.update(osdGroupVO);
        }
    }

    public boolean checkVirtualSizeByRatio(String poolUuid, long requiredSize) {
        CephPrimaryStoragePoolVO pool = dbf.findByUuid(poolUuid, CephPrimaryStoragePoolVO.class);
        if (pool.getOsdGroup() == null) {
            throw new OperationFailureException(operr("cannot find ceph pool [%s] related osdgroup", poolUuid));
        }

        CephOsdGroupVO osdGroupVO = dbf.findByUuid(pool.getOsdGroup().getUuid(), CephOsdGroupVO.class);
        return PrimaryStorageCapacityChecker.New(osdGroupVO.getPrimaryStorageUuid(),
                osdGroupVO.getAvailableCapacity(), osdGroupVO.getTotalPhysicalCapacity(), osdGroupVO.getAvailablePhysicalCapacity())
                .checkRequiredSize(requiredSize);
    }

    public Long calculateAvailableCapacityByRatio(CephOsdGroupVO osdGroup, String primaryStorageUuid) {
        StopWatch watch = Utils.getStopWatch();
        watch.start();
        List<CephPrimaryStoragePoolVO> pools = getOsdGroupRelatedPools(osdGroup.getUuid());

        if (pools.isEmpty()) {
            logger.warn(String.format("ceph osd group[%s] has no related pools, skip it", osdGroup.getUuid()));
            return 0L;
        }

        List<String> poolNames = pools.stream()
                .map(CephPrimaryStoragePoolVO::getPoolName)
                .collect(Collectors.toList());

        poolNames = poolNames.stream().distinct().collect(Collectors.toList());

        long usedSize = 0;

        List<VolumeStatus> needCountVolumeStates = asList(VolumeStatus.Creating, VolumeStatus.Ready, VolumeStatus.Deleted);
        List<VolumeVO> volumes = Q.New(VolumeVO.class)
                .in(VolumeVO_.status, needCountVolumeStates)
                .eq(VolumeVO_.primaryStorageUuid, primaryStorageUuid)
                .list();
        List<ImageCacheVO> imageCaches = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.primaryStorageUuid, primaryStorageUuid).list();
        List<VolumeSnapshotVO> snapshots = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageUuid, primaryStorageUuid).list();

        for (String poolName : poolNames) {
            String installPathPrefix = String.format("ceph://%s", poolName);
            long volSize = volumes.parallelStream()
                    .filter(v -> v.getInstallPath() != null && v.getInstallPath()
                            .substring(0, v.getInstallPath().lastIndexOf("/"))
                            .equals((installPathPrefix)))
                    .map(VolumeVO::getSize)
                    .map(v -> Long.parseLong(String.valueOf(v)))
                    .reduce(0L, Long::sum);

            long imageCacheSize = imageCaches.parallelStream()
                    .filter(v -> v.getInstallUrl() != null && v.getInstallUrl()
                            .substring(0, v.getInstallUrl().lastIndexOf("/"))
                            .equals((installPathPrefix)))
                    .map(ImageCacheVO::getSize)
                    .map(v -> Long.parseLong(String.valueOf(v)))
                    .reduce(0L, Long::sum);

            long snapShotSize = snapshots.parallelStream()
                    .filter(v -> v.getPrimaryStorageInstallPath() != null && v.getPrimaryStorageInstallPath()
                            .substring(0, v.getPrimaryStorageInstallPath().lastIndexOf("/"))
                            .equals((installPathPrefix)))
                    .map(VolumeSnapshotVO::getSize)
                    .map(v -> Long.parseLong(String.valueOf(v)))
                    .reduce(0L, Long::sum);

            usedSize = usedSize + ratioMgr.calculateByRatio(primaryStorageUuid, volSize) + imageCacheSize + snapShotSize;
        }

        watch.stop();
        logger.info(String.format("it cost %d ms to calculate osdGroup[%s] used virtual size", watch.getLapse() ,osdGroup.getUuid()));
        return osdGroup.getTotalPhysicalCapacity() - usedSize;
    }

    public void calculatePrimaryStorageCapacityByOsds(CephCapacity cephCapacity) {
        long total = cephCapacity.getTotalCapacity();
        long avail = cephCapacity.getAvailableCapacity();
        List<CephPoolCapacity> poolCapacities = cephCapacity.getPoolCapacities();

        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(primaryStorageUuid);
        updater.run(cap -> {
            Map<String, CephPoolCapacity> osdPoolCapacity = new HashMap<>();
            List<String> poolsName = Q.New(CephPrimaryStoragePoolVO.class)
                    .select(CephPrimaryStoragePoolVO_.poolName)
                    .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                    .listValues();
            for (CephPoolCapacity poolCapacitiy : poolCapacities) {
                if (poolCapacitiy.getRelatedOsdCapacity() == null || !poolsName.contains(poolCapacitiy.getName())) {
                    continue;
                }

                Map<String, CephPoolCapacity> osdPoolCap = poolCapacitiy.getRelatedOsdCapacity().keySet().stream()
                        .collect(Collectors.toMap(osdName -> osdName, osdName -> {
                            if (!osdPoolCapacity.containsKey(osdName)) {
                                return poolCapacitiy;
                            }
                            // osd multiplexing
                            Float diskUtilization = osdPoolCapacity.get(osdName).getDiskUtilization();
                            return diskUtilization > poolCapacitiy.getDiskUtilization() ? poolCapacitiy : osdPoolCapacity.get(osdName);
                        }));
                osdPoolCapacity.putAll(osdPoolCap);
            }

            long osdsTotalSize = 0;
            long osdsAvailCap = 0;
            for (String osdName : osdPoolCapacity.keySet()) {
                CephPoolCapacity cephPoolCapacity = osdPoolCapacity.get(osdName);
                osdsTotalSize += cephPoolCapacity.getRelatedOsdCapacity().get(osdName).getSize() * cephPoolCapacity.getDiskUtilization();
                osdsAvailCap += cephPoolCapacity.getRelatedOsdCapacity().get(osdName).getAvailableCapacity() * cephPoolCapacity.getDiskUtilization();
            }
            if (osdsTotalSize == 0) {
                logger.debug("no osds related to pool found, using bare capacity instead, fsid: " + cephCapacity.getFsid());
                osdsTotalSize = total;
                osdsAvailCap = avail;
            }

            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                // init
                cap.setAvailableCapacity(osdsAvailCap);
            }

            cap.setTotalCapacity(osdsTotalSize);
            cap.setTotalPhysicalCapacity(osdsTotalSize);
            cap.setAvailablePhysicalCapacity(osdsAvailCap);
            return cap;
        });
    }
}
