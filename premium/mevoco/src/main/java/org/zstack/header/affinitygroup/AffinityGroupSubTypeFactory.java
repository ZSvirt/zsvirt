package org.zstack.header.affinitygroup;

public interface AffinityGroupSubTypeFactory {
    AffinityGroupSubType getAffinityGroupSubType();

    AffinityGroupVO persistAffinityGroup(CreateAffinityGroupMsg msg, AffinityGroupVO vo);

    AffinityGroupVO persistAffinityGroup(APICreateAffinityGroupMsg msg, AffinityGroupVO vo);
}
