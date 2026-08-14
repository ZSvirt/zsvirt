package org.zstack.storage.ceph;

import org.zstack.header.tag.TagDefinition;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class CephMonSystemTags {
    public static String EXTRA_IPS_TOKEN = "extraips";
    public static PatternedSystemTag EXTRA_IPS =
            new PatternedSystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), CephPrimaryStorageMonVO.class);
}
