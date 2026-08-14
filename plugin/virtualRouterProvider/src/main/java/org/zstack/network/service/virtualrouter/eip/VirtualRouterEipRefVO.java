package org.zstack.network.service.virtualrouter.eip;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.virtualrouter.VirtualRouter;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VirtualRouterVmVO.class, joinColumn = "virtualRouterVmUuid"),
        @SoftDeletionCascade(parent = EipVO.class, joinColumn = "eipUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = EipVO.class, myField = "eipUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "virtualRouterVmUuid", targetField = "uuid"),
        }
)
public class VirtualRouterEipRefVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = EipVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String eipUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String virtualRouterVmUuid;

    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
