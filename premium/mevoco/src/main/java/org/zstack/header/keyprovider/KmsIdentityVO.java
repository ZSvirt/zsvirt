package org.zstack.header.keyprovider;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
public class KmsIdentityVO {
    @Id
    @Column
    private String uuid;

    @Column
    @Index
    @ForeignKey(parentEntityClass = KmsVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String kmsUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private KmsIdentityType identityType;

    @Column(columnDefinition = "Text")
    @Lob
    private String clientCertPem;

    @Column(columnDefinition = "Text")
    @Lob
    @Convert(converter = PasswordConverter.class)
    private String clientKeyPem;

    @Column(columnDefinition = "Text")
    @Lob
    @Convert(converter = PasswordConverter.class)
    private String csrPem;

    @Column
    private Timestamp certExpiredDate;

    @Column
    private Timestamp createDate;

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

    public String getKmsUuid() {
        return kmsUuid;
    }

    public void setKmsUuid(String kmsUuid) {
        this.kmsUuid = kmsUuid;
    }

    public KmsIdentityType getIdentityType() {
        return identityType;
    }

    public void setIdentityType(KmsIdentityType identityType) {
        this.identityType = identityType;
    }

    public String getClientCertPem() {
        return clientCertPem;
    }

    public void setClientCertPem(String clientCertPem) {
        this.clientCertPem = clientCertPem;
    }

    public String getClientKeyPem() {
        return clientKeyPem;
    }

    public void setClientKeyPem(String clientKeyPem) {
        this.clientKeyPem = clientKeyPem;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public Timestamp getCertExpiredDate() {
        return certExpiredDate;
    }

    public void setCertExpiredDate(Timestamp certExpiredDate) {
        this.certExpiredDate = certExpiredDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
