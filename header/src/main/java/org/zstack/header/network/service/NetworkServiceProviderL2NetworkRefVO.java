package org.zstack.header.network.service;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;

@Entity
@Table
@SqlTrigger(foreignVOClass = NetworkServiceProviderVO.class, foreignVOJoinColumn = "networkServiceProviderUuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = L2NetworkVO.class, joinColumn = "l2NetworkUuid"),
        @SoftDeletionCascade(parent = NetworkServiceProviderVO.class, joinColumn = "networkServiceProviderUuid")
})
public class NetworkServiceProviderL2NetworkRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = NetworkServiceProviderVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String networkServiceProviderUuid;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l2NetworkUuid;


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

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
}
