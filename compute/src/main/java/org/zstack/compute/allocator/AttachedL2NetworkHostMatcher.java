package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.network.l2.L2NetworkHostUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
class AttachedL2NetworkHostMatcher {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    List<String> collectL3NetworkUuids(HostAllocatorSpec spec) {
        List<String> l3Uuids = spec.getL3NetworkUuids();
        List<L3NetworkInventory> serviceL3s = new ArrayList<>();
        for (GetL3NetworkForVmNetworkService extp : pluginRgty.getExtensionList(GetL3NetworkForVmNetworkService.class)) {
            serviceL3s.addAll(extp.getL3NetworkForVmNetworkService(spec.getVmInstance()));
        }
        if (!serviceL3s.isEmpty()) {
            for (String uuid : serviceL3s.stream().map(L3NetworkInventory::getUuid).distinct().collect(Collectors.toList())) {
                if (!l3Uuids.contains(uuid)) {
                    l3Uuids.add(uuid);
                }
            }
        }
        return l3Uuids;
    }

    @Transactional(readOnly = true)
    Set<String> listAllowedHostUuids(Collection<String> l3NetworkUuids) {
        if (l3NetworkUuids == null || l3NetworkUuids.isEmpty()) {
            return new HashSet<>();
        }

        List<String> l2uuids = Q.New(L3NetworkVO.class)
                .in(L3NetworkVO_.uuid, l3NetworkUuids)
                .select(L3NetworkVO_.l2NetworkUuid)
                .listValues();
        if (l2uuids.isEmpty()) {
            return new HashSet<>();
        }

        List<L2NetworkClusterRefVO> refs = Q.New(L2NetworkClusterRefVO.class)
                .in(L2NetworkClusterRefVO_.l2NetworkUuid, l2uuids)
                .list();
        if (refs.isEmpty()) {
            return new HashSet<>();
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
            return new HashSet<>();
        }

        List<String> retHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .listValues();
        if (retHostUuids == null || retHostUuids.isEmpty()) {
            return new HashSet<>();
        }

        List<String> excludeHostUuids = L2NetworkHostUtils.getExcludeHostUuids(l2uuids, retHostUuids);
        retHostUuids.removeAll(excludeHostUuids);
        return new HashSet<>(retHostUuids);
    }
}
