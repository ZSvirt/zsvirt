package org.zstack.sdk;



public class VpcHaGroupInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.util.List monitors;
    public void setMonitors(java.util.List monitors) {
        this.monitors = monitors;
    }
    public java.util.List getMonitors() {
        return this.monitors;
    }

    public java.util.List vrRefs;
    public void setVrRefs(java.util.List vrRefs) {
        this.vrRefs = vrRefs;
    }
    public java.util.List getVrRefs() {
        return this.vrRefs;
    }

    public java.util.List services;
    public void setServices(java.util.List services) {
        this.services = services;
    }
    public java.util.List getServices() {
        return this.services;
    }

    public java.util.List usedIps;
    public void setUsedIps(java.util.List usedIps) {
        this.usedIps = usedIps;
    }
    public java.util.List getUsedIps() {
        return this.usedIps;
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
