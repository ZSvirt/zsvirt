package org.zstack.header.storageDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = LunVO.class)
public class LunInventory implements Serializable {
    private String name;

    private String uuid;

    private String wwid;

    private String vendor;

    private String model;

    private String wwn;

    private String serial;

    private String type;

    @Deprecated
    private String hctl;

    @Deprecated
    private String path;

    private String state;

    private Long size;

    private String multipathDeviceUuid;

    private String source;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LunInventory)) return false;

        LunInventory that = (LunInventory) o;
        if (getSerial() != null && getWwid() != null) {
            return getSerial().equals(that.getSerial()) && getWwid().equals(that.getWwid());
        } else if (getSerial() != null) {
            return getSerial().equals(that.getSerial());
        } else if (getWwid() != null) {
            return getWwid().equals(that.getWwid());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getSerial() == null ? 0 : getSerial().hashCode();
    }

    public LunInventory() {
    }

    public LunInventory(LunAO ao) {
        this.setName(ao.getName());
        this.setUuid(ao.getUuid());
        this.setWwid(ao.getWwid());
        this.setVendor(ao.getVendor());
        this.setModel(ao.getModel());
        this.setWwn(ao.getWwn());
        this.setSerial(ao.getSerial());
        this.setType(ao.getType());
        this.setHctl(ao.getHctl());
        this.setPath(ao.getPath());
        this.setSize(ao.getSize());
        this.setState(ao.getState());
        this.setMultipathDeviceUuid(ao.getMultipathDeviceUuid());
        this.setSource(ao.getSource());
        this.setCreateDate(ao.getCreateDate());
        this.setLastOpDate(ao.getLastOpDate());
    }

    public static LunInventory valueOf(LunVO vo) {
        return new LunInventory(vo);
    }

    public static List<LunInventory> valueOf(Collection<? extends LunVO> vos) {
        return vos.stream().map(LunInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMultipathDeviceUuid() {
        return multipathDeviceUuid;
    }

    public void setMultipathDeviceUuid(String multipathDeviceUuid) {
        this.multipathDeviceUuid = multipathDeviceUuid;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
