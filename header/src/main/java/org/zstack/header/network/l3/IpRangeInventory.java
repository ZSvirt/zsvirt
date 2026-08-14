package org.zstack.header.network.l3;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for ip range
 * @category l3network
 * @example {
 * "inventory": {
 * "uuid": "b1cfcdeca4024d13ac82edbe8d959720",
 * "l3NetworkUuid": "50e637dc68b7480291ba87cbb81d94ad",
 * "name": "TestIpRange",
 * "description": "Test",
 * "startIp": "10.0.0.100",
 * "endIp": "10.10.1.200",
 * "netmask": "255.0.0.0",
 * "gateway": "10.0.0.1",
 * "createDate": "May 4, 2014 4:30:23 PM",
 * "lastOpDate": "May 4, 2014 4:30:23 PM"
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = IpRangeVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid")
})
public class IpRangeInventory implements Serializable {
    /**
     * @desc ip range uuid
     */
    private String uuid;
    /**
     * @desc uuid of l3Network this ip range belongs to. See :ref:`L3NetworkInventory`
     */
    private String l3NetworkUuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;
    /**
     * @desc start ip address, in IPv4
     */
    private String startIp;
    /**
     * @desc end ip address, in IPv4
     */
    private String endIp;
    /**
     * @desc netmask, in IPv4
     */
    private String netmask;
    /**
     * @desc gateway, in IPv4
     */
    private String gateway;

    private String networkCidr;

    private Integer ipVersion;

    private String addressMode;

    private Integer prefixLen;

    private IpRangeType ipRangeType;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public static IpRangeInventory valueOf(IpRangeVO vo) {
        IpRangeInventory inv = new IpRangeInventory();
        inv.setUuid(vo.getUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setEndIp(vo.getEndIp());
        inv.setGateway(vo.getGateway());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setName(vo.getName());
        inv.setNetmask(vo.getNetmask());
        inv.setStartIp(vo.getStartIp());
        inv.setUuid(vo.getUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setNetworkCidr(vo.getNetworkCidr());
        inv.setIpVersion(vo.getIpVersion());
        inv.setAddressMode(vo.getAddressMode());
        inv.setPrefixLen(vo.getPrefixLen());

        return inv;
    }

    public static List<IpRangeInventory> valueOf(Collection<IpRangeVO> vos) {
        List<IpRangeInventory> invs = new ArrayList<IpRangeInventory>(vos.size());
        for (IpRangeVO vo : vos) {
            invs.add(IpRangeInventory.valueOf(vo));
        }
        return invs;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
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

    public int getIpVersion() {
        return ipVersion;
    }

    public String getAddressMode() {
        return addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Integer getPrefixLen() {
        return prefixLen;
    }

    public void setPrefixLen(Integer prefixLen) {
        this.prefixLen = prefixLen;
    }

    public IpRangeType getIpRangeType() {
        return ipRangeType;
    }

    public void setIpRangeType(IpRangeType ipRangeType) {
        this.ipRangeType = ipRangeType;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }

    public static IpRangeInventory fromMessage(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());
        ipr.setStartIp(msg.getStartIp());
        ipr.setEndIp(msg.getEndIp());
        ipr.setNetmask(msg.getNetmask());
        ipr.setPrefixLen(NetworkUtils.getPrefixLengthFromNetmask(msg.getNetmask()));
        ipr.setGateway(msg.getGateway());
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        SubnetUtils su = new SubnetUtils(msg.getStartIp(), msg.getNetmask());
        SubnetInfo si = su.getInfo();
        su = new SubnetUtils(si.getNetworkAddress(), msg.getNetmask());
        si = su.getInfo();
        ipr.setNetworkCidr(si.getCidrSignature());
        ipr.setUuid(msg.getResourceUuid());
        ipr.setIpVersion(IPv6Constants.IPv4);
        ipr.setIpRangeType(IpRangeType.valueOf(msg.getIpRangeType()));
        return ipr;
    }

    private static IpRangeInventory createAndSetIpv4RangeAttribute(APIAddIpRangeByNetworkCidrMsg msg, SubnetInfo subnet,
                                                                   String startIp, String endIp, String gateway) {
        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setNetworkCidr(subnet.getCidrSignature());
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());
        ipr.setIpRangeType(IpRangeType.valueOf(msg.getIpRangeType()));

        ipr.setGateway(gateway);
        ipr.setStartIp(startIp);
        ipr.setEndIp(endIp);

        ipr.setNetmask(subnet.getNetmask());
        ipr.setPrefixLen(NetworkUtils.getPrefixLengthFromNetmask(subnet.getNetmask()));
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        ipr.setUuid(msg.getResourceUuid());
        ipr.setIpVersion(IPv6Constants.IPv4);

        return ipr;
    }

    public static List<IpRangeInventory> fromMessage(APIAddIpRangeByNetworkCidrMsg msg) {
        List<IpRangeInventory> iprs = new ArrayList<>();
        SubnetUtils utils = new SubnetUtils(msg.getNetworkCidr());
        SubnetInfo subnet = utils.getInfo();
        utils = new SubnetUtils(subnet.getNetworkAddress(), subnet.getNetmask());
        subnet = utils.getInfo();

        String lowAddress = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getLowAddress()));
        String highAddress = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getHighAddress()));

        if (IpRangeType.valueOf(msg.getIpRangeType()) == IpRangeType.Normal) {
            if (msg.getGateway() == null || msg.getGateway().equals(lowAddress)) {
                String gateway = lowAddress;
                String startIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(lowAddress) + 1);
                String endIp = highAddress;
                IpRangeInventory ipr = createAndSetIpv4RangeAttribute(msg, subnet, startIp, endIp, gateway);
                iprs.add(ipr);
            } else if (msg.getGateway().equals(highAddress)) {
                String gateway = highAddress;
                String startIp = lowAddress;
                String endIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(highAddress) - 1);
                IpRangeInventory ipr = createAndSetIpv4RangeAttribute(msg, subnet, startIp, endIp, gateway);
                iprs.add(ipr);
            } else {
                String gateway = msg.getGateway();
                String startIp = lowAddress;
                String endIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(gateway) - 1);
                iprs.add(createAndSetIpv4RangeAttribute(msg, subnet, startIp, endIp, gateway));

                startIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(gateway) + 1);
                endIp = highAddress;
                iprs.add(createAndSetIpv4RangeAttribute(msg, subnet, startIp, endIp, gateway));
            }
        } else {
            String gateway = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(lowAddress));
            String startIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(lowAddress));
            String endIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(highAddress));
            iprs.add(createAndSetIpv4RangeAttribute(msg, subnet, startIp, endIp, gateway));
        }
        return iprs;
    }

    public static IpRangeInventory fromMessage(APIAddIpv6RangeByNetworkCidrMsg msg) {
        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setNetworkCidr(IPv6NetworkUtils.getFormalCidrOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());

        ipr.setAddressMode(msg.getAddressMode());
        ipr.setStartIp(IPv6NetworkUtils.getStartIpOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setEndIp(IPv6NetworkUtils.getEndIpOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setGateway(IPv6NetworkUtils.getGatewayOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        ipr.setUuid(msg.getResourceUuid());
        ipr.setIpVersion(IPv6Constants.IPv6);
        ipr.setPrefixLen(IPv6NetworkUtils.getPrefixLenOfNetworkCidr(msg.getNetworkCidr()));
        ipr.setIpRangeType(IpRangeType.valueOf(msg.getIpRangeType()));

        return ipr;
    }

    public static IpRangeInventory fromMessage(APIAddIpv6RangeMsg msg) {
        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setNetworkCidr(IPv6NetworkUtils.getNetworkCidrOfIpRange(msg.getStartIp(), msg.getPrefixLen()));
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());

        ipr.setAddressMode(msg.getAddressMode());
        ipr.setStartIp(IPv6NetworkUtils.getIpv6AddressCanonicalString(msg.getStartIp()));
        ipr.setEndIp(IPv6NetworkUtils.getIpv6AddressCanonicalString(msg.getEndIp()));
        ipr.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(msg.getPrefixLen()));
        ipr.setGateway(IPv6NetworkUtils.getIpv6AddressCanonicalString(msg.getGateway()));
        ipr.setPrefixLen(msg.getPrefixLen());
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        ipr.setUuid(msg.getResourceUuid());
        ipr.setIpVersion(IPv6Constants.IPv6);
        ipr.setIpRangeType(IpRangeType.valueOf(msg.getIpRangeType()));

        return ipr;
    }

    public SubnetUtils toSubnetUtils() {
        return new SubnetUtils(new SubnetUtils(gateway, netmask).getInfo().getNetworkAddress(), netmask);
    }
}
