package org.zstack.header.vmscheduling;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
@TagDefinition
public class VmSchedulingRuleSystemTags {
    public static String VM_SCHEDULING_RULE_GROUP_UUID_TOKEN = "vmSchedulingRuleGroupUuid";
    public static PatternedSystemTag VM_SCHEDULING_RULE_GROUP_UUID = new PatternedSystemTag(String.format("vmSchedulingRuleGroupUuid::{%s}", VM_SCHEDULING_RULE_GROUP_UUID_TOKEN), VmInstanceVO.class);

}
