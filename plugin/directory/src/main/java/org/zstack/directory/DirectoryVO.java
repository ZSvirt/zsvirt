package org.zstack.directory;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author shenjin
 * @date 2022/11/29 10:12
 */
@Entity
@Table
public class DirectoryVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    private String name;
    @Column
    private String groupName;
    @Column
    private String parentUuid;
    @Column
    private String rootDirectoryUuid;
    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = ZoneVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String zoneUuid;
    @Column
    private String type;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getRootDirectoryUuid() {
        return rootDirectoryUuid;
    }

    public void setRootDirectoryUuid(String rootDirectoryUuid) {
        this.rootDirectoryUuid = rootDirectoryUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
