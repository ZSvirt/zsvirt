package org.zstack.vrouterRoute;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;

/**
 * Created by weiwang on 24/06/2017.
 */
@PythonClassInventory
public class VRouterRouteEntryAO implements Serializable {
    private String destination;
    private String target;
    private VRouterRouteEntryType type;
    private String status;
    private Integer distance;
    private String uuid;
    private String description;

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

    public VRouterRouteEntryType getType() {
        return type;
    }

    public void setType(VRouterRouteEntryType type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

