package org.zstack.header.cloudformation;

import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.*;

/**
 * Created by mingjian.deng on 2018/6/12.
 */
@Entity
@Table
public class CloudFormationStackResourceRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;
    @Column
    @ForeignKey(parentEntityClass = ResourceStackVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String stackUuid;
    @Column
    @ForeignKey(parentEntityClass = ResourceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    @Index
    private String resourceUuid;
    @Column
    private String resourceType;
    @Column
    private String resourceName;
    @Column
    private Boolean reserve;
    @Column
    private Integer round;

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Boolean getReserve() {
        return reserve;
    }

    public void setReserve(Boolean reserve) {
        this.reserve = reserve;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
