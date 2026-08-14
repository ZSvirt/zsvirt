package org.zstack.nas;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/3/8.
 */
@Entity
@Table
@BaseResource
public class NasMountTargetVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    @ForeignKey(parentEntityClass = NasFileSystemVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String nasFileSystemUuid;
    @Column
    protected String mountDomain;
    @Column
    private String type;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

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

    public String getNasFileSystemUuid() {
        return nasFileSystemUuid;
    }

    public void setNasFileSystemUuid(String nasFileSystemUuid) {
        this.nasFileSystemUuid = nasFileSystemUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getMountDomain() {
        return mountDomain;
    }

    public void setMountDomain(String mountDomain) {
        this.mountDomain = mountDomain;
    }
}
