package org.zstack.storage.device.iscsi;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@PythonClassInventory
@Inventory(mappingVOClass = IscsiTargetVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "iscsiServer", inventoryClass = IscsiServerInventory.class,
                foreignKey = "IscsiServerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "iscsiLun", inventoryClass = IscsiLunInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "iscsiTargetUuid"),
})
public class IscsiTargetInventory implements Serializable {
    private String iscsiServerUuid;

    private String uuid;

    private String iqn;

    @APINoSee
    private String state;

    private List<IscsiLunInventory> iscsiLuns = new ArrayList<>();

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IscsiTargetInventory that = (IscsiTargetInventory) o;
        return iqn != null && iqn.equals(that.iqn) && new HashSet<>(iscsiLuns).equals(new HashSet<>(that.iscsiLuns));
    }

    @Override
    public int hashCode() {
        if (iqn == null) {
            return 0;
        }
        int result = iqn.hashCode();
        Collections.sort(iscsiLuns);
        result = 31 * result + iscsiLuns.hashCode();
        return result;
    }

    public IscsiTargetInventory() {
    }

    public IscsiTargetInventory(IscsiTargetVO vo) {
        this.setUuid(vo.getUuid());
        this.setIqn(vo.getIqn());
        this.setState(vo.getState());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setIscsiLuns(IscsiLunInventory.valueOf2(vo.getIscsiLuns()));
    }

    public IscsiTargetInventory(IscsiTargetStruct s) {
        this.setIqn(s.iqn);
        this.setIscsiLuns(IscsiLunInventory.valueOf3(s.iscsiLunStructList));
    }

    public static List<IscsiTargetInventory> valueOf2(Collection<IscsiTargetStruct> structs) {
        List<IscsiTargetInventory> invs = new ArrayList<>();
        for (IscsiTargetStruct s : structs) {
            invs.add(new IscsiTargetInventory(s));
        }

        return invs;
    }

    public static IscsiTargetInventory valueOf(IscsiTargetVO vo) {
        return new IscsiTargetInventory(vo);
    }

    public static List<IscsiTargetInventory> valueOf1(Collection<IscsiTargetVO> vos) {
        List<IscsiTargetInventory> invs = new ArrayList<IscsiTargetInventory>();
        for (IscsiTargetVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getIscsiServerUuid() {
        return iscsiServerUuid;
    }

    public void setIscsiServerUuid(String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
    }

    public String getIqn() {
        return iqn;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<IscsiLunInventory> getIscsiLuns() {
        return iscsiLuns;
    }

    public void setIscsiLuns(List<IscsiLunInventory> iscsiLuns) {
        this.iscsiLuns = iscsiLuns;
    }
}
