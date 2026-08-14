package org.zstack.storage.device.iscsi;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = IscsiServerVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "iscsiCluster", inventoryClass = IscsiServerClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "iscsiServerUuid"),
        @ExpandedQuery(expandedField = "iscsiTarget", inventoryClass = IscsiTargetInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "iscsiServerUuid"),
})
public class IscsiServerInventory implements Serializable {
    private String uuid;

    private String name;

    private String ip;

    private Integer port;

    private String chapUserName;

    @NoLogging
    private String chapUserPassword;

    private String state;

    private List<IscsiTargetInventory> iscsiTargets;

    private List<IscsiServerClusterRefInventory> iscsiClusterRefs;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public IscsiServerInventory() {
    }

    public IscsiServerInventory(IscsiServerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setIp(vo.getIp());
        this.setPort(vo.getPort());
        this.setChapUserName(vo.getChapUserName());
        this.setChapUserPassword(vo.getChapUserPassword());
        this.setState(vo.getState());
        this.setIscsiClusterRefs(IscsiServerClusterRefInventory.valueOf1(vo.getIscsiClusterRefs()));
        this.setIscsiTargets(IscsiTargetInventory.valueOf1(vo.getIscsiTargets()));
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static IscsiServerInventory valueOf(IscsiServerVO vo) {
        return new IscsiServerInventory(vo);
    }

    public static List<IscsiServerInventory> valueOf1(Collection<IscsiServerVO> vos) {
        List<IscsiServerInventory> invs = new ArrayList<IscsiServerInventory>();
        for (IscsiServerVO vo : vos) {
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

    public String getChapUserName() {
        return chapUserName;
    }

    public void setChapUserName(String chapUserName) {
        this.chapUserName = chapUserName;
    }

    public String getChapUserPassword() {
        return chapUserPassword;
    }

    public void setChapUserPassword(String chapUserPassword) {
        this.chapUserPassword = chapUserPassword;
    }

    public List<IscsiTargetInventory> getIscsiTargets() {
        return iscsiTargets;
    }

    public void setIscsiTargets(List<IscsiTargetInventory> iscsiTargets) {
        this.iscsiTargets = iscsiTargets;
    }

    public List<IscsiServerClusterRefInventory> getIscsiClusterRefs() {
        return iscsiClusterRefs;
    }

    public void setIscsiClusterRefs(List<IscsiServerClusterRefInventory> iscsiClusterRefs) {
        this.iscsiClusterRefs = iscsiClusterRefs;
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
}
