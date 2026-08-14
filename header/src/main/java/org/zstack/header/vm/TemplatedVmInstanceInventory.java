package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = TemplatedVmInstanceVO.class)
public class TemplatedVmInstanceInventory implements Serializable {
    private String uuid;

    @Queryable(mappingClass = VmInstanceInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "name"))
    private String name;

    @Queryable(mappingClass = VmInstanceInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "zoneUuid"))
    private String zoneUuid;

    @Queryable(mappingClass = VmInstanceInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "accountUuid"))
    private String accountUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public TemplatedVmInstanceInventory() {
    }

    public TemplatedVmInstanceInventory(TemplatedVmInstanceInventory other) {
        this.uuid = other.getUuid();
        this.name = other.getName();
        this.zoneUuid = other.getZoneUuid();
        this.accountUuid = other.getAccountUuid();
        this.createDate = other.getCreateDate();
        this.lastOpDate = other.getLastOpDate();
    }

    public static TemplatedVmInstanceInventory valueOf(TemplatedVmInstanceVO vo) {
        TemplatedVmInstanceInventory inventory = new TemplatedVmInstanceInventory();
        inventory.setUuid(vo.getUuid());
        if (vo.getVm() != null) {
            inventory.setName(vo.getVm().getName());
            inventory.setZoneUuid(vo.getVm().getZoneUuid());
            inventory.setAccountUuid(vo.getVm().getAccountUuid());
        }
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<TemplatedVmInstanceInventory> valueOf(Collection<TemplatedVmInstanceVO> vos) {
        return vos.stream().map(TemplatedVmInstanceInventory::valueOf).collect(Collectors.toList());
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

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
}
