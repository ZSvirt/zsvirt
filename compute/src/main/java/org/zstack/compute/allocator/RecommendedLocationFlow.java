package org.zstack.compute.allocator;

import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmLocationSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/12/17
 */
public class RecommendedLocationFlow extends AbstractHostAllocatorFlow {
    private final CLogger logger = Utils.getLogger(RecommendedLocationFlow.class);

    @Override
    public void allocate() {
        if (spec.isListAllHosts()) {
            next();
            return;
        }

        mark();

        // handle not recommended hosts
        List<HostCandidate> from = new ArrayList<>(candidates);
        List<HostCandidate> to = filter(from, h -> h.notRecommendBy != null);
        to = to.isEmpty() ? filter(from, h -> h.notRecommendBy != null && h.recommendBy == null) : to;

        if (to.isEmpty()) {
            to = from;
        } else {
            for (HostCandidate candidate : from) {
                if (!to.contains(candidate)) {
                    reject(candidate, i18m("not recommended by %s", candidate.notRecommendBy));
                }
            }
        }

        // handle recommend hosts (recommendBy list size is number of votes)
        from = to;
        to = filter(from, h -> h.recommendBy != null);
        if (to.isEmpty()) {
            next();
            return;
        }

        int maxVote = to.stream()
                .map(h -> h.recommendBy.size())
                .max(Integer::compareTo)
                .orElse(0);

        for (HostCandidate candidate : from) {
            if (candidate.recommendBy == null || candidate.recommendBy.size() < maxVote) {
                reject(candidate, i18m("another host is recommended"));
            }
        }

        next();
    }

    private void mark() {
        markRecommendedHosts(candidates);
        markNotRecommendedHosts(candidates);
        markRecommendedClusters(candidates);
        markNotRecommendedClusters(candidates);
    }

    private void markRecommendedHosts(List<HostCandidate> list) {
        final Set<String> uuidSet = spec.getLocationSpecs().stream()
                .filter(spec -> HostVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::recommended)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
        for (String uuid : uuidSet) {
            HostCandidate host = findOneOrNull(list, h -> h.getUuid().equals(uuid));
            if (host != null) {
                host.markAsRecommended(getClass().getSimpleName() + "-recommendedHosts");
            }
        }
    }

    private void markNotRecommendedHosts(List<HostCandidate> list) {
        final Set<String> uuidSet = spec.getLocationSpecs().stream()
                .filter(spec -> HostVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::notRecommended)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
        if (isEmpty(uuidSet)) {
            return;
        }

        for (HostCandidate host : list) {
            if (!uuidSet.contains(host.getUuid())) {
                host.markAsNotRecommended(getClass().getSimpleName() + "-notRecommendedHosts");
            }
        }
    }

    private void markRecommendedClusters(List<HostCandidate> list) {
        final Set<String> uuidSet = spec.getLocationSpecs().stream()
                .filter(spec -> ClusterVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::recommended)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
        for (String uuid : uuidSet) {
            filter(list, h -> h.host.getClusterUuid().equals(uuid))
                    .forEach(candidate -> candidate.markAsRecommended(getClass().getSimpleName() + "-recommendedClusters"));
        }
    }

    private void markNotRecommendedClusters(List<HostCandidate> list) {
        final Set<String> uuidSet = spec.getLocationSpecs().stream()
                .filter(spec -> ClusterVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::notRecommended)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
        if (isEmpty(uuidSet)) {
            return;
        }

        for (HostCandidate candidate : list) {
            if (!uuidSet.contains(candidate.host.getClusterUuid())) {
                candidate.markAsNotRecommended(getClass().getSimpleName() + "-notRecommendedClusters");
            }
        }
    }
}
