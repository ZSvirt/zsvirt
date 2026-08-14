package org.zstack.twoFactorAuthentication;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class TwoFactorAuthenticationSystemTags {
    public static String TWOFA_TOKEN_TOKEN = "twofatoken";
    public static PatternedSystemTag TWOFA_TOKEN = new PatternedSystemTag(String.format("twofatoken::{%s}", TWOFA_TOKEN_TOKEN), AccountVO.class);
}
