package org.zstack.encrypt;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by Wenhao.Zhang on 22/11/18
 */
@TagDefinition
public class EncryptSystemTags {
    public static final String ENCRYPTION_PARAM_HEADER = "encryptionParam";
    public static final String ENCRYPTION_TYPE_TOKEN = "encryptionType";
    public static final String CIPHER_TEXT_TOKEN = "cipherText";

    // Only for input
    public static PatternedSystemTag ENCRYPTION_PARAM = new PatternedSystemTag(
            String.format("%s::{%s}::{%s}", ENCRYPTION_PARAM_HEADER, ENCRYPTION_TYPE_TOKEN, CIPHER_TEXT_TOKEN),
            AccountVO.class);
}
