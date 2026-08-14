package org.zstack.header.core.encrypt;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @author hanyu.liang
 * @date 2023/5/5 16:18
 */
@RestRequest(
        path = "/encrypted/fields",
        method = HttpMethod.GET,
        responseClass = APIGetEncryptedFieldReply.class
)
public class APIGetEncryptedFieldMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String encryptedType;

    public String getEncryptedType() {
        return encryptedType;
    }

    public void setEncryptedType(String encryptedType) {
        this.encryptedType = encryptedType;
    }

    public static APIGetEncryptedFieldMsg __example__() {
        APIGetEncryptedFieldMsg msg = new APIGetEncryptedFieldMsg();
        msg.setEncryptedType("SM4");
        return msg;
    }
}
