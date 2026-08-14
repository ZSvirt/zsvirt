package org.zstack.compute.affinityGroup;

import org.zstack.header.affinitygroup.AffinityGroupType;

public interface AffinityGroupManager {
    AffinityGroupRatingFactory getAffinityGroupRating(AffinityGroupType type);
    void deleteAffinityGroupUsage(String vmUuid);
    void addVmToAffinityGroupUsage (String agUuid, String vmUuid);
    void createVmSystemTagForAffinityGroup(String agUuid, String vmUuid);
}
