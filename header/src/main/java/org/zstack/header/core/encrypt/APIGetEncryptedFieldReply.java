package org.zstack.header.core.encrypt;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @author hanyu.liang
 * @date 2023/5/5 16:18
 */
@RestResponse(fieldsTo = "all")
public class APIGetEncryptedFieldReply extends APIReply {
    List<String> encryptedFields;

    public List<String> getEncryptedFields() {
        return encryptedFields;
    }

    public void setEncryptedFields(List<String> encryptedFields) {
        this.encryptedFields = encryptedFields;
    }

    public static APIGetEncryptedFieldReply __example__() {
        APIGetEncryptedFieldReply ret = new APIGetEncryptedFieldReply();
        ret.setEncryptedFields(Arrays.asList(EncryptedFieldBundle.class.getSimpleName()));
        return ret;
    }
}
