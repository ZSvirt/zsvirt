package org.zstack.guesttools.advanced;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@org.zstack.header.vo.EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid")
        }
)
public class VmCustomSpecificationVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String vmInstanceUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String platform;

    @Column
    private String hostname;

    @Column
    @Convert(converter = PasswordConverter.class)
    private String rootPassword;

    @Column
    private Boolean generateSID;

    @Column
    @Enumerated(EnumType.STRING)
    private VmCustomSpecificationDomainMode domainMode;

    @Column
    private String domainName;

    @Column
    private String domainUsername;

    @Column
    @Convert(converter = PasswordConverter.class)
    private String domainPassword;

    @Column
    private String organization;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    public VmCustomSpecificationVO() {
    }

    public VmCustomSpecificationVO(VmCustomSpecificationStruct spec) {
        updateVOByVmCustomSpec(spec);
    }

    public void updateVOByVmCustomSpec(VmCustomSpecificationStruct spec) {
        this.setPlatform(spec.getPlatform());
        this.setHostname(spec.getHostname());
        this.setRootPassword(spec.getRootPassword());
        this.setGenerateSID(spec.getGenerateSID());
        this.setDomainMode(spec.getDomainMode());
        this.setDomainName(spec.getDomainName());
        this.setDomainUsername(spec.getDomainUsername());
        this.setDomainPassword(spec.getDomainPassword());
        this.setOrganization(spec.getOrganization());
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public Boolean isGenerateSID() {
        return generateSID;
    }

    public void setGenerateSID(Boolean generateSID) {
        this.generateSID = generateSID;
    }

    public VmCustomSpecificationDomainMode getDomainMode() {
        return domainMode;
    }

    public void setDomainMode(VmCustomSpecificationDomainMode domainMode) {
        this.domainMode = domainMode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainUsername() {
        return domainUsername;
    }

    public void setDomainUsername(String domainUsername) {
        this.domainUsername = domainUsername;
    }

    public String getDomainPassword() {
        return domainPassword;
    }

    public void setDomainPassword(String domainPassword) {
        this.domainPassword = domainPassword;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
