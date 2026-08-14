package org.zstack.header.acl;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@Entity
@Table
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = AccessControlListEntryVO.class, myField = "aclUuid", targetField = "uuid"),
        }
)
public class AccessControlListVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Integer ipVersion;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="aclUuid", insertable=false, updatable=false)
    @NoView
    private Set<AccessControlListEntryVO> entries = new HashSet<AccessControlListEntryVO>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Set<AccessControlListEntryVO> getEntries() {
        return entries;
    }

    public void setEntries(Set<AccessControlListEntryVO> entries) {
        this.entries = entries;
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
