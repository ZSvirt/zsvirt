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

@PythonClassInventory
@Inventory(mappingVOClass = RemoteVtepVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = L2VxlanNetworkPoolInventory.class,
                foreignKey = "poolUuid", expandedInventoryKey = "uuid")
})
public class RemoteVtepInventory {
    private String uuid;

    private String vtepIp;

    private Integer port;

    private String type;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String poolUuid;

    public RemoteVtepInventory() {
    }

    protected RemoteVtepInventory(RemoteVtepVO vo) {
        this.setUuid(vo.getUuid());
        this.setVtepIp(vo.getVtepIp());
        this.setPort(vo.getPort());
        this.setPoolUuid(vo.getPoolUuid());
        this.setType(vo.getType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static RemoteVtepInventory valueOf(RemoteVtepVO vo) {
        return new RemoteVtepInventory(vo);
    }

    public static List<RemoteVtepInventory> valueOf(Collection<RemoteVtepVO> vos) {
        List<RemoteVtepInventory> invs = new ArrayList<>(vos.size());
        for (RemoteVtepVO vo : vos) {
            invs.add(RemoteVtepInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
