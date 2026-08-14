package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RandomSortFlow extends AbstractHostSortorFlow {
    private boolean skip = true;

    @Override
    public void sort() {
        Collections.shuffle(candidates);
        prepareForNext(candidates);
    }

    private void prepareForNext(List<HostInventory> hosts) {
        if (hosts != null && hosts.size() > 0) {
            subCandidates.clear();
            subCandidates.addAll(hosts);
            skip = false;
        }
    }

    @Override
    public boolean skipNext() {
        return skip;
    }
}
