package org.zstack.usbDevice;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by GuoYi on 10/21/17.
 */
@StaticMetamodel(UsbDeviceVO.class)
public class UsbDeviceVO_ extends UsbDeviceAO_ {
    public static volatile SingularAttribute<UsbDeviceVO, String> hostUuid;
    public static volatile SingularAttribute<UsbDeviceVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<UsbDeviceVO, UsbDeviceState> state;
    public static volatile SingularAttribute<UsbDeviceVO, String> attachType;
}
