package org.zstack.compute.sriov;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AdminOnlyTag;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by GuoYi on 12/9/19.
 */
@TagDefinition
public class SriovSystemTags {
    @AdminOnlyTag
    public static PatternedSystemTag L2_ENABLE_SRIOV = new PatternedSystemTag("enableSRIOV", L2NetworkVO.class);
}
