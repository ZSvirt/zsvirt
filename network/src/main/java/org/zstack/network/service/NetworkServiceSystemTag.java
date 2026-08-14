package org.zstack.network.service;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by weiwang on 17/05/2017.
 */
@TagDefinition
public class NetworkServiceSystemTag {

    public static final String L3_UUID_TOKEN = "l3Uuid";
    public static final String MTU_TOKEN = "mtu";
    public static PatternedSystemTag L3_MTU = new PatternedSystemTag(
            String.format("l3NetworkUuid::{%s}::mtu::{%s}",
                    L3_UUID_TOKEN, MTU_TOKEN), L3NetworkVO.class
    );
}