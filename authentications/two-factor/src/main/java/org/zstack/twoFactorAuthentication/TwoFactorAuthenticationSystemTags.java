package org.zstack.twoFactorAuthentication;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class TwoFactorAuthenticationSystemTags {
    public static String TWOFA_TOKEN_TOKEN = "twofatoken";
    public static SystemTag TWOFA_TOKEN = new SystemTag(String.format("twofatoken::{%s}", TWOFA_TOKEN_TOKEN), AccountVO.class);
}
