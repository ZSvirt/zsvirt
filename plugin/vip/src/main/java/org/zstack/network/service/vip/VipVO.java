package org.zstack.network.service.vip;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l3.*;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = NormalIpRangeVO.class, myField = "ipRangeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = UsedIpVO.class, myField = "usedIpUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VipPeerL3NetworkRefVO.class, myField = "uuid", targetField = "vipUuid"),
                @EntityGraph.Neighbour(type = VipNetworkServicesRefVO.class, myField = "uuid", targetField = "vipUuid")
        }
)
public class VipVO extends ResourceVO implements OwnedByAccount {
    protected static final CLogger logger = Utils.getLogger(VipVO.class);

    @Column
    @Index
    private String  name;

    @Column
    private String  description;

    @Column
    @ForeignKey(parentEntityClass = IpRangeEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String ipRangeUuid;
    
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;
    
    @Column
    @Index
    private String ip;
    
    @Column
    private String gateway;

    @Column
    @Enumerated(EnumType.STRING)
    private VipState state;

    @Column
    private String netmask;

    @Column
    private Integer prefixLen;

    @Column
    private String serviceProvider;

    @Column
    private String useFor;

    @Column
    private String usedIpUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vipUuid", insertable = false, updatable = false)
    @NoView
    private Set<VipPeerL3NetworkRefVO> peerL3NetworkRefs = new HashSet<>();

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="vipUuid", insertable=false, updatable=false)
    @NoView
    private Set<VipNetworkServicesRefVO> servicesRefs = new HashSet<VipNetworkServicesRefVO>();

    @Column
    private boolean system;

    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public VipState getState() {
        return state;
    }

    public void setState(VipState state) {
        this.state = state;
    }

    public Set<VipPeerL3NetworkRefVO> getPeerL3NetworkRefs() {
        return peerL3NetworkRefs;
    }

    public void setPeerL3NetworkRefs(Set<VipPeerL3NetworkRefVO> peerL3NetworkRefs) {
        this.peerL3NetworkRefs = peerL3NetworkRefs;
    }

    public Set<String> getPeerL3NetworkUuids() {
        if (getPeerL3NetworkRefs() != null && !getPeerL3NetworkRefs().isEmpty()) {
            return getPeerL3NetworkRefs().stream()
                    .map(ref -> ref.getL3NetworkUuid())
                    .collect(Collectors.toSet());
        }

        return null;
    }

    public Set<VipNetworkServicesRefVO> getServicesRefs() {
        return servicesRefs;
    }

    public void setServicesRefs(Set<VipNetworkServicesRefVO> servicesRefs) {
        this.servicesRefs = servicesRefs;
    }

    public Set<String> getServicesTypes() {
        if (getServicesRefs() != null && !getServicesRefs().isEmpty()) {
            return getServicesRefs().stream()
                                         .map(VipNetworkServicesRefVO::getServiceType)
                                         .collect(Collectors.toSet());
        }

        return new HashSet<>();
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Deprecated
    public String getUseFor() {
        if (getServicesTypes() != null && !getServicesTypes().isEmpty()) {
            return StringUtils.join(getServicesTypes(), ',');
        }

        return null;
    }

    @Deprecated
    public void setUseFor(String useFor) {
        this.useFor = useFor;
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

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
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

    public Integer getPrefixLen() {
        return prefixLen;
    }

    public void setPrefixLen(Integer prefixLen) {
        this.prefixLen = prefixLen;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
