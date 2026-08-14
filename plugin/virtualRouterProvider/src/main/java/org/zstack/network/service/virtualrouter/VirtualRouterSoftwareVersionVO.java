package org.zstack.network.service.virtualrouter;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
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
public class VirtualRouterSoftwareVersionVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VirtualRouterVmVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String uuid;

    @Column
    private String softwareName;

    @Column
    private String currentVersion;

    @Column
    private String latestVersion;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String ipsecCurrentVersion) {
        this.currentVersion = ipsecCurrentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String ipsecLatestVersion) {
        this.latestVersion = ipsecLatestVersion;
    }
}