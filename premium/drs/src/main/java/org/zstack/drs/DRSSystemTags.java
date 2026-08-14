package org.zstack.drs;

import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by lining on 2019/12/12.
 */
@TagDefinition
public class DRSSystemTags {
    public static final String HOST_LOAD_OVER_THRESHOLD_TOKEN = "hostLoadOverThreshold";
    public static PatternedSystemTag HOST_LOAD_OVER_THRESHOLD = new PatternedSystemTag(
            String.format("hostLoadOverThreshold::{%s}", HOST_LOAD_OVER_THRESHOLD_TOKEN),
            ClusterDRSVO.class
    );
}
