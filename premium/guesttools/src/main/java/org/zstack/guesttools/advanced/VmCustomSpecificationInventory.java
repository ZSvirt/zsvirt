package org.zstack.guesttools.advanced;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.DocUtils;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmCustomSpecificationVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid")
})
public class VmCustomSpecificationInventory implements Serializable {
    private String uuid;
    private String vmInstanceUuid;
    private String name;
    private String description;
    private String platform;
    private String hostname;
    @GsonTransient
    @APINoSee
    private String rootPassword;
    private Boolean generateSID;
    private VmCustomSpecificationDomainMode domainMode;
    private String domainName;
    private String domainUsername;
    @GsonTransient
    @APINoSee
    private String domainPassword;
    private String organization;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmCustomSpecificationInventory() {}

    public VmCustomSpecificationInventory(VmCustomSpecificationVO vo) {
        this.uuid = vo.getUuid();
        this.vmInstanceUuid = vo.getVmInstanceUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.platform = vo.getPlatform();
        this.hostname = vo.getHostname();
        this.rootPassword = vo.getRootPassword();
        this.generateSID = vo.isGenerateSID();
        this.domainMode = vo.getDomainMode();
        this.domainName = vo.getDomainName();
        this.domainUsername = vo.getDomainUsername();
        this.domainPassword = vo.getDomainPassword();
        this.organization = vo.getOrganization();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static VmCustomSpecificationInventory valueOf(VmCustomSpecificationVO vo) {
        return new VmCustomSpecificationInventory(vo);
    }

    public static List<VmCustomSpecificationInventory> valueOf(List<VmCustomSpecificationVO> vos) {
        return vos.stream().map(VmCustomSpecificationInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public static VmCustomSpecificationInventory __example__() {
        VmCustomSpecificationInventory inv = new VmCustomSpecificationInventory();
        inv.setUuid(DocUtils.createFixedUuid(VmCustomSpecificationVO.class));
        inv.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        inv.setName("custom-specification-for-vm-1");
        inv.setDescription("Test");
        inv.setPlatform(ImagePlatform.Windows.toString());
        inv.setHostname("vm-1");
        inv.setRootPassword("password");
        inv.setGenerateSID(true);
        inv.setDomainMode(VmCustomSpecificationDomainMode.Domain);
        inv.setDomainName("zsv.test");
        inv.setDomainUsername("test");
        inv.setDomainPassword("password");
        inv.setOrganization("OU=zsv,DC=zsv,DC=test");

        return inv;
    }
}
