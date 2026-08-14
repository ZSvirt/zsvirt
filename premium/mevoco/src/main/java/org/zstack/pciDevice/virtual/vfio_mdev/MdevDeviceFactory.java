package org.zstack.pciDevice.virtual.vfio_mdev;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecInventory;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefVO;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefVO_;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesMsg;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesMsg;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.virtual.VirtualPciDeviceFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 2019-04-24.
 */
public class MdevDeviceFactory implements VirtualPciDeviceFactory {
    private static CLogger logger = Utils.getLogger(MdevDeviceFactory.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PciDeviceManager pciMgr;

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_VFIO_MDEV;
    }

    @Override
    public void generateVirtualPciDevices(APIGenerateVirtualPciDevicesMsg msg, Completion completion) {
        APIGenerateMdevDevicesMsg gmsg = (APIGenerateMdevDevicesMsg) msg;
        PciDeviceInventory pciDevice = PciDeviceInventory.valueOf(dbf.findByUuid(gmsg.getPciDeviceUuid(), PciDeviceVO.class));
        MdevDeviceSpecInventory mdevSpec = dbf.findByUuid(gmsg.getMdevSpecUuid(), MdevDeviceSpecVO.class).toInventory();
        PciDeviceBackend bkd = pciMgr.getPciDeviceBackendByHostUuid(pciDevice.getHostUuid());
        bkd.generateVfioMdevDevices(pciDevice.getHostUuid(), pciDevice, mdevSpec, null, new Completion(msg) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void ungenerateVirtualPciDevices(APIUngenerateVirtualPciDevicesMsg msg, Completion completion) {
        APIUngenerateMdevDevicesMsg gmsg = (APIUngenerateMdevDevicesMsg) msg;
        PciDeviceInventory pciDevice = PciDeviceInventory.valueOf(dbf.findByUuid(gmsg.getPciDeviceUuid(), PciDeviceVO.class));
        String mdevSpecUuid = Q.New(PciDeviceMdevSpecRefVO.class)
                .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, pciDevice.getUuid())
                .eq(PciDeviceMdevSpecRefVO_.effective, true)
                .select(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                .findValue();
        if (StringUtils.isBlank(mdevSpecUuid)) {
            completion.fail(Platform.operr(
                    "pci device[uuid:%s] is known as %s, but cannot find it's mdev spec, so abort.",
                    pciDevice.getUuid(), PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED));
            return;
        }

        MdevDeviceSpecInventory mdevSpec = dbf.findByUuid(mdevSpecUuid, MdevDeviceSpecVO.class).toInventory();
        PciDeviceBackend bkd = pciMgr.getPciDeviceBackendByHostUuid(pciDevice.getHostUuid());
        bkd.unGenerateVfioMdevDevices(pciDevice.getHostUuid(), pciDevice, mdevSpec, gmsg.getMdevUuids(), new Completion(msg) {
            @Override
            public void success() {
                SQL.New(PciDeviceMdevSpecRefVO.class)
                        .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, pciDevice.getUuid())
                        .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, mdevSpecUuid)
                        .set(PciDeviceMdevSpecRefVO_.effective, false)
                        .update();
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
