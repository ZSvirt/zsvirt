package org.zstack.encrypt;

import com.google.gson.JsonObject;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;

public interface EncryptionParamParser {
    String encryptionType();

    /**
     * parse cipherText to JsonObject
     */
    ErrorableValue<ParsedResult> parse(String cipherText, EncryptedMessageBundle bundle);

    ErrorCode checkUserInfo(Object userInfo, EncryptedMessageBundle bundle);
    ErrorCode putUserInfoIntoSystemTag(Object userInfo, EncryptedMessageBundle bundle);

    class ParsedResult {
        public JsonObject jsonObject;
        public Object userInfo;
    }
}
