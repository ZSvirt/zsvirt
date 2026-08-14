package org.zstack.network.service.virtualrouter;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
public class VirtualRouterBootstrapIsoVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String virtualRouterUuid;
    
    @Column
    private String isoPath;

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
