package org.zstack.header.vm.metadata;

public class ResourceMetadata {
    private String resourceUuid;
    private String vo;
    private String systemTags;
    private String resourceConfigs;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getVo() {
        return vo;
    }

    public void setVo(String vo) {
        this.vo = vo;
    }

    public String getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(String systemTags) {
        this.systemTags = systemTags;
    }

    public String getResourceConfigs() {
        return resourceConfigs;
    }

    public void setResourceConfigs(String resourceConfigs) {
        this.resourceConfigs = resourceConfigs;
    }
}
