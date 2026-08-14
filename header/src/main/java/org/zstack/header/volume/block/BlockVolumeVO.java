package org.zstack.header.volume.block;

import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @author shenjin
 * @date 2023/6/13 16:03
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class BlockVolumeVO extends VolumeVO {
    @Column
    private String iscsiPath;
    @Column
    private String vendor;
    
    public BlockVolumeVO() {
    }
    
    public BlockVolumeVO(VolumeVO vo) {
        this.setUuid(vo.getUuid());
        this.setDescription(vo.getDescription());
        this.setName(vo.getName());
        this.setDiskOfferingUuid(vo.getDiskOfferingUuid());
        this.setSize(vo.getSize());
        this.setActualSize(vo.getActualSize());
        this.setType(vo.getType());
        this.setState(vo.getState());
        this.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        this.setStatus(vo.getStatus());
        this.setAccountUuid(vo.getAccountUuid());
        this.setDeviceId(vo.getDeviceId());
        this.setFormat(vo.getFormat());
        this.setInstallPath(vo.getInstallPath());
        this.setRootImageUuid(vo.getRootImageUuid());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setLastVmInstanceUuid(vo.getLastVmInstanceUuid());
        this.setShareable(vo.isShareable());
        this.setProtocol(vo.getProtocol());
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getIscsiPath() {
        return iscsiPath;
    }

    public void setIscsiPath(String iscsiPath) {
        this.iscsiPath = iscsiPath;
    }
}
