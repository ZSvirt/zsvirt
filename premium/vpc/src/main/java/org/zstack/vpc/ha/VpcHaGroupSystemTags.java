package org.zstack.vpc.ha;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vpc.ha.VpcHaGroupVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by shixin on 05/10/2019
 */
@TagDefinition
public class VpcHaGroupSystemTags {
    public static String VPCHA_ROUTER_AFFINITYGROUP_TOKEN = "vpcHaAffinityGroup";
    public static PatternedSystemTag VPCHA_ROUTER_AFFINITYGROUP =
            new PatternedSystemTag(String.format(
                    "vpcHaAffinityGroup::{%s}", VPCHA_ROUTER_AFFINITYGROUP_TOKEN), VpcHaGroupVO.class);
}
