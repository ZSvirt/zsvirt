package org.zstack.header.keyprovider;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class CertificateInfo implements Serializable {
    private String subject;
    private String issuer;
    private String commonName;
    private List<String> subjectAltNamesDns;
    private List<String> subjectAltNamesIp;
    private Timestamp expiredDate;

    public CertificateInfo() {
    }

    public CertificateInfo(String subject, String issuer, String commonName, List<String> subjectAltNamesDns, List<String> subjectAltNamesIp,
                           Timestamp expiredDate) {
        this.subject = subject;
        this.issuer = issuer;
        this.commonName = commonName;
        this.subjectAltNamesDns = subjectAltNamesDns == null ? Collections.emptyList() : subjectAltNamesDns;
        this.subjectAltNamesIp = subjectAltNamesIp == null ? Collections.emptyList() : subjectAltNamesIp;
        this.expiredDate = expiredDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public List<String> getSubjectAltNamesDns() {
        return subjectAltNamesDns;
    }

    public void setSubjectAltNamesDns(List<String> subjectAltNamesDns) {
        this.subjectAltNamesDns = subjectAltNamesDns;
    }

    public List<String> getSubjectAltNamesIp() {
        return subjectAltNamesIp;
    }

    public void setSubjectAltNamesIp(List<String> subjectAltNamesIp) {
        this.subjectAltNamesIp = subjectAltNamesIp;
    }

    public Timestamp getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }
}
