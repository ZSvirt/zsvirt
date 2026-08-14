package org.zstack.network.service.virtualrouter;

public class VirtualRouterMetadataStruct {
    private String vrUuid;
    private String zvrVersion;
    private String vyosVersion;
    private String kernelVersion;

    public String getVrUuid() {
        return vrUuid;
    }

    public void setVrUuid(String vrUuid) {
        this.vrUuid = vrUuid;
    }

    public String getZvrVersion() {
        return zvrVersion;
    }

    public void setZvrVersion(String zvrVersion) {
        this.zvrVersion = zvrVersion;
    }

    public String getVyosVersion() {
        return vyosVersion;
    }

    public void setVyosVersion(String vyosVersion) {
        this.vyosVersion = vyosVersion;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

}
