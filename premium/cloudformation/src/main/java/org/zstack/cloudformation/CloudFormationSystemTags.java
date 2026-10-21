package org.zstack.cloudformation;

import org.zstack.header.cloudformation.StackTemplateVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by mingjian.deng on 2018/6/27.
 */
@TagDefinition
public class CloudFormationSystemTags {
    public static final String SYSTEM_TEMPLATE_TOKEN = "systemtemplate";
    public static SystemTag SYSTEM_TEMPLATE = new SystemTag(
            SYSTEM_TEMPLATE_TOKEN, StackTemplateVO.class);

    public static final String CREATE_BY_CLOUDFORMATION_TOKEN = "cloudformation::autotag";
    public static SystemTag CREATE_BY_CLOUDFORMATION = new SystemTag(
            CREATE_BY_CLOUDFORMATION_TOKEN, StackTemplateVO.class);
}
