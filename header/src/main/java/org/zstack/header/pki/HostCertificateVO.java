package org.zstack.header.pki;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
public class HostCertificateVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String caUuid;

    @Column(name = "certUsage")
    private String usage;

    @Column
    private String serial;

    @Column
    private String fingerprint;

    @Column
    private String sanSnapshot;

    @Column
    private String status;

    @Column
    private Timestamp notBefore;

    @Column
    private Timestamp notAfter;

    @Column
    private Timestamp lastInstallDate;

    @Column
    private String lastError;

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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getCaUuid() {
        return caUuid;
    }

    public void setCaUuid(String caUuid) {
        this.caUuid = caUuid;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
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

    public String getSanSnapshot() {
        return sanSnapshot;
    }

    public void setSanSnapshot(String sanSnapshot) {
        this.sanSnapshot = sanSnapshot;
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

    public Timestamp getLastInstallDate() {
        return lastInstallDate;
    }

    public void setLastInstallDate(Timestamp lastInstallDate) {
        this.lastInstallDate = lastInstallDate;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
