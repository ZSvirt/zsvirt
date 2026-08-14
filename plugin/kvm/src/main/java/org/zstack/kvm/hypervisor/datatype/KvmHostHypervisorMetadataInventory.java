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
@Inventory(mappingVOClass = KvmHostHypervisorMetadataVO.class, collectionValueOfMethod = "valueOf1")
public class KvmHostHypervisorMetadataInventory implements Serializable {
    private String uuid;
    private String categoryUuid;
    private String managementNodeUuid;
    private String hypervisor;
    private String version;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public KvmHostHypervisorMetadataInventory() {
    }

    public static KvmHostHypervisorMetadataInventory valueOf(KvmHostHypervisorMetadataVO vo) {
        KvmHostHypervisorMetadataInventory inv = new KvmHostHypervisorMetadataInventory();
        inv.setUuid(vo.getUuid());
        inv.setCategoryUuid(vo.getCategoryUuid());
        inv.setManagementNodeUuid(vo.getManagementNodeUuid());
        inv.setHypervisor(vo.getHypervisor());
        inv.setVersion(vo.getVersion());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<KvmHostHypervisorMetadataInventory> valueOf1(Collection<KvmHostHypervisorMetadataVO> vos) {
        return vos.stream().map(KvmHostHypervisorMetadataInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCategoryUuid() {
        return categoryUuid;
    }

    public void setCategoryUuid(String categoryUuid) {
        this.categoryUuid = categoryUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
