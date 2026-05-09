package org.zstack.sdk;



public class VolumeTO extends org.zstack.sdk.BaseVirtualDeviceTO {

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public int deviceId;
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
    public int getDeviceId() {
        return this.deviceId;
    }

    public java.lang.String deviceType;
    public void setDeviceType(java.lang.String deviceType) {
        this.deviceType = deviceType;
    }
    public java.lang.String getDeviceType() {
        return this.deviceType;
    }

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public boolean useVirtio;
    public void setUseVirtio(boolean useVirtio) {
        this.useVirtio = useVirtio;
    }
    public boolean getUseVirtio() {
        return this.useVirtio;
    }

    public boolean useVirtioSCSI;
    public void setUseVirtioSCSI(boolean useVirtioSCSI) {
        this.useVirtioSCSI = useVirtioSCSI;
    }
    public boolean getUseVirtioSCSI() {
        return this.useVirtioSCSI;
    }

    public boolean useSCSI;
    public void setUseSCSI(boolean useSCSI) {
        this.useSCSI = useSCSI;
    }
    public boolean getUseSCSI() {
        return this.useSCSI;
    }

    public boolean shareable;
    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }
    public boolean getShareable() {
        return this.shareable;
    }

    public java.lang.String cacheMode;
    public void setCacheMode(java.lang.String cacheMode) {
        this.cacheMode = cacheMode;
    }
    public java.lang.String getCacheMode() {
        return this.cacheMode;
    }

    public boolean aioNative;
    public void setAioNative(boolean aioNative) {
        this.aioNative = aioNative;
    }
    public boolean getAioNative() {
        return this.aioNative;
    }

    public java.lang.String wwn;
    public void setWwn(java.lang.String wwn) {
        this.wwn = wwn;
    }
    public java.lang.String getWwn() {
        return this.wwn;
    }

    public int bootOrder;
    public void setBootOrder(int bootOrder) {
        this.bootOrder = bootOrder;
    }
    public int getBootOrder() {
        return this.bootOrder;
    }

    public int physicalBlockSize;
    public void setPhysicalBlockSize(int physicalBlockSize) {
        this.physicalBlockSize = physicalBlockSize;
    }
    public int getPhysicalBlockSize() {
        return this.physicalBlockSize;
    }

    public java.lang.String type;
    public void setType(java.lang.String type) {
        this.type = type;
    }
    public java.lang.String getType() {
        return this.type;
    }

    public java.lang.String format;
    public void setFormat(java.lang.String format) {
        this.format = format;
    }
    public java.lang.String getFormat() {
        return this.format;
    }

    public java.lang.String primaryStorageType;
    public void setPrimaryStorageType(java.lang.String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }
    public java.lang.String getPrimaryStorageType() {
        return this.primaryStorageType;
    }

    public java.lang.String multiQueues;
    public void setMultiQueues(java.lang.String multiQueues) {
        this.multiQueues = multiQueues;
    }
    public java.lang.String getMultiQueues() {
        return this.multiQueues;
    }

    public int ioThreadId;
    public void setIoThreadId(int ioThreadId) {
        this.ioThreadId = ioThreadId;
    }
    public int getIoThreadId() {
        return this.ioThreadId;
    }

    public java.lang.String ioThreadPin;
    public void setIoThreadPin(java.lang.String ioThreadPin) {
        this.ioThreadPin = ioThreadPin;
    }
    public java.lang.String getIoThreadPin() {
        return this.ioThreadPin;
    }

    public int controllerIndex;
    public void setControllerIndex(int controllerIndex) {
        this.controllerIndex = controllerIndex;
    }
    public int getControllerIndex() {
        return this.controllerIndex;
    }

    public java.lang.String luksSecretUuid;
    public void setLuksSecretUuid(java.lang.String luksSecretUuid) {
        this.luksSecretUuid = luksSecretUuid;
    }
    public java.lang.String getLuksSecretUuid() {
        return this.luksSecretUuid;
    }

}
