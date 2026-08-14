package org.zstack.kvm.hypervisor.datatype;

import org.zstack.header.host.HostAO;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table
public class HostOsCategoryVO implements ToInventory {
    @Id
    @Column
    @Index
    private String uuid;
    /**
     * equals to {@link HostAO#getArchitecture()}
     */
    @Column
    private String architecture;
    /**
     * "centos Core 7.6.1810" / "centos Core 7.4.1708" ...
     */
    @Column
    private String osReleaseVersion;

    @Column
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoryUuid", insertable = false, updatable = false)
    private List<KvmHostHypervisorMetadataVO> metadataList;

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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getOsReleaseVersion() {
        return osReleaseVersion;
    }

    public void setOsReleaseVersion(String osReleaseVersion) {
        this.osReleaseVersion = osReleaseVersion;
    }

    public List<KvmHostHypervisorMetadataVO> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<KvmHostHypervisorMetadataVO> metadataList) {
        this.metadataList = metadataList;
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
