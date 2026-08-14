package org.zstack.usbDevice;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceTO {
    private String busNum;
    private String devNum;
    private String idVendor;
    private String idProduct;
    private String iManufacturer;
    private String iProduct;
    private String iSerial;
    private String usbVersion;
    private String attachType;

    public UsbDeviceTO(UsbDeviceTO other) {
        this.setBusNum(other.getBusNum());
        this.setDevNum(other.getDevNum());
        this.setIdVendor(other.getIdVendor());
        this.setIdProduct(other.getIdProduct());
        this.setiManufacturer(other.getiManufacturer());
        this.setiProduct(other.getiProduct());
        this.setiSerial(other.getiSerial());
        this.setUsbVersion(other.getUsbVersion());
        this.setAttachType(other.getAttachType());
    }

    public UsbDeviceTO(String[] usbInfo) {
        this.setBusNum(usbInfo[0]);
        this.setDevNum(usbInfo[1]);
        this.setIdVendor(usbInfo[2]);
        this.setIdProduct(usbInfo[3]);
        this.setiManufacturer(usbInfo[4]);
        this.setiProduct(usbInfo[5]);
        this.setiSerial(usbInfo[6]);
        this.setUsbVersion(usbInfo[7]);
    }

    public UsbDeviceTO(UsbDeviceVO vo) {
        this.setBusNum(vo.getBusNum());
        this.setDevNum(vo.getDevNum());
        this.setIdVendor(vo.getIdVendor());
        this.setIdProduct(vo.getIdProduct());
        this.setiManufacturer(vo.getiManufacturer());
        this.setiProduct(vo.getiProduct());
        this.setiSerial(vo.getiSerial());
        this.setUsbVersion(vo.getUsbVersion());
        this.setAttachType(vo.getAttachType());
    }

    public UsbDeviceTO(UsbDeviceInventory vo) {
        this.setBusNum(vo.getBusNum());
        this.setDevNum(vo.getDevNum());
        this.setIdVendor(vo.getIdVendor());
        this.setIdProduct(vo.getIdProduct());
        this.setiManufacturer(vo.getiManufacturer());
        this.setiProduct(vo.getiProduct());
        this.setiSerial(vo.getiSerial());
        this.setUsbVersion(vo.getUsbVersion());
        this.setAttachType(vo.getAttachType());
    }

    public static UsbDeviceTO valueOf(UsbDeviceVO vo) {
        return new UsbDeviceTO(vo);
    }

    public static UsbDeviceTO valueOf(UsbDeviceInventory usbDevice) {
        return new UsbDeviceTO(usbDevice);
    }

    public static List<UsbDeviceTO> valueOf(List<UsbDeviceVO> vos) {
        List<UsbDeviceTO> tos = new ArrayList<>();
        for (UsbDeviceVO vo : vos) {
            tos.add(new UsbDeviceTO(vo));
        }
        return tos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsbDeviceTO that = (UsbDeviceTO) o;

        if (busNum != null ? !busNum.equals(that.busNum) : that.busNum != null) return false;
        if (devNum != null ? !devNum.equals(that.devNum) : that.devNum != null) return false;
        if (idVendor != null ? !idVendor.equals(that.idVendor) : that.idVendor != null) return false;
        if (idProduct != null ? !idProduct.equals(that.idProduct) : that.idProduct != null) return false;
        if (iManufacturer != null ? !iManufacturer.equals(that.iManufacturer) : that.iManufacturer != null)
            return false;
        if (iProduct != null ? !iProduct.equals(that.iProduct) : that.iProduct != null) return false;
        if (iSerial != null ? !iSerial.equals(that.iSerial) : that.iSerial != null) return false;
        return usbVersion != null ? usbVersion.equals(that.usbVersion) : that.usbVersion == null;
    }

    @Override
    public int hashCode() {
        int result = busNum != null ? busNum.hashCode() : 0;
        result = 31 * result + (devNum != null ? devNum.hashCode() : 0);
        result = 31 * result + (idVendor != null ? idVendor.hashCode() : 0);
        result = 31 * result + (idProduct != null ? idProduct.hashCode() : 0);
        result = 31 * result + (iManufacturer != null ? iManufacturer.hashCode() : 0);
        result = 31 * result + (iProduct != null ? iProduct.hashCode() : 0);
        result = 31 * result + (iSerial != null ? iSerial.hashCode() : 0);
        result = 31 * result + (usbVersion != null ? usbVersion.hashCode() : 0);
        return result;
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

    public String getAttachType() {
        return attachType;
    }

    public void setAttachType(String attachType) {
        this.attachType = attachType;
    }
}
