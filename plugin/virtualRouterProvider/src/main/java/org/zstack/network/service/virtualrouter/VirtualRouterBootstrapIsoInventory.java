package org.zstack.network.service.virtualrouter;

import java.io.Serializable;

public class VirtualRouterBootstrapIsoInventory implements Serializable {
    private long id;
    private String virtualRouterUuid;
    private String isoPath;
    
    public static VirtualRouterBootstrapIsoInventory valueOf(VirtualRouterBootstrapIsoVO vo) {
    	VirtualRouterBootstrapIsoInventory inv = new VirtualRouterBootstrapIsoInventory();
    	inv.setId(vo.getId());
    	inv.setIsoPath(vo.getIsoPath());
    	inv.setVirtualRouterUuid(vo.getVirtualRouterUuid());
    	return inv;
    }
    
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getVirtualRouterUuid() {
		return virtualRouterUuid;
	}
	public void setVirtualRouterUuid(String virtualRouterUuid) {
		this.virtualRouterUuid = virtualRouterUuid;
	}
	public String getIsoPath() {
		return isoPath;
	}
	public void setIsoPath(String isoPath) {
		this.isoPath = isoPath;
	}
}
