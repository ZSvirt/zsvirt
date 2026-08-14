package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    private final static CLogger logger = Utils.getLogger(HostPrimaryStorageAllocatorFlow.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    private List<HostVO> filterHostHavingAccessiblePrimaryStorage(List<String> huuids, HostAllocatorSpec spec){
        String vmOperation = spec.getVmOperation();
        Set<String> requiredPsUuids = spec.getRequiredPrimaryStorageUuids();
        List<Set<String>> optionalPrimaryStorageUuidList = spec.getOptionalPrimaryStorageUuids();
        StringBuilder sqlappend = new StringBuilder();
        if (!requiredPsUuids.isEmpty()) {
            sqlappend.append(requiredPsUuids.size() == 1 ?
                    String.format(" and ps.uuid = '%s'", requiredPsUuids.iterator().next()) :
                    String.format(" and ps.uuid in ('%s')" +
                                    " group by ref.clusterUuid" +
                                    " having count(distinct ref.primaryStorageUuid) = %d",
                            String.join("','", requiredPsUuids), requiredPsUuids.size()));
        }

        List<String> clusterUuids = null;
        for (Set<String> optionalPrimaryStorageUuids : optionalPrimaryStorageUuidList) {
            if (CollectionUtils.isEmpty(optionalPrimaryStorageUuids)) {
                continue;
            }
            List<String> tmpClusterUuids = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, optionalPrimaryStorageUuids)
                    .listValues();
            clusterUuids = clusterUuids != null ? clusterUuids.stream().filter(tmpClusterUuids::contains).collect(Collectors.toList())
                    : tmpClusterUuids;
            if (clusterUuids.isEmpty()) {
                return Collections.emptyList();
            }
        }
        if (clusterUuids != null) {
            sqlappend.append(String.format(" and ref.clusterUuid in ('%s')", String.join("','", clusterUuids)));
        }

        String psStatus = "";
        if (!VmOperation.NewCreate.toString().equals(vmOperation)) {
            psStatus = " and (ps.state = :state or ps.state =:state1)";
        } else {
            psStatus = " and ps.state = :state";
        }

        String sql = "select h" +
                " from HostVO h" +
                " where h.uuid in :uuids" +
                " and h.clusterUuid in" +
                " (" +
                " select ref.clusterUuid" +
                " from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps" +
                " where ref.primaryStorageUuid = ps.uuid" +
                " and ps.status = :status" +
                psStatus +
                sqlappend +
                " )";

        TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
        query.setParameter("uuids", huuids);
        query.setParameter("state", PrimaryStorageState.Enabled);
        query.setParameter("status", PrimaryStorageStatus.Connected);
        if (!VmOperation.NewCreate.toString().equals(vmOperation)) {
            query.setParameter("state1", PrimaryStorageState.Disabled);
        }
        List<HostVO> hosts = query.getResultList();

        // in case no host is connected
        if (hosts.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> hostUuids = hosts.stream().map(HostVO::getUuid).collect(Collectors.toList());
        List<String> disconnectHostUuids = requiredPsUuids.isEmpty() ? Collections.emptyList() :
                Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.hostUuid)
                        .in(PrimaryStorageHostRefVO_.primaryStorageUuid, requiredPsUuids)
                        .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                        .in(PrimaryStorageHostRefVO_.hostUuid, hostUuids)
                        .listValues();

        if (!disconnectHostUuids.isEmpty()) {
            logger.trace(String.format("There are some disconnection between primary storage[uuids:%s]" +
                    " and host[uuids:%s], remove these hosts", requiredPsUuids, disconnectHostUuids));
            Set<String> discHostSet = new HashSet<>(disconnectHostUuids);
            hosts.removeIf(it -> discHostSet.contains(it.getUuid()));
            hostUuids = hosts.stream().map(HostVO::getUuid).collect(Collectors.toList());
        }

        List<String> connectedHostUuids = null;
        for (Set<String> optionalPrimaryStorageUuids : optionalPrimaryStorageUuidList) {
            if (CollectionUtils.isEmpty(optionalPrimaryStorageUuids)) {
                continue;
            }

            List<String> tmpHostUuids =  Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.hostUuid)
                    .in(PrimaryStorageHostRefVO_.primaryStorageUuid, optionalPrimaryStorageUuids)
                    .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .in(PrimaryStorageHostRefVO_.hostUuid, hostUuids)
                    .listValues();
            connectedHostUuids = connectedHostUuids == null ? tmpHostUuids : connectedHostUuids.stream().filter(tmpHostUuids::contains)
                    .collect(Collectors.toList());
            if (connectedHostUuids.isEmpty()) {
                return Collections.emptyList();
            }
        }
        if (connectedHostUuids != null) {
            List<String> finalConnectedHostUuids = connectedHostUuids;
            hosts = hosts.stream().filter(h -> finalConnectedHostUuids.contains(h.getUuid())).collect(Collectors.toList());
        }
        return hosts;
    }

    @Transactional(readOnly = true)
    private void allocateFromCandidates() {
        List<String> huuids = allHostUuidList();
        Set<String> requiredPsUuids = spec.getRequiredPrimaryStorageUuids();

        List<HostVO> hosts = filterHostHavingAccessiblePrimaryStorage(huuids, spec);
        Set<String> uuidSet = transformToSet(hosts, HostVO::getUuid);
        for (HostCandidate candidate : candidates) {
            if (!uuidSet.contains(candidate.host.getUuid())) {
                reject(candidate, i18m("not accessible to the specific primary storage"));
            }
        }

        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            return;
        }

        huuids = transform(filterHostHavingAccessiblePrimaryStorage(huuids, spec), HostVO::getUuid);
        if (huuids.isEmpty()) {
            rejectAll(i18m("not accessible to the specific primary storage"));
            return;
        }

        // for new created vm
        String sql = "select ps.uuid" +
                " from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps, HostVO h" +
                " where ref.primaryStorageUuid = ps.uuid" +
                " and ps.state = :state" +
                " and ps.status = :status" +
                " and ref.clusterUuid = h.clusterUuid" +
                " and h.uuid in (:huuids)";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("state", PrimaryStorageState.Enabled);
        q.setParameter("status", PrimaryStorageStatus.Connected);
        q.setParameter("huuids", huuids);
        List<String> psUuids = q.getResultList();
        if (psUuids.isEmpty()) {
            rejectAll(i18m("no primary storage available for new-created VM"));
            return;
        }

        if (!requiredPsUuids.isEmpty()) {
            if (psUuids.containsAll(requiredPsUuids)) {
                psUuids.clear();
                psUuids.addAll(requiredPsUuids);
                huuids = SQL.New("select h.uuid from HostVO h" +
                        " where h.uuid in :huuids" +
                        " and h.uuid not in (" +
                        " select ref.hostUuid from PrimaryStorageHostRefVO ref" +
                        " where ref.primaryStorageUuid in :psUuids" +
                        " and ref.status != :phStatus" +
                        " )", String.class)
                        .param("huuids", huuids)
                        .param("psUuids", requiredPsUuids)
                        .param("phStatus", PrimaryStorageHostStatus.Connected)
                        .list();
                if (huuids.isEmpty()) {
                    rejectAll(i18m("primary storage %s required for new-created VM", requiredPsUuids));
                    return;
                }
            } else {
                rejectAll(i18m("primary storage %s required for new-created VM", requiredPsUuids));
                return;
            }
        }

        String sqlappend = psUuids.size() == 1 ?
                String.format(" and ref.primaryStorageUuid = '%s'", psUuids.get(0)) :
                String.format(" and ref.primaryStorageUuid in ('%s')", String.join("','", psUuids));

        sql = "select h.uuid, ref.primaryStorageUuid, cap.availableCapacity" +
                " from PrimaryStorageClusterRefVO ref, HostVO h, PrimaryStorageCapacityVO cap" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and cap.uuid = ref.primaryStorageUuid" +
                " and h.uuid in (:hostUuids)" +
                sqlappend;
        TypedQuery<Tuple> hostCluster = dbf.getEntityManager().createQuery(sql, Tuple.class);
        hostCluster.setParameter("hostUuids", huuids);
        List<Tuple> result = hostCluster.getResultList();
        Map<String, Long> psCap = new HashMap<>();           // psUuid -> available capacities (with ratio).
        Map<String, Long> availableHostPs = new HashMap<>(); // hostUuid -> available capacities (with ratio).
        Map<String, String> hostPsDict = new HashMap<>();    // host -> ps mapping
        for (Tuple t : result) {
            String hostUuid = t.get(0, String.class);
            String psUuid = t.get(1, String.class);
            Long psAvaCap = t.get(2, Long.class);

            if (!psCap.containsKey(psUuid)) {
                psCap.put(psUuid, ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, psAvaCap));
            }

            Long tmpCap = availableHostPs.get(hostUuid);
            if (tmpCap == null) {
                availableHostPs.put(hostUuid, psCap.get(psUuid));
                hostPsDict.put(hostUuid, psUuid);
            } else {
                availableHostPs.put(hostUuid, tmpCap + psCap.get(psUuid));
            }
        }

        Set<String> hostCandidates = new HashSet<>();
        Map<String, Boolean> psImageCacheDict = new HashMap<>();
        for (String hostUuid: availableHostPs.keySet()) {
            if (hasCapablePS(hostPsDict.get(hostUuid), availableHostPs.get(hostUuid), psImageCacheDict)) {
                hostCandidates.add(hostUuid);
            }
        }

        if (hostCandidates.isEmpty()) {
            rejectAll(i18m("no capable primary storage for new-created VM"));
            return;
        }

        for (HostCandidate candidate : candidates) {
            if (!hostCandidates.contains(candidate.getUuid())) {
                if (spec.getVmOperation().equals(VmOperation.NewCreate.toString())) {
                    reject(candidate, i18m(
                            "required primary storage[state: %s, status: %s, available capacity %s bytes]",
                            PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getDiskSize()));
                } else {
                    reject(candidate, i18m(
                            "required primary storage[state: %s or %s, status: %s]",
                            PrimaryStorageState.Enabled, PrimaryStorageState.Disabled, PrimaryStorageStatus.Connected));
                }
            }
        }
    }

    private Boolean psHasImageCache(String psUuid, Map<String, Boolean> psImageCacheDict) {
        Boolean b = psImageCacheDict.get(psUuid);
        if (b != null) {
            return b;
        }

        b = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, psUuid)
                .eq(ImageCacheVO_.imageUuid, spec.getImage().getUuid())
                .isExists();
        psImageCacheDict.put(psUuid, b);
        return b;
    }

    private boolean hasCapablePS(String psUuid, Long cap, Map<String, Boolean> psImageCacheDict) {
        // background: http://dev.zstack.io/browse/ZSTAC-4852
        // only fix problem one
        long requiredSize = spec.getDiskSize();

        if (spec.getImage() != null) {
            if (!psHasImageCache(psUuid, psImageCacheDict)) {
                // the primary storage doesn't have the image in cache
                // so we need to add the image size
                requiredSize +=  spec.getImage().getActualSize();
            }
        }

        // if multi ps, don't consider image cache, because of cannot determine the primary storage associated with the root disk
        return cap >= requiredSize;
    }

    @Override
    public void allocate() {
        allocateFromCandidates();
        next();
    }
}
