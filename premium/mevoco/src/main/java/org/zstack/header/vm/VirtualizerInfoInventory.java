package org.zstack.header.vm;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 23/02/21
 */
public class VirtualizerInfoInventory {
    private String uuid;
    private String resourceType;
    private List<VirtualizerInfo> infoList;
    private List<String> error;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public List<VirtualizerInfo> getInfoList() {
        return infoList;
    }

    public void setInfoList(List<VirtualizerInfo> infoList) {
        this.infoList = infoList;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }
}
