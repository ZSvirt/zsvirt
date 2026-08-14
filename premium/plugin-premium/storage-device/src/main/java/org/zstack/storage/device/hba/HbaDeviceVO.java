package org.zstack.storage.device.hba;

import org.zstack.header.host.HostEO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 15:36
 */
@Entity
@Table
@BaseResource
public class HbaDeviceVO extends ResourceVO implements ToInventory {
    @Column
    private String name;
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private HbaType hbaType;
    @Column
    private String createDate;
    @Column
    private String lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(String lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public HbaType getHbaType() {
        return hbaType;
    }

    public void setHbaType(HbaType hbaType) {
        this.hbaType = hbaType;
    }
}
