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
        responseClass = APIUploadKmsServerCertEvent.class,
        isAction = true
)
public class APIUploadKmsServerCertMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KmsVO.class, emptyString = false)
    private String uuid;

    @APIParam(emptyString = false)
    @NoLogging
    private String serverCertPem;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerCertPem() {
        return serverCertPem;
    }

    public void setServerCertPem(String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    public static APIUploadKmsServerCertMsg __example__() {
        APIUploadKmsServerCertMsg msg = new APIUploadKmsServerCertMsg();
        msg.setUuid(uuid(KmsVO.class));
        msg.setServerCertPem("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        return msg;
    }
}
