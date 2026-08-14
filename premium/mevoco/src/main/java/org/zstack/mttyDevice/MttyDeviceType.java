package org.zstack.mttyDevice;

/**
 * @author yu.sun
 * @date 2022/11/16 17:41
 **/
public enum MttyDeviceType {
    SE_Controller(MttyDeviceConstants.SE_Controller);

    private String value;

    MttyDeviceType(String value) {
        this.value = value;
    }

    public boolean isEqual(String str) {
        return this.value.equals(str);
    }
}