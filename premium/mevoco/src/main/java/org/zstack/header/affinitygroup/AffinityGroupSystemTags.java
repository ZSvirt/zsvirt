package org.zstack.header.affinitygroup;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;


@TagDefinition
public class AffinityGroupSystemTags {
    public static String AFFINITY_GROUP_UUID_TOKEN = "affinityGroupUuid";
    public static PatternedSystemTag AFFINITY_GROUP_UUID = new PatternedSystemTag(String.format("affinityGroupUuid::{%s}", AFFINITY_GROUP_UUID_TOKEN), VmInstanceVO.class);
}
