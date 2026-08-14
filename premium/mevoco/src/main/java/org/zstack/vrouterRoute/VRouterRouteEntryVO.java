package org.zstack.vrouterRoute;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by weiwang on 15/06/2017.
 */
@Entity
@Table
public class VRouterRouteEntryVO extends ResourceVO {
    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private VRouterRouteEntryType type;

    @Column
    @ForeignKey(parentEntityClass = VRouterRouteTableVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String routeTableUuid;

    @Column
    private String destination;

    @Column
    private String target;

    @Column
    private Integer distance;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VRouterRouteEntryType getType() {
        return type;
    }

    public void setType(VRouterRouteEntryType type) {
        this.type = type;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
