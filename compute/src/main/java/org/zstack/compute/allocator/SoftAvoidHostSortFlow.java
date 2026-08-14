package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmLocationSpec;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SoftAvoidHostSortFlow extends AbstractHostSortorFlow {
    @Override
    public void sort() {
        subCandidates.clear();
        subCandidates.addAll(candidates);

        Set<String> softAvoidHosts = notRecommendedHostUuids();
        subCandidates.removeIf(inv -> softAvoidHosts.contains(inv.getUuid()));
    }

    private Set<String> notRecommendedHostUuids() {
        if (isEmpty(spec.getLocationSpecs())) {
            return new HashSet<>();
        }

        return spec.getLocationSpecs().stream()
                .filter(spec -> HostVO.class.getSimpleName().equals(spec.getResourceType()))
                .filter(VmLocationSpec::notRecommended)
                .flatMap(spec -> spec.getUuids().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean skipNext() {
        return false;
    }
}
