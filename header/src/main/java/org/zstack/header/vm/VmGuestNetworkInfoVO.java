package org.zstack.header.vm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@org.zstack.header.vo.EntityGraph(
        parents = {
                @org.zstack.header.vo.EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "vmNicUuid", targetField = "uuid")
        }
)
public class VmGuestNetworkInfoVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    @Index
    private String ipAddress;

    @Column
    private String gateway;

    /**
     * A JSON string
     * contains a list of DNS servers for Linux vm regardless of ip version,
     * or a list of ip4 DNS servers for Windows vm.
     *
     * @see org.zstack.utils.gson.JSONObjectUtil#toJsonString(java.lang.Object)
     */
    @Column
    private String dnsList;

    /**
     * A JSON string contains a list of ipv4 route
     *
     * @see org.zstack.utils.gson.JSONObjectUtil#toJsonString(java.lang.Object)
     * @see org.zstack.network.service.HostRouteUtils.HostRouteInfo
     */
    @Column
    private String routeList;

    @Column
    @Index
    private String ipv6Address;

    @Column
    private String ipv6Gateway;

    /**
     * A JSON string that contains a list of ipv6 DNS servers for Windows vm only.
     *
     * @see org.zstack.utils.gson.JSONObjectUtil#toJsonString(java.lang.Object)
     */
    @Column
    private String dns6List;

    /**
     * A JSON string contains a list of ipv6 route
     *
     * @see org.zstack.utils.gson.JSONObjectUtil#toJsonString(java.lang.Object)
     * @see org.zstack.network.service.HostRouteUtils.HostRouteInfo
     */
    @Column
    private String route6List;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public VmGuestNetworkInfoVO() {
    }

    public VmGuestNetworkInfoVO(String VmInstanceUuid, String vmNicUuid) {
        this.vmInstanceUuid = VmInstanceUuid;
        this.vmNicUuid = vmNicUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDnsList() {
        return dnsList;
    }

    public void setDnsList(String dnsList) {
        this.dnsList = dnsList;
    }

    public String getRouteList() {
        return routeList;
    }

    public void setRouteList(String routeList) {
        this.routeList = routeList;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ip6) {
        this.ipv6Address = ip6;
    }

    public String getIpv6Gateway() {
        return ipv6Gateway;
    }

    public void setIpv6Gateway(String ipv6Gateway) {
        this.ipv6Gateway = ipv6Gateway;
    }

    public String getDns6List() {
        return dns6List;
    }

    public void setDns6List(String dns6List) {
        this.dns6List = dns6List;
    }

    public String getRoute6List() {
        return route6List;
    }

    public void setRoute6List(String route6List) {
        this.route6List = route6List;
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
}
