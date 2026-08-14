package org.zstack.usbDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
@PythonClassInventory
@Inventory(mappingVOClass = UsbDeviceVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
})
public class UsbDeviceInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String hostUuid;
    private String vmInstanceUuid;
    private UsbDeviceState state;
    private String busNum;
    private String devNum;
    private String idVendor;
    private String idProduct;
    private String iManufacturer;
    private String iProduct;
    private String iSerial;
    private String usbVersion;
    private String attachType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public UsbDeviceInventory() {
    }

    public UsbDeviceInventory(UsbDeviceVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setHostUuid(vo.getHostUuid());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setState(vo.getState());
        this.setBusNum(vo.getBusNum());
        this.setDevNum(vo.getDevNum());
        this.setIdVendor(vo.getIdVendor());
        this.setIdProduct(vo.getIdProduct());
        this.setiManufacturer(vo.getiManufacturer());
        this.setiProduct(vo.getiProduct());
        this.setiSerial(vo.getiSerial());
        this.setUsbVersion(vo.getUsbVersion());
        this.setAttachType(vo.getAttachType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static UsbDeviceInventory valueOf(UsbDeviceVO vo) {
        return new UsbDeviceInventory(vo);
    }

    public static List<UsbDeviceInventory> valueOf(Collection<UsbDeviceVO> vos) {
        List<UsbDeviceInventory> invs = new ArrayList<>();
        for (UsbDeviceVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getAttachType() {
        return attachType;
    }

    public void setAttachType(String attachType) {
        this.attachType = attachType;
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public UsbDeviceState getState() {
        return state;
    }

    public void setState(UsbDeviceState state) {
        this.state = state;
    }

    public String getBusNum() {
        return busNum;
    }

    public void setBusNum(String busNum) {
        this.busNum = busNum;
    }

    public String getDevNum() {
        return devNum;
    }

    public void setDevNum(String devNum) {
        this.devNum = devNum;
    }

    public String getIdVendor() {
        return idVendor;
    }

    public void setIdVendor(String idVendor) {
        this.idVendor = idVendor;
    }

    public String getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(String idProduct) {
        this.idProduct = idProduct;
    }

    public String getiManufacturer() {
        return iManufacturer;
    }

    public void setiManufacturer(String iManufacturer) {
        this.iManufacturer = iManufacturer;
    }

    public String getiProduct() {
        return iProduct;
    }

    public void setiProduct(String iProduct) {
        this.iProduct = iProduct;
    }

    public String getiSerial() {
        return iSerial;
    }

    public void setiSerial(String iSerial) {
        this.iSerial = iSerial;
    }

    public String getUsbVersion() {
        return usbVersion;
    }

    public void setUsbVersion(String usbVersion) {
        this.usbVersion = usbVersion;
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
