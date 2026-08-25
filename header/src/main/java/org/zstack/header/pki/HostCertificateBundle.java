package org.zstack.header.pki;

import java.sql.Timestamp;

public class HostCertificateBundle {
    private String certificatePem;
    private String caCertPem;
    private String serial;
    private String fingerprint;
    private Timestamp notBefore;
    private Timestamp notAfter;

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    public String getCaCertPem() {
        return caCertPem;
    }

    public void setCaCertPem(String caCertPem) {
        this.caCertPem = caCertPem;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Timestamp getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Timestamp notBefore) {
        this.notBefore = notBefore;
    }

    public Timestamp getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Timestamp notAfter) {
        this.notAfter = notAfter;
    }
}
