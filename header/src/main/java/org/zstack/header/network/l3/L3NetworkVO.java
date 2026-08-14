package org.zstack.header.network.l3;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table
@EO(EOClazz = L3NetworkEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = L2NetworkVO.class, myField = "l2NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
        },
        friends = {
                @EntityGraph.Neighbour(type = IpRangeVO.class, myField = "uuid", targetField = "l3NetworkUuid"),
        }
)
public class L3NetworkVO extends L3NetworkAO implements OwnedByAccount {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<L3NetworkDnsVO> dns = new HashSet<L3NetworkDnsVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<IpRangeVO> ipRanges = new HashSet<IpRangeVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<NetworkServiceL3NetworkRefVO> networkServices = new HashSet<NetworkServiceL3NetworkRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<L3NetworkHostRouteVO> hostRoutes = new HashSet<L3NetworkHostRouteVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<ReservedIpRangeVO> reservedIpRanges = new HashSet<ReservedIpRangeVO>();

    @Transient
    private String accountUuid;

    public L3NetworkVO() {
    }

    public L3NetworkVO(L3NetworkVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setState(vo.getState());
        this.setType(vo.getType());
        this.setZoneUuid(vo.getZoneUuid());
        this.setL2NetworkUuid(vo.getL2NetworkUuid());
        this.setSystem(vo.isSystem());
        this.setDnsDomain(vo.getDnsDomain());
        this.setIpVersion(vo.getIpVersion());
        this.setEnableIPAM(vo.getEnableIPAM());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setCategory(vo.getCategory());
        this.setDns(vo.getDns());
        this.setIpRanges(vo.getIpRanges());
        this.setNetworkServices(vo.getNetworkServices());
        this.setHostRoutes(vo.getHostRoutes());
        this.setReservedIpRanges(vo.getReservedIpRanges());
        this.setAccountUuid(vo.getAccountUuid());
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<L3NetworkDnsVO> getDns() {
        return dns;
    }

    public void setDns(Set<L3NetworkDnsVO> dns) {
        this.dns = dns;
    }

    public Set<IpRangeVO> getIpRanges() {
        return ipRanges;
    }

    public void setIpRanges(Set<IpRangeVO> ipRanges) {
        this.ipRanges = ipRanges;
    }

    public Set<NetworkServiceL3NetworkRefVO> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Set<NetworkServiceL3NetworkRefVO> networkServices) {
        this.networkServices = networkServices;
    }

    public Set<L3NetworkHostRouteVO> getHostRoutes() {
        return hostRoutes;
    }

    public void setHostRoutes(Set<L3NetworkHostRouteVO> hostRoutes) {
        this.hostRoutes = hostRoutes;
    }

    public Set<ReservedIpRangeVO> getReservedIpRanges() {
        return reservedIpRanges;
    }

    public void setReservedIpRanges(Set<ReservedIpRangeVO> reservedIpRanges) {
        this.reservedIpRanges = reservedIpRanges;
    }

    public List<Integer> getIpVersions() {
        return getIpRanges().stream().map(IpRangeAO::getIpVersion).distinct()
                .sorted().collect(Collectors.toList());
    }

    public boolean enableIpAllocation() {
        if (L3NetworkType.valueOf(getType()).isMandatoryIpAllocation()) {
            return true;
        }

        for (NetworkServiceL3NetworkRefVO ref : networkServices) {
            if (ref.getNetworkServiceType().equals(NetworkServiceType.DHCP.toString())) {
                return true;
            }
        }

        return false;
    }
}
