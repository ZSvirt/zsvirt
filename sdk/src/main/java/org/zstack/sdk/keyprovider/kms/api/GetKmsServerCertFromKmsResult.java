package org.zstack.sdk.keyprovider.kms.api;

import org.zstack.sdk.CertificateInfo;

public class GetKmsServerCertFromKmsResult {
    public java.lang.String serverCertPem;
    public void setServerCertPem(java.lang.String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }
    public java.lang.String getServerCertPem() {
        return this.serverCertPem;
    }

    public boolean selfSigned;
    public void setSelfSigned(boolean selfSigned) {
        this.selfSigned = selfSigned;
    }
    public boolean getSelfSigned() {
        return this.selfSigned;
    }

    public CertificateInfo serverCertInfo;
    public void setServerCertInfo(CertificateInfo serverCertInfo) {
        this.serverCertInfo = serverCertInfo;
    }
    public CertificateInfo getServerCertInfo() {
        return this.serverCertInfo;
    }

}
