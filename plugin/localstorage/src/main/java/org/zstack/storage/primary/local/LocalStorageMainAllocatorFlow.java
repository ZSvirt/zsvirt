package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 * Created by frank on 7/1/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageMainAllocatorFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(LocalStorageMainAllocatorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    private class Result {
        List<PrimaryStorageVO> result;
        ErrorCode errorCode;
    }

    @Transactional(readOnly = true)
    private Result allocate(Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        TypedQuery<LocalStorageHostRefVO> query;
        Result ret = new Result();
        long reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());

        String primaryStorageCandidateSql = spec.getCandidatePrimaryStorageUuids().isEmpty() ? "" : String.format(" and pri.uuid in ('%s')",
                String.join("','", spec.getCandidatePrimaryStorageUuids()));
        if (spec.getRequiredHostUuid() != null) {
            String sql = "select lref" +
                    " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO pref, HostVO host, LocalStorageHostRefVO lref" +
                    " where pri.uuid = pref.primaryStorageUuid" +
                    " and lref.primaryStorageUuid = pri.uuid" +
                    " and host.uuid = :huuid" +
                    " and host.uuid = lref.hostUuid" +
                    " and host.clusterUuid = pref.clusterUuid" +
                    " and pri.state = :pstate" +
                    " and pri.status = :pstatus" +
                    primaryStorageCandidateSql +
                    " and host.state = :hstate" +
                    " and host.status = :hstatus" +
                    " and pri.type = :ptype";
            query = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
            query.setParameter("huuid", spec.getRequiredHostUuid());
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
            ret.errorCode = operr("the required host[uuid:%s] cannot satisfy conditions[state: %s, status: %s, size > %s bytes]," +
                            " or doesn't belong to a local primary storage satisfying conditions[state: %s, status: %s]," +
                            " or its cluster doesn't attach to any local primary storage",
                    spec.getRequiredHostUuid(),
                    HostState.Enabled,
                    HostStatus.Connected,
                    spec.getSize(),
                    PrimaryStorageState.Enabled,
                    PrimaryStorageStatus.Connected);
        } else if (spec.getRequiredZoneUuid() != null) {
            String sql = "select ref" +
                    " from PrimaryStorageVO pri, LocalStorageHostRefVO ref, HostVO host" +
                    " where pri.uuid = ref.primaryStorageUuid" +
                    " and pri.state = :pstate" +
                    " and pri.status = :pstatus" +
                    " and pri.zoneUuid = :zoneUuid" +
                    primaryStorageCandidateSql +
                    " and pri.type = :ptype" +
                    " and host.uuid = ref.hostUuid" +
                    " and host.state = :hstate" +
                    " and host.status = :hstatus";
            query = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
            ret.errorCode = operr("no local primary storage in zone[uuid:%s] can satisfy conditions[state: %s, status: %s]" +
                            " or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                    spec.getRequiredZoneUuid(),
                    PrimaryStorageState.Enabled,
                    PrimaryStorageStatus.Connected,
                    HostState.Enabled,
                    HostStatus.Connected,
                    spec.getSize());
        } else {
            String sql = "select ref" +
                    " from PrimaryStorageVO pri, LocalStorageHostRefVO ref, HostVO host" +
                    " where pri.uuid = ref.primaryStorageUuid" +
                    " and host.uuid = ref.hostUuid" +
                    " and pri.state = :pstate" +
                    " and pri.status = :pstatus" +
                    primaryStorageCandidateSql +
                    " and host.state = :hstate" +
                    " and host.status = :hstatus" +
                    " and pri.type = :ptype";
            query = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);

            ret.errorCode = operr("no local primary storage can satisfy conditions[state: %s, status: %s]" +
                            " or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                    PrimaryStorageState.Enabled,
                    PrimaryStorageStatus.Connected,
                    HostState.Enabled,
                    HostStatus.Connected,
                    spec.getSize());
        }

        List<LocalStorageHostRefVO> refs = query.getResultList();

        // if required host exists, filter the refs first
        if (spec.getRequiredHostUuid() != null) {
            logger.debug(String.format("host[uuid:%s] is request, only check its capacity", spec.getRequiredHostUuid()));
            refs = refs.stream()
                    .filter(ref -> ref.getHostUuid().equals(spec.getRequiredHostUuid()))
                    .collect(Collectors.toList());
        }

        // TODO: need code refactor
        List<LocalStorageHostRefVO> candidateHosts = new ArrayList<>();
        for (LocalStorageHostRefVO ref : refs) {
            if (spec.isNoOverProvisioning()) {
                if ((ref.getAvailableCapacity() - reservedCapacity) >= spec.getSize()) {
                    candidateHosts.add(ref);
                }
            } else {
                if (ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(ref.getPrimaryStorageUuid(),
                        ref.getAvailableCapacity()) - reservedCapacity >= spec.getSize()) {
                    candidateHosts.add(ref);
                }
            }
        }

        logger.debug(String.format("candidate hosts [%s]", candidateHosts.stream().map(LocalStorageHostRefVO::getHostUuid).collect(Collectors.toList())));
        if (!candidateHosts.isEmpty()) {
            Iterator<LocalStorageHostRefVO> it = candidateHosts.iterator();
            List<ErrorCode> errs = new ArrayList<>();
            while (it.hasNext()) {
                LocalStorageHostRefVO ref = it.next();
                if (!physicalCapacityMgr.checkCapacityByRatio(ref.getPrimaryStorageUuid(), ref.getTotalPhysicalCapacity(), ref.getAvailablePhysicalCapacity())
                        || !physicalCapacityMgr.checkRequiredCapacityByRatio(ref.getPrimaryStorageUuid(), ref.getTotalPhysicalCapacity(), spec.getTotalSize())) {
                    errs.add(operr("{the physical capacity usage of the host[uuid:%s] has exceeded the threshold[%s]}",
                            ref.getHostUuid(), physicalCapacityMgr.getRatio(ref.getPrimaryStorageUuid())));
                    it.remove();
                }
            }
            if (candidateHosts.isEmpty()) {
                ret.errorCode = operr("failed allocate localstorage").withCause(errs);
            }
        }
        Set<String> candidates = new HashSet<>();
        if (!candidateHosts.isEmpty()) {
            if (PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())) {
                candidates.addAll(considerImageCache(spec, candidateHosts));
            } else {
                candidates.addAll(candidateHosts.stream()
                        .map(LocalStorageHostRefVO::getPrimaryStorageUuid)
                        .collect(Collectors.toList())
                );
            }
        }

        logger.debug(String.format("after check image cache, primary storage candidates [%s]", candidates));
        List<PrimaryStorageVO> res;
        if (candidates.isEmpty()) {
            res = new ArrayList<>();
        } else {
            String sql = "select ps from PrimaryStorageVO ps where ps.uuid in (:psUuids)";
            TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            q.setParameter("psUuids", candidates);
            res = q.getResultList();
        }
        /**
         * 1. if ps is indicated, then choose it directly
         * 2. if ps is not indicated and exclude ps is indicated, then exclude it and choose others
         */
        if (spec.getPossiblePrimaryStorageTypes() != null && !spec.getPossiblePrimaryStorageTypes().isEmpty()) {
            Iterator<PrimaryStorageVO> it = res.iterator();
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
            Iterator<PrimaryStorageVO> it = res.iterator();
            while (it.hasNext()) {
                PrimaryStorageVO psvo = it.next();
                if (spec.getExcludePrimaryStorageTypes().contains(psvo.getType())) {
                    logger.debug(String.format("the primary storage[name:%s, uuid:%s, type:%s] is in exclude primary storage types[%s]," +
                            " remove it", psvo.getName(), psvo.getUuid(), psvo.getType(), spec.getExcludePrimaryStorageTypes()));
                    it.remove();
                }
            }
        }

        ret.result = res;
        return ret;
    }

    private Collection<? extends String> considerImageCache(PrimaryStorageAllocationSpec spec, List<LocalStorageHostRefVO> candidateHosts) {
        if (spec.getImageUuid() == null) {
            return candidateHosts.stream()
                    .map(LocalStorageHostRefVO::getPrimaryStorageUuid)
                    .collect(Collectors.toList());
        }

        List<String> ret = new ArrayList<>();

        String sql = "select i.actualSize from ImageVO i where i.uuid = :uuid";
        TypedQuery<Long> sq = dbf.getEntityManager().createQuery(sql, Long.class);
        sq.setParameter("uuid", spec.getImageUuid());
        long imageActualSize = sq.getSingleResult();

        for (LocalStorageHostRefVO ref : candidateHosts) {
            sql = "select count(i) from ImageCacheVO i where i.installUrl like :mark and i.primaryStorageUuid in (:psUuids)";
            TypedQuery<Long> iq = dbf.getEntityManager().createQuery(sql, Long.class);
            iq.setParameter("psUuids", ref.getPrimaryStorageUuid());
            iq.setParameter("mark", String.format("%%hostUuid://%s%%", ref.getHostUuid()));
            iq.setMaxResults(1);
            long count = iq.getSingleResult();

            if (count > 0) {
                // the host has the image in cache
                ret.add(ref.getPrimaryStorageUuid());
            } else {
                // the host doesn't has the image in cache
                // we need to add the image size;
                if (spec.isNoOverProvisioning()) {
                    if (ref.getAvailableCapacity() > spec.getSize() + imageActualSize) {
                        ret.add(ref.getPrimaryStorageUuid());
                    }
                } else {
                    if (ref.getAvailableCapacity() > ratioMgr.calculateByRatio(ref.getPrimaryStorageUuid(), spec.getSize()) + imageActualSize) {
                        ret.add(ref.getPrimaryStorageUuid());
                    }
                }
            }
        }

        return ret;
    }


    @Override
    public void run(FlowTrigger trigger, Map data) {
        Result ret = allocate(data);
        if (!ret.result.isEmpty()) {
            data.put(AllocatorParams.CANDIDATES, ret.result);
            trigger.next();
            return;
        }

        ErrorCode err = ret.errorCode;
        trigger.fail(err);
    }
}
