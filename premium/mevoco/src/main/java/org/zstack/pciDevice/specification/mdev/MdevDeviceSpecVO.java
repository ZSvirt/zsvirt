package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-04-17.
 */
@Entity
@Table
@BaseResource
public class MdevDeviceSpecVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String specification;

    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceType type;

    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceSpecState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @PreUpdate
    void preUpdate() {
        lastOpDate = null;
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

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public MdevDeviceType getType() {
        return type;
    }

    public void setType(MdevDeviceType type) {
        this.type = type;
    }

    public MdevDeviceSpecState getState() {
        return state;
    }

    public void setState(MdevDeviceSpecState state) {
        this.state = state;
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
