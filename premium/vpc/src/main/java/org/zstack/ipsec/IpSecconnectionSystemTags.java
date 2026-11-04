package org.zstack.ipsec;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by shixin.ruan on 2021/04/01
 */
@TagDefinition
public class IpSecconnectionSystemTags {
    public static String IPSEC_PEER_REMOTE_ID_TOKEN = "remoteId";
    public static SystemTag IPSEC_PEER_REMOTE_ID =
            new SystemTag(String.format(
                    "remoteId::{%s}", IPSEC_PEER_REMOTE_ID_TOKEN),
                    IPsecConnectionVO.class);

    public static SystemTag IPSEC_LOW_VERSION = new SystemTag("vpcLowVersion", IPsecConnectionVO.class);
}
