package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 06/03/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VtepVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = L2VxlanNetworkPoolInventory.class,
                foreignKey = "poolUuid", expandedInventoryKey = "uuid")
})
public class VtepInventory {
    private String uuid;

    private String hostUuid;

    private String vtepIp;

    private Integer port;

    private String type;

    private String physicalInterface;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String poolUuid;

    public VtepInventory() {
    }

    protected VtepInventory(VtepVO vo) {
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setVtepIp(vo.getVtepIp());
        this.setPort(vo.getPort());
        this.setPoolUuid(vo.getPoolUuid());
        this.setType(vo.getType());
        this.setPhysicalInterface(vo.getPhysicalInterface());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static VtepInventory valueOf(VtepVO vo) {
        return new VtepInventory(vo);
    }

    public static List<VtepInventory> valueOf(Collection<VtepVO> vos) {
        List<VtepInventory> invs = new ArrayList<>(vos.size());
        for (VtepVO vo : vos) {
            invs.add(VtepInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
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
