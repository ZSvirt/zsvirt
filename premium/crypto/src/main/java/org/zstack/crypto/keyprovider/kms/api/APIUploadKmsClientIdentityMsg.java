package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KmsIdentityType;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/kms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUploadKmsClientIdentityEvent.class,
        isAction = true
)
public class APIUploadKmsClientIdentityMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KmsVO.class, emptyString = false)
    private String uuid;

    @APIParam(validEnums = {KmsIdentityType.class}, emptyString = false)
    private String identityType;

    @APIParam(emptyString = false)
    @NoLogging
    private String kmsClientCertPem;

    @APIParam(emptyString = false)
    @NoLogging
    private String kmsClientKeyPem;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getKmsClientCertPem() {
        return kmsClientCertPem;
    }

    public void setKmsClientCertPem(String kmsClientCertPem) {
        this.kmsClientCertPem = kmsClientCertPem;
    }

    public String getKmsClientKeyPem() {
        return kmsClientKeyPem;
    }

    public void setKmsClientKeyPem(String kmsClientKeyPem) {
        this.kmsClientKeyPem = kmsClientKeyPem;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    public static APIUploadKmsClientIdentityMsg __example__() {
        APIUploadKmsClientIdentityMsg msg = new APIUploadKmsClientIdentityMsg();
        msg.setUuid(uuid(KmsVO.class));
        msg.setIdentityType(KmsIdentityType.UPLOADED.toString());
        msg.setKmsClientCertPem("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        msg.setKmsClientKeyPem("-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----");
        return msg;
    }
}
