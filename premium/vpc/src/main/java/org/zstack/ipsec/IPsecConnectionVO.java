package org.zstack.ipsec;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by xing5 on 2016/11/3.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = IPsecPeerCidrVO.class, myField = "uuid", targetField = "connectionUuid"),
                @EntityGraph.Neighbour(type = IPsecL3NetworkRefVO.class, myField = "uuid", targetField = "connectionUuid"),
        }
)
public class IPsecConnectionVO extends ResourceVO{
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String peerAddress;
    @Column
    private String authMode;
    @Column
    private String authKey;
    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.RESTRICT)
    private String vipUuid;
    @Column
    private String ikeAuthAlgorithm;
    @Column
    private String ikeEncryptionAlgorithm;
    @Column
    private int ikeDhGroup;
    @Column
    private String policyAuthAlgorithm;
    @Column
    private String policyEncryptionAlgorithm;
    @Column
    private String pfs;
    @Column
    private String policyMode;
    @Column
    private String transformProtocol;
    @Column
    private String ikeVersion;
    @Column
    private String idType;
    @Column
    private String localId;
    @Column
    private String remoteId;
    @Column
    private int ikeLifeTime;
    @Column
    private int lifeTime;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Column
    @Enumerated(EnumType.STRING)
    private IPsecState state;
    @Column
    @Enumerated(EnumType.STRING)
    private IPSecStatus status;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="connectionUuid", insertable=false, updatable=false)
    @NoView
    private Set<IPsecPeerCidrVO> peerCidrs = new HashSet<>();
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="connectionUuid", insertable=false, updatable=false)
    @NoView
    private Set<IPsecL3NetworkRefVO> l3Networks = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public IPsecState getState() {
        return state;
    }

    public void setState(IPsecState state) {
        this.state = state;
    }

    public IPSecStatus getStatus() {
        return status;
    }

    public void setStatus(IPSecStatus status) {
        this.status = status;
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

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getIkeAuthAlgorithm() {
        return ikeAuthAlgorithm;
    }

    public void setIkeAuthAlgorithm(String ikeAuthAlgorithm) {
        this.ikeAuthAlgorithm = ikeAuthAlgorithm;
    }

    public String getIkeEncryptionAlgorithm() {
        return ikeEncryptionAlgorithm;
    }

    public void setIkeEncryptionAlgorithm(String ikeEncryptionAlgorithm) {
        this.ikeEncryptionAlgorithm = ikeEncryptionAlgorithm;
    }

    public int getIkeDhGroup() {
        return ikeDhGroup;
    }

    public void setIkeDhGroup(int ikeDhGroup) {
        this.ikeDhGroup = ikeDhGroup;
    }

    public String getPolicyAuthAlgorithm() {
        return policyAuthAlgorithm;
    }

    public void setPolicyAuthAlgorithm(String policyAuthAlgorithm) {
        this.policyAuthAlgorithm = policyAuthAlgorithm;
    }

    public String getPolicyEncryptionAlgorithm() {
        return policyEncryptionAlgorithm;
    }

    public void setPolicyEncryptionAlgorithm(String policyEncryptionAlgorithm) {
        this.policyEncryptionAlgorithm = policyEncryptionAlgorithm;
    }

    public String getPfs() {
        return pfs;
    }

    public void setPfs(String pfs) {
        this.pfs = pfs;
    }

    public String getPolicyMode() {
        return policyMode;
    }

    public void setPolicyMode(String policyMode) {
        this.policyMode = policyMode;
    }

    public String getTransformProtocol() {
        return transformProtocol;
    }

    public void setTransformProtocol(String transformProtocol) {
        this.transformProtocol = transformProtocol;
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

    public Set<IPsecPeerCidrVO> getPeerCidrs() {
        return peerCidrs;
    }

    public void setPeerCidrs(Set<IPsecPeerCidrVO> peerCidrs) {
        this.peerCidrs = peerCidrs;
    }

    public Set<IPsecL3NetworkRefVO> getL3Networks() {
        return l3Networks;
    }

    public void setL3Networks(Set<IPsecL3NetworkRefVO> l3Networks) {
        this.l3Networks = l3Networks;
    }

    public String getIkeVersion() {
        return ikeVersion;
    }

    public void setIkeVersion(String ikeVersion) {
        this.ikeVersion = ikeVersion;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public int getIkeLifeTime() {
        return ikeLifeTime;
    }

    public void setIkeLifeTime(int ikeLifeTime) {
        this.ikeLifeTime = ikeLifeTime;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

}
