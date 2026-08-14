package org.zstack.pciDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 18:22
 */
public class DefaultPciDeviceFactory implements PciDeviceTypeFactory {

    private static CLogger logger = Utils.getLogger(DefaultPciDeviceFactory.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Override
    public List<PciDeviceType> getTypes() {
        List<PciDeviceType> types = new ArrayList<>();
        types.add(PciDeviceType.GPU_Audio_Controller);
        types.add(PciDeviceType.GPU_USB_Controller);
        types.add(PciDeviceType.GPU_Serial_Controller);
        types.add(PciDeviceType.Audio_Controller);
        types.add(PciDeviceType.USB_Controller);
        types.add(PciDeviceType.Serial_Controller);
        types.add(PciDeviceType.Moxa_Device);
        types.add(PciDeviceType.PCI_Bridge);
        types.add(PciDeviceType.Host_Bridge);
        types.add(PciDeviceType.Generic);
        types.add(PciDeviceType.Custom);
        return types;
    }

    @Override
    public void createPciDevices(List<PciDeviceVO> vos) {
        dbf.persistCollection(vos);
    }

    @Override
    public void updatePciDevices(List<PciDeviceVO> vos) {
        dbf.updateCollection(vos);
    }

    @Override
    public void removePciDevices(List<PciDeviceTO> tos) {
        List<String> garbageUuids = tos.stream().map(PciDeviceTO::getUuid).collect(Collectors.toList());
        if (!garbageUuids.isEmpty()) {
            SQL.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, garbageUuids).hardDelete();
            SQL.New(MdevDeviceVO.class).in(MdevDeviceVO_.parentUuid, garbageUuids).hardDelete();
            logger.debug(String.format("delete garbage pci devices[uuids:%s, desc:%s] in host[uuid:%s]",
                    garbageUuids,
                    tos.stream().map(PciDeviceTO::getDescription).collect(Collectors.toList()),
                    tos.get(0).getHostUuid()));
        }
    }
}
