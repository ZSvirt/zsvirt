package org.zstack.cloudformation;

import org.zstack.header.cloudformation.StackTemplateVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by mingjian.deng on 2018/6/27.
 */
@TagDefinition
public class CloudFormationSystemTags {
    public static final String SYSTEM_TEMPLATE_TOKEN = "systemtemplate";
    public static PatternedSystemTag SYSTEM_TEMPLATE = new PatternedSystemTag(
            SYSTEM_TEMPLATE_TOKEN, StackTemplateVO.class);

    public static final String CREATE_BY_CLOUDFORMATION_TOKEN = "cloudformation::autotag";
    public static PatternedSystemTag CREATE_BY_CLOUDFORMATION = new PatternedSystemTag(
            CREATE_BY_CLOUDFORMATION_TOKEN, StackTemplateVO.class);
}
