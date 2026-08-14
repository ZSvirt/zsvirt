package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.distinctByKey;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageMainAllocatorFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageMainAllocatorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;

    private class Result {
        List<PrimaryStorageVO> result;
        String error;
    }

    @Transactional(readOnly = true)
    private Result allocate(Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        logger.debug(JSONObjectUtil.toJsonString(spec));
        TypedQuery<PrimaryStorageVO> query;
        String errorInfo;
        String sql;
        String primaryStorageCandidateSql = spec.getCandidatePrimaryStorageUuids().isEmpty() ? "" : String.format(" and pri.uuid in ('%s')",
                String.join("','", spec.getCandidatePrimaryStorageUuids()));
        if (spec.getRequiredHostUuid() != null) {
            sql = "select pri" +
                    " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host" +
                    " where host.uuid = :huuid" +
                    " and host.clusterUuid = ref.clusterUuid" +
                    " and ref.primaryStorageUuid = pri.uuid" +
                    " and pri.status = :status" +
                    " and pri.state = :priState" +
                    primaryStorageCandidateSql +
                    " and pri.uuid not in (" +
                    " select phref.primaryStorageUuid from PrimaryStorageHostRefVO phref" +
                    " where phref.hostUuid = :huuid" +
                    " and phref.status != :phStatus" +
                    " )";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("huuid", spec.getRequiredHostUuid());
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("phStatus", PrimaryStorageHostStatus.Connected);
            errorInfo = i18n("cannot find primary storage satisfying conditions" +
                            "[connected to host:%s, state:%s, status: %s, available capacity > %s",
                    spec.getRequiredHostUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredClusterUuids() != null && !spec.getRequiredClusterUuids().isEmpty()) {
            sql = "select pri" +
                    " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid in (:clusterUuids)" +
                    " and ref.primaryStorageUuid = pri.uuid" +
                    primaryStorageCandidateSql +
                    " and pri.status = :status" +
                    " and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("clusterUuids", spec.getRequiredClusterUuids());
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            errorInfo = String.format("cannot find primary storage satisfying conditions" +
                            "[attached to clusters:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredClusterUuids(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredZoneUuid() != null) {
            sql = "select pri" +
                    " from PrimaryStorageVO pri" +
                    " where pri.zoneUuid = :zoneUuid" +
                    primaryStorageCandidateSql +
                    " and pri.status = :status" +
                    " and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            errorInfo = String.format("cannot find primary storage satisfying conditions[in zone:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredZoneUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else {
            sql = "select pri" +
                    " from PrimaryStorageVO pri" +
                    " where pri.status = :status" +
                    primaryStorageCandidateSql +
                    " and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            errorInfo = String.format("cannot find primary storage satisfying conditions[state:%s, status:%s, available capacity > %s",
                    PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        }

        List<PrimaryStorageVO> vos = query.getResultList()
                .stream().filter(distinctByKey(PrimaryStorageVO::getUuid)).collect(Collectors.toList());
        logger.debug("select primary storage by sql: " + sql);

        /**
         * 1. if ps is indicated, then choose it directly
         * 2. if ps is not indicated and exclude ps is indicated, then exclude it and choose others
         */
        if (spec.getPossiblePrimaryStorageTypes() != null && !spec.getPossiblePrimaryStorageTypes().isEmpty()) {
            Iterator<PrimaryStorageVO> it = vos.iterator();
            while (it.hasNext()) {
                PrimaryStorageVO psvo = it.next();
                if (!spec.getPossiblePrimaryStorageTypes().contains(psvo.getType())) {
                    logger.debug(String.format("the primary storage[name:%s, uuid:%s, type:%s] is not in possible primary storage types[%s]," +
                            " remove it", psvo.getName(), psvo.getUuid(), psvo.getType(), spec.getPossiblePrimaryStorageTypes()));
                    it.remove();
                }
            }
        }

        if (spec.getCandidatePrimaryStorageUuids().isEmpty() && spec.getExcludePrimaryStorageTypes() != null && !spec.getExcludePrimaryStorageTypes().isEmpty()) {
            Iterator<PrimaryStorageVO> it = vos.iterator();
            while (it.hasNext()) {
                PrimaryStorageVO psvo = it.next();
                if (spec.getExcludePrimaryStorageTypes().contains(psvo.getType())) {
                    logger.debug(String.format("the primary storage[name:%s, uuid:%s, type:%s] is in exclude primary storage types[%s]," +
                            " remove it", psvo.getName(), psvo.getUuid(), psvo.getType(), spec.getExcludePrimaryStorageTypes()));
                    it.remove();
                }
            }
        }


        List<PrimaryStorageVO> res = new ArrayList<>();
        if (PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())) {
            res.addAll(considerImageCache(spec, vos));
            res = considerImageBackupStorageRef(spec, vos);
        } else {
            for (PrimaryStorageVO vo : vos) {
                if (ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(vo.getUuid(),
                        vo.getCapacity().getAvailableCapacity()) > spec.getSize()) {
                    res.add(vo);
                }
            }
        }

        Result ret = new Result();
        ret.error = errorInfo;
        ret.result = res;
        return ret;
    }

    @Transactional(readOnly = true)
    private Collection<? extends PrimaryStorageVO> considerImageCache(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> vos) {
        if (spec.getImageUuid() == null) {
            return vos;
        }

        List<PrimaryStorageVO> res = new ArrayList<>();
        if (vos.isEmpty()) {
            return res;
        }

        List<String> psUuids = transformAndRemoveNull(vos, ResourceVO::getUuid);

        String sql = "select i.primaryStorageUuid from ImageCacheVO i where i.primaryStorageUuid in (:psUuids) and i.imageUuid = :iuuid";
        TypedQuery<String> iq = dbf.getEntityManager().createQuery(sql, String.class);
        iq.setParameter("psUuids", psUuids);
        iq.setParameter("iuuid", spec.getImageUuid());
        List<String> hasImagePrimaryStorage = iq.getResultList();

        long imageSize = 0;
        sql = "select i.actualSize from ImageVO i where i.uuid = :uuid";
        TypedQuery<Long> sq = dbf.getEntityManager().createQuery(sql, Long.class);
        sq.setParameter("uuid", spec.getImageUuid());
        List<Long> resultList = sq.getResultList();
        if (resultList.isEmpty()) {
            resultList = Q.New(ImageCacheVO.class)
                    .select(ImageCacheVO_.size)
                    .eq(ImageCacheVO_.imageUuid, spec.getImageUuid())
                    .limit(1)
                    .listValues();

            if (resultList.isEmpty()) {
                resultList = Q.New(ImageCacheShadowVO.class)
                        .select(ImageCacheShadowVO_.size)
                        .eq(ImageCacheShadowVO_.imageUuid, spec.getImageUuid())
                        .limit(1)
                        .listValues();

                if (resultList.isEmpty()) {
                    throw new OperationFailureException(operr("no way to get image size of %s, report exception.", spec.getImageUuid()));
                }
            }
        }

        imageSize = resultList.get(0);

        for (PrimaryStorageVO vo : vos) {
            long requiredSize = ratioMgr.calculateByRatio(vo.getUuid(), spec.getSize());
            if (!hasImagePrimaryStorage.contains(vo.getUuid())) {
                requiredSize += imageSize;
            }

            if (vo.getCapacity().getAvailableCapacity() >= requiredSize) {
                res.add(vo);
            }
        }

        return res;
    }

    @Transactional(readOnly = true)
    private List<PrimaryStorageVO> considerImageBackupStorageRef(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> vos){
        List<BackupStorageVO> bsvos = SQL.New("select bs" +
                " from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                " where ref.imageUuid = :imgUuid" +
                " and bs.uuid = ref.backupStorageUuid", BackupStorageVO.class)
                .param("imgUuid", spec.getImageUuid())
                .list();

        List<String> relatedPsUuids = new ArrayList<>();
        for (BackupStorageVO bs : bsvos) {
            List<String> pss = BackupStorageType.valueOf(bs.getType()).findRelatedPrimaryStorage(bs.getUuid());
            if (pss != null && !pss.isEmpty()) {
                relatedPsUuids.addAll(pss);
            }
        }

        if (relatedPsUuids.isEmpty()) {
            return vos;
        } else {
            return vos.stream().filter(ps -> relatedPsUuids.contains(ps.getUuid())).collect(Collectors.toList());
        }
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Result ret = allocate(data);
        if (ret.result.isEmpty()) {
            throw new OperationFailureException(operr("%s", ret.error));
        }

        logger.debug(String.format("PrimaryStorageMainAllocatorFlow: %s", ret.result.size()));
        data.put(AllocatorParams.CANDIDATES, ret.result);
        trigger.next();
    }
}
