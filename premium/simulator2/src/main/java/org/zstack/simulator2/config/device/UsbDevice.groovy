package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by hyz on 2020/12/15.
 */
class UsbDevice extends VO {
    @Col
    String hostIp
    @Col
    String busNum
    @Col
    String devNum
    @Col
    String idVendor
    @Col
    String idProduct
    @Col
    String iManufacturer
    @Col
    String iProduct
    @Col
    String iSerial
    @Col
    String usbVersion
}

