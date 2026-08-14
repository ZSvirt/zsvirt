package org.zstack.usbDevice;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(UsbDeviceAO.class)
public class UsbDeviceAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<UsbDeviceVO, String> name;
    public static volatile SingularAttribute<UsbDeviceVO, String> description;
    public static volatile SingularAttribute<UsbDeviceVO, String> busNum;
    public static volatile SingularAttribute<UsbDeviceVO, String> devNum;
    public static volatile SingularAttribute<UsbDeviceVO, String> idVendor;
    public static volatile SingularAttribute<UsbDeviceVO, String> idProduct;
    public static volatile SingularAttribute<UsbDeviceVO, String> iManufacturer;
    public static volatile SingularAttribute<UsbDeviceVO, String> iProduct;
    public static volatile SingularAttribute<UsbDeviceVO, String> iSerial;
    public static volatile SingularAttribute<UsbDeviceVO, String> usbVersion;
    public static volatile SingularAttribute<UsbDeviceVO, Timestamp> createDate;
    public static volatile SingularAttribute<UsbDeviceVO, Timestamp> lastOpDate;
}
