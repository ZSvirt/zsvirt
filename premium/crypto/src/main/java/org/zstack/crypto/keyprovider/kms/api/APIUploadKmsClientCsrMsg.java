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
        responseClass = APIUploadKmsClientCsrEvent.class,
        isAction = true
)
public class APIUploadKmsClientCsrMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KmsVO.class, emptyString = false)
    private String uuid;

    @APIParam(emptyString = false)
    @NoLogging
    private String csrPem;

    @APIParam(emptyString = false)
    @NoLogging
    private String csrKeyPem;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public String getCsrKeyPem() {
        return csrKeyPem;
    }

    public void setCsrKeyPem(String csrKeyPem) {
        this.csrKeyPem = csrKeyPem;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    public static APIUploadKmsClientCsrMsg __example__() {
        APIUploadKmsClientCsrMsg msg = new APIUploadKmsClientCsrMsg();
        msg.setUuid(uuid(KmsVO.class));
        msg.setCsrPem("-----BEGIN CERTIFICATE REQUEST-----\n...\n-----END CERTIFICATE REQUEST-----");
        msg.setCsrKeyPem("-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----");
        return msg;
    }
}
