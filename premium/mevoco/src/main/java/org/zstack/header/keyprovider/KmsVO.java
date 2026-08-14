package org.zstack.header.keyprovider;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.NoView;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@AutoDeleteTag
public class KmsVO extends KeyProviderVO {
    @Column
    private String endpoint;

    @Column
    private Integer port;

    @Column
    @Enumerated(EnumType.STRING)
    private KmipVersion kmipVersion;

    @Column
    private String username;

    @Column
    @Convert(converter = PasswordConverter.class)
    private String password;

    @Column
    @Enumerated(EnumType.STRING)
    private KmsTrustState trustState;

    @Column
    private String activeIdentityUuid;

    @Column
    private Timestamp serverCertExpiredDate;

    @Column(columnDefinition = "Text")
    @Lob
    private String serverCertPem;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activeIdentityUuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    @NoView
    private KmsIdentityVO activeIdentity;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public KmipVersion getKmipVersion() {
        return kmipVersion;
    }

    public void setKmipVersion(KmipVersion kmipVersion) {
        this.kmipVersion = kmipVersion;
    }

    public String getKmipVersionString() {
        return kmipVersion == null ? null : kmipVersion.toString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public KmsTrustState getTrustState() {
        return trustState;
    }

    public void setTrustState(KmsTrustState trustState) {
        this.trustState = trustState;
    }

    public String getActiveIdentityUuid() {
        return activeIdentityUuid;
    }

    public void setActiveIdentityUuid(String activeIdentityUuid) {
        this.activeIdentityUuid = activeIdentityUuid;
    }

    public Timestamp getServerCertExpiredDate() {
        return serverCertExpiredDate;
    }

    public void setServerCertExpiredDate(Timestamp serverCertExpiredDate) {
        this.serverCertExpiredDate = serverCertExpiredDate;
    }

    public String getServerCertPem() {
        return serverCertPem;
    }

    public void setServerCertPem(String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }

    public KmsIdentityVO getActiveIdentity() {
        return activeIdentity;
    }

    public void setActiveIdentity(KmsIdentityVO activeIdentity) {
        this.activeIdentity = activeIdentity;
    }
}
