package org.zstack.ha;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/3/28.
 */
@TagDefinition
public class HaSystemTags {
    @Deprecated
    public static String HA_TOKEN = "ha";
    /**
     * replace by table {@link VmHaVO} soon
     */
    @Deprecated
    public static PatternedSystemTag HA = new PatternedSystemTag(String.format("ha::{%s}", HA_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String INHIBIT_HA_TOKEN = "inhibitHA";
    /**
     * replace by table {@link VmHaVO} soon
     */
    @Deprecated
    public static PatternedSystemTag INHIBIT_HA = new PatternedSystemTag(String.format("%s", INHIBIT_HA_TOKEN), VmInstanceVO.class);

    public static String VM_FENCED_TOKEN = "fencer";
    public static PatternedSystemTag VM_FENCED_BY =
            new PatternedSystemTag(String.format("vmFencedBy::{%s}", VM_FENCED_TOKEN), VmInstanceVO.class);
}
