package org.zstack.usbDevice;

import org.apache.logging.log4j.util.Strings;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/12/12.
 */
@MappedSuperclass
public class UsbDeviceAO extends ResourceVO {
    @Column
    protected String name;

    @Column
    protected String description;

    @Column
    protected String busNum;

    @Column
    protected String devNum;

    @Column
    protected String idVendor;

    @Column
    protected String idProduct;

    @Column
    protected String iManufacturer;

    @Column
    protected String iProduct;

    @Column
    protected String iSerial;

    @Column
    protected String usbVersion;

    @Column
    protected Timestamp createDate;

    @Column
    protected Timestamp lastOpDate;

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
        if (Strings.isNotEmpty(iSerial) && iSerial.length() > 32) {
            iSerial = iSerial.substring(0, 32);
        }

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
