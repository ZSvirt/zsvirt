package org.zstack.pciDevice.specification.pci;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.pciDevice.PciDeviceType;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-03-05.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PciDeviceSpecVO.class)
public class PciDeviceSpecInventory {
    private String uuid;
    private String name;
    private String description;
    private String vendorId;

    private String vendor;
    private String deviceId;

    private String device;
    private String subvendorId;
    private String subdeviceId;
    private String ramSize;
    private Integer maxPartNum;
    private PciDeviceType type;
    private PciDeviceSpecState state;
    private Boolean isVirtual;
    @APINoSee
    @NoJsonSchema
    @GsonTransient
    private String romContent;
    private String romVersion;
    private String romMd5sum;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public PciDeviceSpecInventory() {
    }

    public PciDeviceSpecInventory(PciDeviceSpecVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.vendorId = vo.getVendorId();
        this.vendor = vo.getVendor();
        this.deviceId = vo.getDeviceId();
        this.device = vo.getDevice();
        this.subvendorId = vo.getSubvendorId();
        this.subdeviceId = vo.getSubdeviceId();
        this.ramSize = vo.getRamSize();
        this.maxPartNum = vo.getMaxPartNum();
        this.type = vo.getType();
        this.state = vo.getState();
        this.isVirtual = vo.isVirtual();
        this.romContent = vo.getRomContent();
        this.romVersion = vo.getRomVersion();
        this.romMd5sum = vo.getRomMd5sum();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static PciDeviceSpecInventory valueOf(PciDeviceSpecVO vo) {
        return new PciDeviceSpecInventory(vo);
    }

    public static List<PciDeviceSpecInventory> valueOf(Collection<PciDeviceSpecVO> vos) {
        return vos.stream().map(PciDeviceSpecInventory::valueOf).collect(Collectors.toList());
    }

    public static PciDeviceSpecInventory __example__() {
        PciDeviceSpecInventory inv = new PciDeviceSpecInventory();
        inv.setUuid(DocUtils.createFixedUuid(PciDeviceSpecVO.class));
        inv.setName("MSI_GTX1060");
        inv.setDescription("NVIDIA Corporation, GP106 [GeForce GTX 1060 6GB], a1, VGA compatible controller");
        inv.setVendorId("10de");
        inv.setVendorId("NVIDIA Corporation [10de]");
        inv.setDeviceId("1c03");
        inv.setDevice("TU102GL [Quadro RTX 6000/8000] [1e30]");
        inv.setSubvendorId("1462");
        inv.setSubdeviceId("3283");
        inv.setRomVersion("86.06.0E.00.28");
        inv.setType(PciDeviceType.GPU_Video_Controller);
        inv.setState(PciDeviceSpecState.Enabled);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
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

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getRamSize() {
        return ramSize;
    }

    public void setRamSize(String ramSize) {
        this.ramSize = ramSize;
    }

    public Integer getMaxPartNum() {
        return maxPartNum;
    }

    public void setMaxPartNum(Integer maxPartNum) {
        this.maxPartNum = maxPartNum;
    }

    public PciDeviceType getType() {
        return type;
    }

    public void setType(PciDeviceType type) {
        this.type = type;
    }

    public PciDeviceSpecState getState() {
        return state;
    }

    public void setState(PciDeviceSpecState state) {
        this.state = state;
    }

    public Boolean getVirtual() {
        return isVirtual;
    }

    public void setVirtual(Boolean virtual) {
        isVirtual = virtual;
    }

    public String getRomContent() {
        return romContent;
    }

    public void setRomContent(String romContent) {
        this.romContent = romContent;
    }

    public String getRomVersion() {
        return romVersion;
    }

    public void setRomVersion(String romVersion) {
        this.romVersion = romVersion;
    }

    public String getRomMd5sum() {
        return romMd5sum;
    }

    public void setRomMd5sum(String romMd5sum) {
        this.romMd5sum = romMd5sum;
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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
