package org.zstack.header.vmscheduling;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
@TagDefinition
public class VmSchedulingRuleSystemTags {
    public static String VM_SCHEDULING_RULE_GROUP_UUID_TOKEN = "vmSchedulingRuleGroupUuid";
    public static SystemTag VM_SCHEDULING_RULE_GROUP_UUID = new SystemTag(String.format("vmSchedulingRuleGroupUuid::{%s}", VM_SCHEDULING_RULE_GROUP_UUID_TOKEN), VmInstanceVO.class);

}
