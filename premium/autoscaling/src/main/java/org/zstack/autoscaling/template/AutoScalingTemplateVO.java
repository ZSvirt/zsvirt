package org.zstack.autoscaling.template;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/19
 */
@Entity
@Table
@BaseResource
@Inheritance(strategy = InheritanceType.JOINED)
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = AutoScalingTemplateGroupRefVO.class, myField = "uuid", targetField = "templateUuid")
        }
)
public class AutoScalingTemplateVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private AutoScalingTemplateState state;

    @Column
    private String description;

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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AutoScalingTemplateState getState() {
        return state;
    }

    public void setState(AutoScalingTemplateState state) {
        this.state = state;
    }
}
