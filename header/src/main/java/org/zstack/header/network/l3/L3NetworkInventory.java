package org.zstack.header.network.l3;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @inventory inventory for l3Network
 * @category l3network
 * @example {
 * "inventory": {
 * "uuid": "f73926eb4f234f8195c61c33d8db419d",
 * "name": "GuestNetwork",
 * "description": "Test",
 * "type": "L3BasicNetwork",
 * "zoneUuid": "732fbb4383b24b019f60d862995976bf",
 * "l2NetworkUuid": "f1a092c6914840c9895c564abbc55375",
 * "state": "Enabled",
 * "createDate": "May 4, 2014 11:07:24 PM",
 * "lastOpDate": "May 4, 2014 11:07:24 PM",
 * "dns": [],
 * "ipRanges": [
 * {
 * "uuid": "78b43f4b0a9745fab49c967e1c35beb1",
 * "l3NetworkUuid": "f73926eb4f234f8195c61c33d8db419d",
 * "name": "TestIpRange",
 * "description": "Test",
 * "startIp": "10.10.2.100",
 * "endIp": "10.20.2.200",
 * "netmask": "255.0.0.0",
 * "gateway": "10.10.2.1",
 * "createDate": "May 4, 2014 11:07:24 PM",
 * "lastOpDate": "May 4, 2014 11:07:24 PM"
 * }
 * ],
 * "networkServices": [
 * {
 * "l3NetworkUuid": "f73926eb4f234f8195c61c33d8db419d",
 * "networkServiceProviderUuid": "bbb525dc4cc8451295d379797e092dba",
 * "networkServiceType": "PortForwarding"
 * }
 * ]
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = L3NetworkVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l2Network", inventoryClass = L2NetworkInventory.class,
                foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l3NetworkUuid"),
        @ExpandedQuery(expandedField = "serviceProviderRef", inventoryClass = NetworkServiceL3NetworkRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l3NetworkUuid", hidden = true),
        @ExpandedQuery(expandedField = "usedIp", inventoryClass = UsedIpInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l3NetworkUuid"),
        @ExpandedQuery(expandedField = "ipRange", inventoryClass = IpRangeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l3NetworkUuid"),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "serviceProvider", expandedField = "serviceProviderRef.serviceProvider")
})
public class L3NetworkInventory implements Serializable {
    /**
     * @desc l3Network uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;
    /**
     * @desc l3Network type
     */
    private String type;
    /**
     * @desc uuid of zone this l3Network belongs to. See :ref:`ZoneInventory`
     */
    private String zoneUuid;
    /**
     * @desc uuid of l2Network this l3Network belongs to. See :ref:`L2Network`
     */
    private String l2NetworkUuid;
    /**
     * @desc - Enabled: ok for operations
     * - Disabled: no vm can be created on this l3Network
     * @choices - Enabled
     * - Disabled
     */
    private String state;

    private String dnsDomain;

    private Boolean system;

    private String category;

    @Deprecated
    private Integer ipVersion;

    private Boolean enableIPAM;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc a list of dns
     */
    @Queryable(mappingClass = L3NetworkDnsInventory.class,
            joinColumn = @JoinColumn(name = "l3NetworkUuid", referencedColumnName = "dns"))
    private List<String> dns;
    /**
     * @desc a list of ip ranges. see :ref:`IpRangeInventory`
     */
    @Queryable(mappingClass = IpRangeInventory.class,
            joinColumn = @JoinColumn(name = "l3NetworkUuid"))
    private List<IpRangeInventory> ipRanges;
    /**
     * @desc a list of network services enabled on this l3Network. See :ref:`NetworkServiceL3NetworkRefInventory`
     */
    @Queryable(mappingClass = NetworkServiceL3NetworkRefInventory.class,
            joinColumn = @JoinColumn(name = "l3NetworkUuid"))
    private List<NetworkServiceL3NetworkRefInventory> networkServices;
    /**
     * @desc a list of host route
     */
    @Queryable(mappingClass = L3NetworkHostRouteInventory.class,
            joinColumn = @JoinColumn(name = "l3NetworkUuid"))
    private List<L3NetworkHostRouteInventory> hostRoute;

    @Queryable(mappingClass = ReservedIpRangeInventory.class,
            joinColumn = @JoinColumn(name = "l3NetworkUuid"))
    private List<ReservedIpRangeInventory> reservedIpRanges;

    public L3NetworkInventory() {
    }

    protected L3NetworkInventory(L3NetworkVO vo) {
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setL2NetworkUuid(vo.getL2NetworkUuid());
        this.setName(vo.getName());
        this.setType(vo.getType());
        this.setDnsDomain(vo.getDnsDomain());
        this.setZoneUuid(vo.getZoneUuid());
        this.setState(vo.getState().toString());
        vo.getDns().stream().sorted(Comparator.comparingLong(L3NetworkDnsVO::getId)).collect(Collectors.toList())
                .forEach(dnsVo -> this.getDns().add(dnsVo.getDns()));
        this.setSystem(vo.isSystem());
        this.setIpRanges(IpRangeInventory.valueOf(vo.getIpRanges()));
        this.setLastOpDate(vo.getLastOpDate());
        this.setNetworkServices(NetworkServiceL3NetworkRefInventory.valueOf(vo.getNetworkServices()));
        this.setCategory(vo.getCategory().toString());
        this.setHostRoute(L3NetworkHostRouteInventory.valueOf(vo.getHostRoutes()));
        this.setReservedIpRanges(ReservedIpRangeInventory.valueOf(vo.getReservedIpRanges()));
        this.setIpVersion(vo.getIpVersion());
        this.setEnableIPAM(vo.getEnableIPAM());
    }

    public static L3NetworkInventory valueOf(L3NetworkVO vo) {
        return new L3NetworkInventory(vo);
    }

    public static List<L3NetworkInventory> valueOf(Collection<L3NetworkVO> vos) {
        List<L3NetworkInventory> invs = new ArrayList<L3NetworkInventory>(vos.size());
        for (L3NetworkVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domain) {
        this.dnsDomain = domain;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }


    public List<String> getDns() {
        if (dns == null) {
            dns = new ArrayList<String>();
        }
        return dns;
    }

    public void setDns(List<String> dns) {
        this.dns = dns;
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

    public List<IpRangeInventory> getIpRanges() {
        return ipRanges;
    }

    public void setIpRanges(List<IpRangeInventory> ipRanges) {
        this.ipRanges = ipRanges;
    }

    public List<ReservedIpRangeInventory> getReservedIpRanges() {
        return reservedIpRanges;
    }

    public void setReservedIpRanges(List<ReservedIpRangeInventory> reservedIpRanges) {
        this.reservedIpRanges = reservedIpRanges;
    }

    public List<NetworkServiceL3NetworkRefInventory> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(List<NetworkServiceL3NetworkRefInventory> networkServices) {
        this.networkServices = networkServices;
    }

    public List<NetworkServiceL3NetworkRefInventory> getNetworkServiceL3NetworkRefByProviderUuid(String providerUuid) {
        if (networkServices == null || networkServices.isEmpty()) {
            return new ArrayList<NetworkServiceL3NetworkRefInventory>(0);
        }

        List<NetworkServiceL3NetworkRefInventory> refs = new ArrayList<NetworkServiceL3NetworkRefInventory>();
        for (NetworkServiceL3NetworkRefInventory ref : networkServices) {
            if (ref.getNetworkServiceProviderUuid().equals(providerUuid)) {
                refs.add(ref);
            }
        }

        return refs;
    }

    public List<String> getNetworkServiceTypesFromProvider(String providerUuid) {
        List<NetworkServiceL3NetworkRefInventory> refs = getNetworkServiceL3NetworkRefByProviderUuid(providerUuid);
        List<String> nwTypes = new ArrayList<String>(refs.size());
        for (NetworkServiceL3NetworkRefInventory ref : refs) {
            nwTypes.add(ref.getNetworkServiceType());
        }
        return nwTypes;
    }

    public List<String> getNetworkServiceTypes() {
        List<String> nwTypes = new ArrayList<String>();
        for (NetworkServiceL3NetworkRefInventory ref : getNetworkServices()) {
            nwTypes.add(ref.getNetworkServiceType());
        }
        return nwTypes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<L3NetworkHostRouteInventory> getHostRoute() {
        return hostRoute;
    }

    public void setHostRoute(List<L3NetworkHostRouteInventory> hostRoute) {
        this.hostRoute = hostRoute;
    }

    @Deprecated
    public Integer getIpVersion() {
        return ipVersion;
    }

    @Deprecated
    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public List<Integer> getIpVersions() {
        List<Integer> ipVersions = new ArrayList<>();
        if (this.ipVersion == IPv6Constants.IPv4) {
            ipVersions.add(IPv6Constants.IPv4);
        } else if (this.ipVersion == IPv6Constants.IPv6) {
            ipVersions.add(IPv6Constants.IPv6);
        } else if (this.ipVersion == IPv6Constants.DUAL_STACK) {
            ipVersions.add(IPv6Constants.IPv4);
            ipVersions.add(IPv6Constants.IPv6);
        }

        return ipVersions;
    }

    public Boolean getEnableIPAM() {
        return enableIPAM;
    }

    public void setEnableIPAM(Boolean enableIPAM) {
        this.enableIPAM = enableIPAM;
    }

    public boolean enableIpAllocation() {
        if (L3NetworkType.valueOf(getType()).isMandatoryIpAllocation()) {
            return true;
        }

        for (NetworkServiceL3NetworkRefInventory ref : networkServices) {
            if (ref.getNetworkServiceType().equals(NetworkServiceType.DHCP.toString())) {
                return true;
            }
        }

        return false;
    }
}
