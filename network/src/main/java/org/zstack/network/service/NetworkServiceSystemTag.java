package org.zstack.network.service;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by weiwang on 17/05/2017.
 */
@TagDefinition
public class NetworkServiceSystemTag {

    public static final String L3_UUID_TOKEN = "l3Uuid";
    public static final String MTU_TOKEN = "mtu";
    public static SystemTag L3_MTU = new SystemTag(
            String.format("l3NetworkUuid::{%s}::mtu::{%s}",
                    L3_UUID_TOKEN, MTU_TOKEN), L3NetworkVO.class
    );
}