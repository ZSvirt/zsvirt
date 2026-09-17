package org.zstack.header.storage.snapshot.group;

import org.zstack.header.query.Unqueryable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VolumeSnapshotGroupTreeInventory {
    private String uuid;
    private String name;
    private String description;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Unqueryable
    private boolean current;
    @Unqueryable
    private boolean incomplete;
    @Unqueryable
    private String parentGroupUuid;
    @Unqueryable
    private List<String> parentGroupUuids = new ArrayList<>();
    @Unqueryable
    private List<VolumeSnapshotGroupTreeInventory> children = new ArrayList<>();
    @Unqueryable
    private List<VolumeSnapshotGroupTreeRefInventory> refs = new ArrayList<>();

    public static VolumeSnapshotGroupTreeInventory valueOf(VolumeSnapshotGroupVO vo) {
        VolumeSnapshotGroupTreeInventory inv = new VolumeSnapshotGroupTreeInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VolumeSnapshotGroupTreeInventory> valueOf(Collection<VolumeSnapshotGroupVO> vos) {
        return vos.stream().map(VolumeSnapshotGroupTreeInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public String getParentGroupUuid() {
        return parentGroupUuid;
    }

    public void setParentGroupUuid(String parentGroupUuid) {
        this.parentGroupUuid = parentGroupUuid;
    }

    public List<String> getParentGroupUuids() {
        return parentGroupUuids;
    }

    public void setParentGroupUuids(List<String> parentGroupUuids) {
        this.parentGroupUuids = parentGroupUuids;
    }

    public List<VolumeSnapshotGroupTreeInventory> getChildren() {
        return children;
    }

    public void setChildren(List<VolumeSnapshotGroupTreeInventory> children) {
        this.children = children;
    }

    public List<VolumeSnapshotGroupTreeRefInventory> getRefs() {
        return refs;
    }

    public void setRefs(List<VolumeSnapshotGroupTreeRefInventory> refs) {
        this.refs = refs;
    }
}
