package org.zstack.sdk;

import org.zstack.sdk.VRouterRouteEntryType;

public class VRouterRouteEntryInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public VRouterRouteEntryType type;
    public void setType(VRouterRouteEntryType type) {
        this.type = type;
    }
    public VRouterRouteEntryType getType() {
        return this.type;
    }

    public java.lang.String routeTableUuid;
    public void setRouteTableUuid(java.lang.String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }
    public java.lang.String getRouteTableUuid() {
        return this.routeTableUuid;
    }

    public java.lang.String destination;
    public void setDestination(java.lang.String destination) {
        this.destination = destination;
    }
    public java.lang.String getDestination() {
        return this.destination;
    }

    public java.lang.String target;
    public void setTarget(java.lang.String target) {
        this.target = target;
    }
    public java.lang.String getTarget() {
        return this.target;
    }

    public java.lang.Integer distance;
    public void setDistance(java.lang.Integer distance) {
        this.distance = distance;
    }
    public java.lang.Integer getDistance() {
        return this.distance;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
