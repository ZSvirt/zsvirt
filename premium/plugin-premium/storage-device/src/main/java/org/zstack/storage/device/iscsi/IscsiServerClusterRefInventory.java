package org.zstack.storage.device.iscsi;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = IscsiServerClusterRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "iscsiServer", inventoryClass = IscsiServerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "iscsiServerUuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid"),
})
public class IscsiServerClusterRefInventory implements Serializable {
    @APINoSee
    private long id;

    private String iscsiServerUuid;

    private String clusterUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public IscsiServerClusterRefInventory() {
    }

    public IscsiServerClusterRefInventory(IscsiServerClusterRefVO vo) {
        this.setId(vo.getId());
        this.setClusterUuid(vo.getClusterUuid());
        this.setIscsiServerUuid(vo.getIscsiServerUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static IscsiServerClusterRefInventory valueOf(IscsiServerClusterRefVO vo) {
        return new IscsiServerClusterRefInventory(vo);
    }

    public static List<IscsiServerClusterRefInventory> valueOf1(Collection<IscsiServerClusterRefVO> vos) {
        List<IscsiServerClusterRefInventory> invs = new ArrayList<IscsiServerClusterRefInventory>();
        for (IscsiServerClusterRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIscsiServerUuid() {
        return iscsiServerUuid;
    }

    public void setIscsiServerUuid(String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
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
