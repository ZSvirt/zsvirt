package org.zstack.vrouterRoute;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import javax.persistence.*;

/**
 * Created by weiwang on 15/06/2017.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VirtualRouterVmVO.class, joinColumn = "virtualRouterVmUuid"),
        @SoftDeletionCascade(parent = VRouterRouteTableVO.class, joinColumn = "routeTableUuid")
})
public class VirtualRouterVRouterRouteTableRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String virtualRouterVmUuid;

    @Column
    @ForeignKey(parentEntityClass = VRouterRouteTableVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)

    private String routeTableUuid;

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
