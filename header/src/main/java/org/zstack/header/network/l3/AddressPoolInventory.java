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
@Inventory(mappingVOClass = AddressPoolVO.class, collectionValueOfMethod="valueOf1")
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid")
})
public class AddressPoolInventory extends IpRangeInventory {

    public static IpRangeInventory valueOf1(AddressPoolVO vo) {
        AddressPoolInventory inv = new AddressPoolInventory();
        inv.setUuid(vo.getUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setEndIp(vo.getEndIp());
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
        inv.setIpRangeType(IpRangeType.AddressPool);

        return inv;
    }

    public static List<IpRangeInventory> valueOf1(Collection<AddressPoolVO> vos) {
        List<IpRangeInventory> invs = new ArrayList<>(vos.size());
        for (AddressPoolVO vo : vos) {
            invs.add(AddressPoolInventory.valueOf1(vo));
        }
        return invs;
    }
}
