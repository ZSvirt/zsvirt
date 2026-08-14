package org.zstack.usbDevice;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by GuoYi on 10/21/17.
 */
public interface UsbDeviceBackend {
    HypervisorType getHypervisorType();

    void attachUsbDeviceToVm(UsbDeviceInventory usbInv, String vmInstanceUuid, String attachType, Completion completion);

    void detachUsbDeviceFromVm(UsbDeviceInventory usbInv, String vmInstanceUuid, Completion completion);

    void syncUsbDeviceFromHost(String hostUuid, NoErrorCompletion completion);

    void startUsbRedirectServer(UsbDeviceInventory usbInv, String port, ReturnValueCompletion<String> completion);

    void reloadRedirectUsb(VmInstanceInventory inv, String hostUuid);

    void closeRedirectPortIfNeeded(UsbDeviceInventory usbInv, Completion completion);
}
