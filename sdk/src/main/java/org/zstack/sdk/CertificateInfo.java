package org.zstack.sdk;



public class CertificateInfo  {

    public java.lang.String subject;
    public void setSubject(java.lang.String subject) {
        this.subject = subject;
    }
    public java.lang.String getSubject() {
        return this.subject;
    }

    public java.lang.String issuer;
    public void setIssuer(java.lang.String issuer) {
        this.issuer = issuer;
    }
    public java.lang.String getIssuer() {
        return this.issuer;
    }

    public java.lang.String commonName;
    public void setCommonName(java.lang.String commonName) {
        this.commonName = commonName;
    }
    public java.lang.String getCommonName() {
        return this.commonName;
    }

    public java.util.List subjectAltNamesDns;
    public void setSubjectAltNamesDns(java.util.List subjectAltNamesDns) {
        this.subjectAltNamesDns = subjectAltNamesDns;
    }
    public java.util.List getSubjectAltNamesDns() {
        return this.subjectAltNamesDns;
    }

    public java.util.List subjectAltNamesIp;
    public void setSubjectAltNamesIp(java.util.List subjectAltNamesIp) {
        this.subjectAltNamesIp = subjectAltNamesIp;
    }
    public java.util.List getSubjectAltNamesIp() {
        return this.subjectAltNamesIp;
    }

    public java.sql.Timestamp expiredDate;
    public void setExpiredDate(java.sql.Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }
    public java.sql.Timestamp getExpiredDate() {
        return this.expiredDate;
    }

}
