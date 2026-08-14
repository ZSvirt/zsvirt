package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/**
 * Created by mingjian.deng on 2017/11/8.
 * LastHostPreferredSortFlow will sort last host as first
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LastHostPreferredSortFlow extends AbstractHostSortorFlow {
    private boolean skip = true;

    @Override
    public void sort() {
        final VmInstanceInventory vm = spec.getVmInstance();
        Optional<HostInventory> hosts = candidates.stream().filter(candidate -> candidate.getUuid().equals(vm.getLastHostUuid())).findAny();
        List<HostInventory> sorted = new ArrayList<>();

        if (hosts.isPresent()) {
            sorted.add(hosts.get());

            candidates.remove(candidates.indexOf(hosts.get()));
            sorted.addAll(candidates);
            candidates.clear();
            candidates.addAll(sorted);
        } else {
            // if last host never existed, we go to the next sort flow
            skip = false;
            subCandidates.addAll(candidates);
        }
    }

    @Override
    public boolean skipNext() {
        return skip;
    }
}
