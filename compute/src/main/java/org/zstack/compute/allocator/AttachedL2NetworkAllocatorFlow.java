package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.Utils;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorFlow implements HostCandidateProducer {
    private static final CLogger logger = Utils.getLogger(AttachedL2NetworkAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Transactional(readOnly = true)
    private List<HostVO> allocate(Collection<String> l3NetworkUuids, HostCandidateProducerContext context) {
        String sql = "select l3.l2NetworkUuid from L3NetworkVO l3 where l3.uuid in (:l3uuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("l3uuids", l3NetworkUuids);
        List<String> l2uuids = q.getResultList();
        if (l2uuids.isEmpty()) {
            return new ArrayList<>();
        }

        sql = "select ref from L2NetworkClusterRefVO ref where ref.l2NetworkUuid in (:l2uuids)";
        TypedQuery<L2NetworkClusterRefVO> rq = dbf.getEntityManager().createQuery(sql, L2NetworkClusterRefVO.class);
        rq.setParameter("l2uuids", l2uuids);
        List<L2NetworkClusterRefVO> refs = rq.getResultList();
        if (refs.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Set<String>> l2ClusterMap = new HashMap<>();
        for (L2NetworkClusterRefVO ref : refs) {
            Set<String> l2s = l2ClusterMap.computeIfAbsent(ref.getClusterUuid(), k -> new HashSet<>());
            l2s.add(ref.getL2NetworkUuid());
        }

        Set<String> clusterUuids = new HashSet<>();
        for (Map.Entry<String, Set<String>> e : l2ClusterMap.entrySet()) {
            if (e.getValue().containsAll(l2uuids)) {
                clusterUuids.add(e.getKey());
            }
        }

        if (clusterUuids.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> retHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .listValues();

        if (retHostUuids.isEmpty()){
            return new ArrayList<>();
        }

        /* in normal case, there is no L2NetworkHostRefVO  */
        List<String> excludeHostUuids = L2NetworkHostUtils.getExcludeHostUuids(l2uuids, retHostUuids);
        retHostUuids.removeAll(excludeHostUuids);
        if (retHostUuids.isEmpty()){
            return new ArrayList<>();
        }

        sql = "select h from HostVO h where h.uuid in (:huuids)";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("huuids", retHostUuids);

        if (context.usePagination()) {
            hq.setFirstResult(context.paginationInfo.getOffset());
            hq.setMaxResults(context.paginationInfo.getLimit());
        }

        return hq.getResultList();
    }

    @Override
    public void produce(HostCandidateProducerContext context) {
        final HostAllocatorSpec spec = context.spec;

        if (spec.getL3NetworkUuids().isEmpty()) {
            if (spec.isAllowNoL3Networks()) {
                return;
            }

            spec.setAllowNoL3Networks(true);
            String sql;
            Set<String> clusterUuids = new HashSet<>();
            clusterUuids.add(spec.getVmInstance().getClusterUuid());

            sql = "select h from HostVO h where h.clusterUuid in (:cuuids)";
            TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
            hq.setParameter("cuuids", clusterUuids);
            if (context.usePagination()) {
                hq.setFirstResult(context.paginationInfo.getOffset());
                hq.setMaxResults(context.paginationInfo.getLimit());
            }
            context.accept(hq.getResultList());
            return;
        }

        List<String> l3Uuids = spec.getL3NetworkUuids();
        List<L3NetworkInventory> serviceL3s = new ArrayList<>();
        for (GetL3NetworkForVmNetworkService extp : pluginRgty.getExtensionList(GetL3NetworkForVmNetworkService.class)) {
            serviceL3s.addAll(extp.getL3NetworkForVmNetworkService(spec.getVmInstance()));
        }
        if (!serviceL3s.isEmpty()) {
            l3Uuids.addAll(serviceL3s.stream().map(L3NetworkInventory::getUuid).distinct().collect(Collectors.toList()));
        }

        final List<HostVO> newcomers = allocate(l3Uuids, context);
        if (newcomers.isEmpty()) {
            logger.warn(String.format("no host found in clusters that has attached to L2Networks which have L3Networks%s", spec.getL3NetworkUuids()));
        }

        context.accept(newcomers);
    }
}
