package org.zstack.crypto.keyprovider.kms.api;

import org.zstack.header.keyprovider.CertificateInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

@RestResponse(fieldsTo = "all")
public class APIGetKmsServerCertFromKmsEvent extends APIEvent {
    @NoLogging
    private String serverCertPem;
    private boolean selfSigned;
    private CertificateInfo serverCertInfo;

    public APIGetKmsServerCertFromKmsEvent() {
        super(null);
    }

    public APIGetKmsServerCertFromKmsEvent(String apiId) {
        super(apiId);
    }

    public String getServerCertPem() {
        return serverCertPem;
    }

    public void setServerCertPem(String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }

    public boolean isSelfSigned() {
        return selfSigned;
    }

    public void setSelfSigned(boolean selfSigned) {
        this.selfSigned = selfSigned;
    }

    public CertificateInfo getServerCertInfo() {
        return serverCertInfo;
    }

    public void setServerCertInfo(CertificateInfo serverCertInfo) {
        this.serverCertInfo = serverCertInfo;
    }

    public static APIGetKmsServerCertFromKmsEvent __example__() {
        APIGetKmsServerCertFromKmsEvent event = new APIGetKmsServerCertFromKmsEvent();
        event.setServerCertPem("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        event.setSelfSigned(true);
        event.setServerCertInfo(new CertificateInfo("CN=kms.example.com,O=zstack", "CN=zstack-ca,O=zstack",
                "kms.example.com", Arrays.asList("kms.example.com"), Arrays.asList("127.0.0.1"), DocUtils.timestamp()));
        return event;
    }
}
