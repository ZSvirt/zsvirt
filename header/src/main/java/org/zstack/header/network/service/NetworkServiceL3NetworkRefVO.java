package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;

@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass = L3NetworkVO.class, foreignVOJoinColumn = "l3NetworkUuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = L3NetworkVO.class, joinColumn = "l3NetworkUuid"),
        @SoftDeletionCascade(parent = NetworkServiceProviderVO.class, joinColumn = "networkServiceProviderUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = NetworkServiceProviderVO.class, myField = "networkServiceProviderUuid", targetField = "uuid")
        }
)
public class NetworkServiceL3NetworkRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    @ForeignKey(parentEntityClass = NetworkServiceProviderVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String networkServiceProviderUuid;

    @Column
    private String networkServiceType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }

    public String getNetworkServiceType() {
        return networkServiceType;
    }

    public void setNetworkServiceType(String networkServiceType) {
        this.networkServiceType = networkServiceType;
    }
}
