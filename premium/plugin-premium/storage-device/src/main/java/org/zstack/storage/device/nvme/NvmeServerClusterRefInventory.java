package org.zstack.storage.device.nvme;

import org.zstack.header.cluster.ClusterInventory;
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
@Inventory(mappingVOClass = NvmeServerClusterRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nvmeServer", inventoryClass = NvmeServerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "nvmeServerUuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid"),
})
public class NvmeServerClusterRefInventory implements Serializable {
    @APINoSee
    private long id;

    private String nvmeServerUuid;

    private String clusterUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public NvmeServerClusterRefInventory() {
    }

    public NvmeServerClusterRefInventory(NvmeServerClusterRefVO vo) {
        this.setId(vo.getId());
        this.setClusterUuid(vo.getClusterUuid());
        this.setNvmeServerUuid(vo.getNvmeServerUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static NvmeServerClusterRefInventory valueOf(NvmeServerClusterRefVO vo) {
        return new NvmeServerClusterRefInventory(vo);
    }

    public static List<NvmeServerClusterRefInventory> valueOf1(Collection<NvmeServerClusterRefVO> vos) {
        List<NvmeServerClusterRefInventory> invs = new ArrayList<NvmeServerClusterRefInventory>();
        for (NvmeServerClusterRefVO vo : vos) {
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

    public String getNvmeServerUuid() {
        return nvmeServerUuid;
    }

    public void setNvmeServerUuid(String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
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
