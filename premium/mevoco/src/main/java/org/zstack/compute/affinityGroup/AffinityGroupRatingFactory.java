package org.zstack.compute.affinityGroup;

import org.zstack.header.affinitygroup.AffinityGroupType;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostInventory;

import java.util.List;
import java.util.Map;

public interface AffinityGroupRatingFactory {
    final class AffinityGroupRatingStruct{
        List<HostInventory> candidates;
        HostAllocatorSpec spec;
        String affinityGroupUuid;

        public String getAffinityGroupUuid() {
            return affinityGroupUuid;
        }

        public void setAffinityGroupUuid(String affinityGroupUuid) {
            this.affinityGroupUuid = affinityGroupUuid;
        }

        public List<HostInventory> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<HostInventory> candidates) {
            this.candidates = candidates;
        }

        public HostAllocatorSpec getSpec() {
            return spec;
        }

        public void setSpec(HostAllocatorSpec spec) {
            this.spec = spec;
        }
    }

    AffinityGroupType getAffinityGroupType();
    /* return a map with the rate of each host */
    Map<String, Long> ratingHostCandidates(AffinityGroupRatingStruct struct);
}
