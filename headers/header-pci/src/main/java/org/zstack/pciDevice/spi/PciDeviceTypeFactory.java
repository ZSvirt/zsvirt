package org.zstack.pciDevice.spi;

import org.zstack.pciDevice.entity.PciDeviceVO;
import org.zstack.pciDevice.header.PciDeviceType;
import org.zstack.pciDevice.message.PciDeviceTO;

import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 18:18
 */
public interface PciDeviceTypeFactory {
    List<PciDeviceType> getTypes();

    void createPciDevices(List<PciDeviceVO> vos);

    void updatePciDevices(List<PciDeviceVO> vos);

    void removePciDevices(List<PciDeviceTO> tos);
}
