package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.mttyDevice.MttyDeviceConstants;
import org.zstack.pciDevice.PciDeviceConstants;

/**
 * Created by GuoYi on 2019/5/12.
 */
public enum MdevDeviceType {
    SE_Controller(MttyDeviceConstants.SE_Controller),
    GPU_Video_Controller(PciDeviceConstants.GPU_Video_Controller),
    GPU_3D_Controller(PciDeviceConstants.GPU_3D_Controller);

    private String value;

    MdevDeviceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isEqual(String str) {
        return this.value.equals(str);
    }
}
