package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmLocationSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AvoidHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        final Set<String> avoidHostUuids = avoidHostUuids();
        final Set<String> onlyAllowedHostUuids = onlyAllowedHostUuids();
        for (HostCandidate candidate : candidates) {
            if (avoidHostUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("in avoid host list"));
            } else if (onlyAllowedHostUuids != null && !onlyAllowedHostUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("not in only-allowed host list"));
            }
        }

        next();
    }

    private Set<String> avoidHostUuids() {
        if (isEmpty(spec.getLocationSpecs())) {
            return new HashSet<>();
        }

        return spec.getLocationSpecs().stream()
                .filter(spec -> HostVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::avoid)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
    }

    private Set<String> onlyAllowedHostUuids() {
        if (isEmpty(spec.getLocationSpecs())) {
            return null;
        }

        List<Set<String>> sets = spec.getLocationSpecs().stream()
                .filter(locationSpec -> HostVO.class.getSimpleName().equals(locationSpec.getResourceType()))
                .filter(VmLocationSpec::onlyAllowed)
                .map(locationSpec -> new HashSet<>(locationSpec.getUuids()))
                .collect(Collectors.toList());
        if (sets.isEmpty()) {
            return null;
        }

        Set<String> allowed = sets.get(0);
        for (int i = 1; i < sets.size(); i++) {
            allowed.retainAll(sets.get(i));
        }
        return allowed;
    }
}
