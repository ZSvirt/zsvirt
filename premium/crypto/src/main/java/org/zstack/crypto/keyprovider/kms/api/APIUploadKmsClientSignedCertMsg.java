package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/kms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUploadKmsClientSignedCertEvent.class,
        isAction = true
)
public class APIUploadKmsClientSignedCertMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KmsVO.class, emptyString = false)
    private String uuid;

    @APIParam(emptyString = false)
    @NoLogging
    private String signedClientCertPem;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSignedClientCertPem() {
        return signedClientCertPem;
    }

    public void setSignedClientCertPem(String signedClientCertPem) {
        this.signedClientCertPem = signedClientCertPem;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    public static APIUploadKmsClientSignedCertMsg __example__() {
        APIUploadKmsClientSignedCertMsg msg = new APIUploadKmsClientSignedCertMsg();
        msg.setUuid(uuid(KmsVO.class));
        msg.setSignedClientCertPem("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        return msg;
    }
}
