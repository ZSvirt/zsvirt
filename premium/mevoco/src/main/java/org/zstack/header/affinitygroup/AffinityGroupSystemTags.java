package org.zstack.header.affinitygroup;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;


@TagDefinition
public class AffinityGroupSystemTags {
    public static String AFFINITY_GROUP_UUID_TOKEN = "affinityGroupUuid";
    public static SystemTag AFFINITY_GROUP_UUID = new SystemTag(String.format("affinityGroupUuid::{%s}", AFFINITY_GROUP_UUID_TOKEN), VmInstanceVO.class);
}
