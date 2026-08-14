package org.zstack.usbDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.allocator.DesignatedHostAllocatorStrategyFactory;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceAllocatorFactory extends DesignatedHostAllocatorStrategyFactory
        implements HostAllocatorFilterExtensionPoint, HostAllocatorStrategyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(UsbDeviceAllocatorFactory.class);
    private HostAllocatorStrategyType type = new HostAllocatorStrategyType(UsbDeviceConstants.USB_DEVICE_ALLOCATOR_STRATEGY, false);

    @Autowired
    private ErrorFacade errf;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        List<UsbDeviceVO> usbDeviceVOS = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, spec.getVmInstance().getUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.PassThrough.toString())
                .list();
        if (usbDeviceVOS.isEmpty()) {
            return;
        }

        String hostTheUsbOn = usbDeviceVOS.get(0).getHostUuid();
        logger.debug(String.format("the vm[uuid:%s] attached USB devices[%s]",
                spec.getVmInstance().getUuid(),
                transform(usbDeviceVOS, UsbDeviceVO::getUuid)));
        for (HostCandidate candidate : candidates) {
            if (!hostTheUsbOn.equals(candidate.getUuid())) {
                candidate.markAsRejected(getClass(), i18m("the specific USB devices required"));
            }
        }
    }

    @Override
    public String getHostAllocatorStrategyName(HostAllocatorSpec spec) {
        List<UsbDeviceVO> usbDeviceVOS = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, spec.getVmInstance().getUuid())
                .list();

        if (usbDeviceVOS == null || usbDeviceVOS.isEmpty()) {
            return null;
        }

        return UsbDeviceConstants.USB_DEVICE_ALLOCATOR_STRATEGY;
    }

    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }
}