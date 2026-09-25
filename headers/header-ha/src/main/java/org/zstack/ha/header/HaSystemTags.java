package org.zstack.ha.header;

import org.zstack.ha.entity.VmHaVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

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
    public static SystemTag HA = new SystemTag(String.format("ha::{%s}", HA_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String INHIBIT_HA_TOKEN = "inhibitHA";
    /**
     * replace by table {@link VmHaVO} soon
     */
    @Deprecated
    public static SystemTag INHIBIT_HA = new SystemTag(String.format("%s", INHIBIT_HA_TOKEN), VmInstanceVO.class);

    public static String VM_FENCED_TOKEN = "fencer";
    public static SystemTag VM_FENCED_BY =
            new SystemTag(String.format("vmFencedBy::{%s}", VM_FENCED_TOKEN), VmInstanceVO.class);
}
