package org.zstack.ipsec;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by shixin.ruan on 2021/04/01
 */
@TagDefinition
public class IpSecconnectionSystemTags {
    public static String IPSEC_PEER_REMOTE_ID_TOKEN = "remoteId";
    public static PatternedSystemTag IPSEC_PEER_REMOTE_ID =
            new PatternedSystemTag(String.format(
                    "remoteId::{%s}", IPSEC_PEER_REMOTE_ID_TOKEN),
                    IPsecConnectionVO.class);

    public static PatternedSystemTag IPSEC_LOW_VERSION = new PatternedSystemTag("vpcLowVersion", IPsecConnectionVO.class);
}
