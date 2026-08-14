package org.zstack.kvm.hypervisor.datatype;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostOsCategoryVO.class, collectionValueOfMethod = "valueOf1")
public class HostOsCategoryInventory implements Serializable {
    private String uuid;
    private String architecture;
    private String osReleaseVersion;
    private List<KvmHostHypervisorMetadataInventory> metadataList;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public HostOsCategoryInventory() {
    }

    public static HostOsCategoryInventory valueOf(HostOsCategoryVO vo) {
        HostOsCategoryInventory inv = new HostOsCategoryInventory();
        inv.setUuid(vo.getUuid());
        inv.setArchitecture(vo.getArchitecture());
        inv.setOsReleaseVersion(vo.getOsReleaseVersion());
        inv.setMetadataList(KvmHostHypervisorMetadataInventory.valueOf1(vo.getMetadataList()));
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<HostOsCategoryInventory> valueOf1(Collection<HostOsCategoryVO> vos) {
        return vos.stream().map(HostOsCategoryInventory::valueOf).collect(Collectors.toList());
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

    public List<KvmHostHypervisorMetadataInventory> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<KvmHostHypervisorMetadataInventory> metadataList) {
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
