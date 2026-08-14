package org.zstack.storage.device.nvme;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = NvmeServerVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nvmeCluster", inventoryClass = NvmeServerClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "nvmeServerUuid"),
        @ExpandedQuery(expandedField = "nvmeTarget", inventoryClass = NvmeTargetInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "nvmeServerUuid"),
})
public class NvmeServerInventory implements Serializable {
    private String uuid;

    private String name;

    private String ip;

    private Integer port;

    private String state;

    private String transport;

    private List<NvmeTargetInventory> nvmeTargets;

    private List<NvmeServerClusterRefInventory> nvmeClusterRefs;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public NvmeServerInventory() {
    }

    public NvmeServerInventory(NvmeServerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setIp(vo.getIp());
        this.setPort(vo.getPort());
        this.setState(vo.getState());
        this.setTransport(vo.getTransport());
        this.setNvmeClusterRefs(NvmeServerClusterRefInventory.valueOf1(vo.getNvmeClusterRefs()));
        this.setNvmeTargets(NvmeTargetInventory.valueOf(vo.getNvmeTargets()));
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static NvmeServerInventory valueOf(NvmeServerVO vo) {
        return new NvmeServerInventory(vo);
    }

    public static List<NvmeServerInventory> valueOf1(Collection<NvmeServerVO> vos) {
        List<NvmeServerInventory> invs = new ArrayList<>();
        for (NvmeServerVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public List<NvmeTargetInventory> getNvmeTargets() {
        return nvmeTargets;
    }

    public void setNvmeTargets(List<NvmeTargetInventory> nvmeTargets) {
        this.nvmeTargets = nvmeTargets;
    }

    public List<NvmeServerClusterRefInventory> getNvmeClusterRefs() {
        return nvmeClusterRefs;
    }

    public void setNvmeClusterRefs(List<NvmeServerClusterRefInventory> nvmeClusterRefs) {
        this.nvmeClusterRefs = nvmeClusterRefs;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }
}
