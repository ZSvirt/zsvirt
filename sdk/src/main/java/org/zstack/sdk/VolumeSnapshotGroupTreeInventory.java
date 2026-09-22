package org.zstack.sdk;



public class VolumeSnapshotGroupTreeInventory  {

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

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
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

    public boolean current;
    public void setCurrent(boolean current) {
        this.current = current;
    }
    public boolean getCurrent() {
        return this.current;
    }

    public boolean incomplete;
    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }
    public boolean getIncomplete() {
        return this.incomplete;
    }

    public java.lang.String parentGroupUuid;
    public void setParentGroupUuid(java.lang.String parentGroupUuid) {
        this.parentGroupUuid = parentGroupUuid;
    }
    public java.lang.String getParentGroupUuid() {
        return this.parentGroupUuid;
    }

    public java.util.List parentGroupUuids;
    public void setParentGroupUuids(java.util.List parentGroupUuids) {
        this.parentGroupUuids = parentGroupUuids;
    }
    public java.util.List getParentGroupUuids() {
        return this.parentGroupUuids;
    }

    public java.util.List children;
    public void setChildren(java.util.List children) {
        this.children = children;
    }
    public java.util.List getChildren() {
        return this.children;
    }

    public java.util.List refs;
    public void setRefs(java.util.List refs) {
        this.refs = refs;
    }
    public java.util.List getRefs() {
        return this.refs;
    }

}
