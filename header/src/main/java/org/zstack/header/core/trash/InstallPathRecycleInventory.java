package org.zstack.header.core.trash;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = InstallPathRecycleVO.class)
public class InstallPathRecycleInventory implements Serializable {
    private long trashId;
    private String resourceUuid;
    private String storageUuid;
    private String storageType;
    private String resourceType;
    private String installPath;
    private Boolean isFolder;
    private String hostUuid;
    private String hypervisorType;
    private Long size;
    private String trashType;
    private Timestamp createDate;

    protected InstallPathRecycleInventory(InstallPathRecycleVO vo) {
        this.setTrashId(vo.getTrashId());
        this.setResourceUuid(vo.getResourceUuid());
        this.setResourceType(vo.getResourceType());
        this.setStorageUuid(vo.getStorageUuid());
        this.setStorageType(vo.getStorageType());
        this.setInstallPath(vo.getInstallPath());
        this.setHostUuid(vo.getHostUuid());
        this.setFolder(vo.getFolder());
        this.setHypervisorType(vo.getHypervisorType());
        this.setSize(vo.getSize());
        this.setTrashType(vo.getTrashType());
        this.setCreateDate(vo.getCreateDate());
    }

    public static InstallPathRecycleInventory valueOf(InstallPathRecycleVO vo) {
        return new InstallPathRecycleInventory(vo);
    }

    public static List<InstallPathRecycleInventory> valueOf(Collection<InstallPathRecycleVO> vos) {
        List<InstallPathRecycleInventory> invs = new ArrayList<InstallPathRecycleInventory>(vos.size());
        for (InstallPathRecycleVO vo : vos) {
            invs.add(InstallPathRecycleInventory.valueOf(vo));
        }
        return invs;
    }

    public InstallPathRecycleInventory() {
    }

    public long getTrashId() {
        return trashId;
    }

    public void setTrashId(long trashId) {
        this.trashId = trashId;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getStorageUuid() {
        return storageUuid;
    }

    public void setStorageUuid(String storageUuid) {
        this.storageUuid = storageUuid;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Boolean getFolder() {
        return isFolder;
    }

    public void setFolder(Boolean isFolder) {
        this.isFolder = isFolder;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getTrashType() {
        return trashType;
    }

    public void setTrashType(String trashType) {
        this.trashType = trashType;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
