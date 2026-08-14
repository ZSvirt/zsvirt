package org.zstack.softwarePackage.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Inventory(mappingVOClass = SoftwarePackageVO.class)
@PythonClassInventory
public class SoftwarePackageInventory implements Serializable {
    private String uuid;
    private String name;
    private String hostUuid;
    private String managementNodeUuid;
    private String installPath;
    private String unzipInstallPath;
    private String type;
    private String md5sum;
    private String status;
    private long size;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public SoftwarePackageInventory() {
    }

    public SoftwarePackageInventory(SoftwarePackageInventory other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.hostUuid = other.hostUuid;
        this.managementNodeUuid = other.managementNodeUuid;
        this.installPath = other.installPath;
        this.unzipInstallPath = other.unzipInstallPath;
        this.type = other.type;
        this.md5sum = other.md5sum;
        this.status = other.status;
        this.size = other.size;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public static SoftwarePackageInventory valueOf(SoftwarePackageVO vo) {
        SoftwarePackageInventory inv = new SoftwarePackageInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setHostUuid(vo.getHostUuid());
        inv.setManagementNodeUuid(vo.getManagementNodeUuid());
        inv.setInstallPath(vo.getInstallPath());
        inv.setUnzipInstallPath(vo.getUnzipInstallPath());
        inv.setType(vo.getType());
        inv.setMd5sum(vo.getMd5sum());
        inv.setStatus(vo.getStatus());
        inv.setSize(vo.getSize());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SoftwarePackageInventory> valueOf(Collection<SoftwarePackageVO> vos) {
        List<SoftwarePackageInventory> invs = new ArrayList<>(vos.size());
        for (SoftwarePackageVO vo : vos) {
            invs.add(SoftwarePackageInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getUnzipInstallPath() {
        return unzipInstallPath;
    }

    public void setUnzipInstallPath(String unzipInstallPath) {
        this.unzipInstallPath = unzipInstallPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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


    public static SoftwarePackageInventory __example__() {
        SoftwarePackageInventory inventory = new SoftwarePackageInventory();
        inventory.setUuid("53c0433a817349f89f66532addeb8b6c");
        inventory.setName("SoftwarePackage");
        inventory.setHostUuid("53c0433a817349f89f66532addeb8b61");
        inventory.setManagementNodeUuid("53c0433a817349f89f66532addeb8b62");
        inventory.setInstallPath("/root/file/ZStone-common-installer-5.4.18-1932.tar.gz");
        inventory.setUnzipInstallPath("/root/file/ZStone-common-installer-5.4.18-1932.tar.gz_2c1571fab770b5bab7f411d48aad5029_1762415407323/zstone-installer");
        inventory.setType("zstone");
        inventory.setMd5sum("53c0433a817349f89f66532addeb8b63");
        inventory.setStatus("Uploaded");
        inventory.setSize(1024);
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        return inventory;
    }
}
