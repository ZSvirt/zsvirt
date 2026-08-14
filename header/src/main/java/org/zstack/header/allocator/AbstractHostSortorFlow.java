package org.zstack.header.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.host.HostInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractHostSortorFlow {
    protected List<HostInventory> candidates = new ArrayList<>();
    protected HostAllocatorSpec spec;
    protected List<HostInventory> subCandidates = new ArrayList<>();

    public List<HostInventory> getCandidates() {
        return candidates;
    }

    public List<HostInventory> getSubCandidates() {
        return subCandidates;
    }

    public void setSubCandidates(List<HostInventory> subCandidates) {
        this.subCandidates = subCandidates;
    }

    public void setCandidates(List<HostInventory> candidates) {
        this.candidates = candidates;
    }

    public abstract void sort();

    public abstract boolean skipNext();

    public HostAllocatorSpec getSpec() {
        return spec;
    }

    public void setSpec(HostAllocatorSpec spec) {
        this.spec = spec;
    }
}
