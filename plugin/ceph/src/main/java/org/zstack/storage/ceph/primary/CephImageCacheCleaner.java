package org.zstack.storage.ceph.primary;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by xing5 on 2016/7/23.
 */
public class CephImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephImageCacheCleaner.class);

    @Override
    protected String getPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    protected GlobalConfig cleanupIntervalConfig() {
        return CephGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL;
    }

    @Transactional
    @Override
    protected List<ImageCacheShadowVO> createShadowImageCacheVOsForNewDeletedAndOld(String psUuid) {
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUuid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        String sql = "select ref.imageCacheId from ImageCacheVolumeRefVO ref where ref.imageCacheId in (:ids)";
        TypedQuery<Long> refq = dbf.getEntityManager().createQuery(sql, Long.class);
        refq.setParameter("ids", staleImageCacheIds);
        List<Long> existing = refq.getResultList();

        staleImageCacheIds.removeAll(existing);

        if (staleImageCacheIds.isEmpty()) {
            return null;
        }

        sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> fq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        fq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> stale = fq.getResultList();

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

    @Override
    public void managementNodeReady() {
        startGC();
    }
}
