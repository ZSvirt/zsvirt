package org.zstack.header.pki;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
public class PkiCaVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String scope;

    @Column
    private String caType;

    @Column
    private String subjectDn;

    @Column
    private String certChainPem;

    @Column
    private String encryptedPrivateKeyPem;

    @Column
    private String serial;

    @Column
    private String fingerprint;

    @Column
    private String status;

    @Column
    private Timestamp notBefore;

    @Column
    private Timestamp notAfter;

    @Column
    private Timestamp createDate;

    @Column
    private String crlPem;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCaType() {
        return caType;
    }

    public void setCaType(String caType) {
        this.caType = caType;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getCertChainPem() {
        return certChainPem;
    }

    public void setCertChainPem(String certChainPem) {
        this.certChainPem = certChainPem;
    }

    public String getEncryptedPrivateKeyPem() {
        return encryptedPrivateKeyPem;
    }

    public void setEncryptedPrivateKeyPem(String encryptedPrivateKeyPem) {
        this.encryptedPrivateKeyPem = encryptedPrivateKeyPem;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getCrlPem() {
        return crlPem;
    }

    public void setCrlPem(String crlPem) {
        this.crlPem = crlPem;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
