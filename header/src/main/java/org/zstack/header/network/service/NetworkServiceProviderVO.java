package org.zstack.header.network.service;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = NetworkServiceProviderL2NetworkRefVO.class, myField = "uuid", targetField = "networkServiceProviderUuid"),
                @EntityGraph.Neighbour(type = NetworkServiceTypeVO.class, myField = "uuid", targetField = "networkServiceProviderUuid")
        }
)
public class NetworkServiceProviderVO {
    @Id
    @Column
    private String uuid;

    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "NetworkServiceTypeVO",
            joinColumns = @JoinColumn(name = "networkServiceProviderUuid")
    )
    @Column(name = "type")
    private Set<String> networkServiceTypes = new HashSet<String>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "networkServiceProviderUuid", insertable = false, updatable = false)
    private Set<NetworkServiceProviderL2NetworkRefVO> attachedL2NetworkRefs = new HashSet<NetworkServiceProviderL2NetworkRefVO>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Set<String> getNetworkServiceTypes() {
        return networkServiceTypes;
    }

    public void setNetworkServiceTypes(Set<String> networkServiceTypes) {
        this.networkServiceTypes = networkServiceTypes;
    }

    public Set<NetworkServiceProviderL2NetworkRefVO> getAttachedL2NetworkRefs() {
        return attachedL2NetworkRefs;
    }

    public void setAttachedL2NetworkRefs(Set<NetworkServiceProviderL2NetworkRefVO> attchedL2NetworkRefs) {
        this.attachedL2NetworkRefs = attchedL2NetworkRefs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
