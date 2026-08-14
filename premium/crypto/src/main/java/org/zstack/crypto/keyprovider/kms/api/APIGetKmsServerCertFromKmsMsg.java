package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/kms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIGetKmsServerCertFromKmsEvent.class,
        isAction = true
)
public class APIGetKmsServerCertFromKmsMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KmsVO.class, emptyString = false)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    public static APIGetKmsServerCertFromKmsMsg __example__() {
        APIGetKmsServerCertFromKmsMsg msg = new APIGetKmsServerCertFromKmsMsg();
        msg.setUuid(uuid(KmsVO.class));
        return msg;
    }
}
