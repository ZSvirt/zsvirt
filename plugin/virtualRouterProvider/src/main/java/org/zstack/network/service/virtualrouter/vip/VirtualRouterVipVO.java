package org.zstack.network.service.virtualrouter.vip;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import javax.persistence.*;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VirtualRouterVmVO.class, joinColumn = "virtualRouterVmUuid"),
        @SoftDeletionCascade(parent = VipVO.class, joinColumn = "uuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "virtualRouterVmUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VipVO.class, myField = "uuid", targetField = "uuid"),
        }
)
public class VirtualRouterVipVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String virtualRouterVmUuid;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }
}
