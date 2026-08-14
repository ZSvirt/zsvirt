package org.zstack.network.service.virtualrouter;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VirtualRouterVmVO.class, joinColumn = "uuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class VirtualRouterMetadataVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VirtualRouterVmVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String uuid;

    @Column
    private String zvrVersion;

    @Column
    private String vyosVersion;

    @Column
    private String kernelVersion;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
