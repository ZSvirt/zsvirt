package org.zstack.simulator2.config.device

import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO

/**
 * Created by hyz on 2020/12/15.
 */
class PciDevice extends VO {
    @Col
    String name
    @Col
    String description
    @Col
    String hostIp
    @Col
    String vendorId
    @Col
    String deviceId
    @Col
    String subvendorId
    @Col
    String subdeviceId
    @Col
    String pciDeviceAddress
    @Col
    String parentAddress
    @Col
    String iommuGroup
    @Col
    String type
    @Col
    String virtStatus
    @Col
    int maxPartNum
    @Col
    String ramSize
    @Col
    String deviceSpecTypeIds = "0"
}
