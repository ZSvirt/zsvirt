package org.zstack.header.network.service;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = NetworkServiceProviderVO.class, myField = "networkServiceProviderUuid", targetField = "uuid")
        }
)
public class NetworkServiceTypeVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = NetworkServiceProviderVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String networkServiceProviderUuid;

    @Column
    private String type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
