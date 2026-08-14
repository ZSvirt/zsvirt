package org.zstack.kvm;

import org.zstack.header.image.ImageInventory;

public class KVMIsoTO extends ImageInventory {
    private String pathInCache;
    private String installUrl;
    
    public KVMIsoTO(ImageInventory inv) {
        this.setCreateDate(inv.getCreateDate());
        this.setDescription(inv.getDescription());
        this.setMediaType(inv.getMediaType());
        this.setGuestOsType(inv.getGuestOsType());
        this.setLastOpDate(inv.getLastOpDate());
        this.setMd5Sum(inv.getMd5Sum());
        this.setName(inv.getName());
        this.setSize(inv.getSize());
        this.setActualSize(inv.getActualSize());
        this.setState(inv.getStatus());
        this.setType(inv.getType());
        this.setUrl(inv.getUrl());
        this.setUuid(inv.getUuid());
    }

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    public String getPathInCache() {
        return pathInCache;
    }

    public void setPathInCache(String pathInCache) {
        this.pathInCache = pathInCache;
    }
}
