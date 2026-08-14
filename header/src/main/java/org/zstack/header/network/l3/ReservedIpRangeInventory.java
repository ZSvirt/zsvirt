package org.zstack.header.network.l3;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ReservedIpRangeVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid")
})
public class ReservedIpRangeInventory implements Serializable {
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

    private Integer ipVersion;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public static ReservedIpRangeInventory valueOf(ReservedIpRangeVO vo) {
        ReservedIpRangeInventory inv = new ReservedIpRangeInventory();
        inv.setUuid(vo.getUuid());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setIpVersion(vo.getIpVersion());
        inv.setStartIp(vo.getStartIp());
        inv.setEndIp(vo.getEndIp());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public static List<ReservedIpRangeInventory> valueOf(Collection<ReservedIpRangeVO> vos) {
        List<ReservedIpRangeInventory> invs = new ArrayList<ReservedIpRangeInventory>(vos.size());
        for (ReservedIpRangeVO vo : vos) {
            invs.add(ReservedIpRangeInventory.valueOf(vo));
        }
        return invs;
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

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }

}
