package org.zstack.sdk;



public class KmsIdentityInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String kmsUuid;
    public void setKmsUuid(java.lang.String kmsUuid) {
        this.kmsUuid = kmsUuid;
    }
    public java.lang.String getKmsUuid() {
        return this.kmsUuid;
    }

    public java.lang.String identityType;
    public void setIdentityType(java.lang.String identityType) {
        this.identityType = identityType;
    }
    public java.lang.String getIdentityType() {
        return this.identityType;
    }

    public java.lang.String clientCertPem;
    public void setClientCertPem(java.lang.String clientCertPem) {
        this.clientCertPem = clientCertPem;
    }
    public java.lang.String getClientCertPem() {
        return this.clientCertPem;
    }

    public java.lang.String csrPem;
    public void setCsrPem(java.lang.String csrPem) {
        this.csrPem = csrPem;
    }
    public java.lang.String getCsrPem() {
        return this.csrPem;
    }

    public java.sql.Timestamp certExpiredDate;
    public void setCertExpiredDate(java.sql.Timestamp certExpiredDate) {
        this.certExpiredDate = certExpiredDate;
    }
    public java.sql.Timestamp getCertExpiredDate() {
        return this.certExpiredDate;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
