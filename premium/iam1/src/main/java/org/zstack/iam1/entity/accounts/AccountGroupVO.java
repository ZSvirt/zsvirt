package org.zstack.iam1.entity.accounts;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = AccountGroupAccountRefVO.class, myField = "uuid", targetField = "groupUuid"),
                @EntityGraph.Neighbour(type = AccountGroupRoleRefVO.class, myField = "uuid", targetField = "groupUuid"),
                @EntityGraph.Neighbour(type = AccountGroupResourceRefVO.class, myField = "uuid", targetField = "groupUuid"),
        }
)
public class AccountGroupVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String parentUuid;

    @Column
    private String rootGroupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getRootGroupUuid() {
        return rootGroupUuid;
    }

    public void setRootGroupUuid(String rootGroupUuid) {
        this.rootGroupUuid = rootGroupUuid;
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
